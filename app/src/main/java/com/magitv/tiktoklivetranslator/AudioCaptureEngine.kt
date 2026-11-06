package com.magitv.tiktoklivetranslator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * PCM capture layer. The consumer must forward frames to a streaming ASR engine.
 * This class deliberately does not pretend that raw PCM is already translated.
 */
class AudioCaptureEngine(
    private val onPcmFrame: (ShortArray, Int) -> Unit,
    private val onError: (String) -> Unit
) {
    enum class Source { MICROPHONE, INTERNAL, BOTH }

    private val running = AtomicBoolean(false)
    private var worker: Thread? = null
    private var mic: AudioRecord? = null
    private var internal: AudioRecord? = null

    fun start(source: Source, projection: MediaProjection? = null) {
        if (running.get()) return
        if (source != Source.MICROPHONE && projection == null) {
            onError("Falta el permiso de captura de audio interno.")
            return
        }
        if (source != Source.INTERNAL &&
            android.os.Build.VERSION.SDK_INT >= 23 &&
            android.app.Application::class.java == null) return

        val sampleRate = 16000
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            onError("El dispositivo no admite el formato PCM solicitado.")
            return
        }
        val bufferBytes = maxOf(minBuffer * 2, 4096)

        try {
            if (source == Source.MICROPHONE || source == Source.BOTH) {
                mic = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferBytes)
                    .build()
            }
            if (source == Source.INTERNAL || source == Source.BOTH) {
                if (Build.VERSION.SDK_INT < 29) {
                    onError("El audio interno requiere Android 10 o posterior.")
                    release()
                    return
                }
                val config = AudioPlaybackCaptureConfiguration.Builder(projection!!)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .build()
                internal = AudioRecord.Builder()
                    .setAudioFormat(format)
                    .setAudioPlaybackCaptureConfig(config)
                    .setBufferSizeInBytes(bufferBytes)
                    .build()
            }
            mic?.startRecording()
            internal?.startRecording()
            running.set(true)
            worker = thread(name = "LiveAudioCapture", isDaemon = true) {
                val micFrame = ShortArray(1024)
                val internalFrame = ShortArray(1024)
                val output = ShortArray(1024)
                while (running.get()) {
                    val micCount = mic?.read(micFrame, 0, micFrame.size, AudioRecord.READ_NON_BLOCKING) ?: 0
                    val internalCount = internal?.read(internalFrame, 0, internalFrame.size, AudioRecord.READ_NON_BLOCKING) ?: 0
                    val count = when (source) {
                        Source.MICROPHONE -> micCount
                        Source.INTERNAL -> internalCount
                        Source.BOTH -> maxOf(micCount, internalCount)
                    }
                    if (count > 0) {
                        for (i in 0 until count) {
                            val a = if (i < micCount) micFrame[i].toInt() else 0
                            val b = if (i < internalCount) internalFrame[i].toInt() else 0
                            output[i] = when (source) {
                                Source.BOTH -> ((a + b) / 2).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                                Source.MICROPHONE -> a.toShort()
                                Source.INTERNAL -> b.toShort()
                            }
                        }
                        onPcmFrame(output.copyOf(count), sampleRate)
                    } else {
                        Thread.sleep(8)
                    }
                }
            }
        } catch (e: SecurityException) {
            onError("Android denegó el acceso a la fuente de audio: ${e.message ?: "permiso no disponible"}.")
            release()
        } catch (e: Exception) {
            onError("No se pudo iniciar la captura: ${e.message ?: "error desconocido"}.")
            release()
        }
    }

    fun stop() {
        running.set(false)
        worker?.join(500)
        worker = null
        release()
    }

    private fun release() {
        try { mic?.stop() } catch (_: Exception) {}
        try { internal?.stop() } catch (_: Exception) {}
        mic?.release()
        internal?.release()
        mic = null
        internal = null
    }
}
