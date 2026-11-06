package com.magitv.tiktoklivetranslator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

class MainActivity : Activity() {
    private val requestMic = 40
    private val requestProjection = 41
    private lateinit var sourceGroup: RadioGroup
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestMic)
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
        }
        root.addView(TextView(this).apply { text = "Traductor de TikTok LIVE"; textSize = 24f })
        root.addView(TextView(this).apply {
            text = "Fuente de audio"; textSize = 18f; setPadding(0, 28, 0, 8)
        })
        sourceGroup = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        sourceGroup.addView(RadioButton(this).apply { id = 1; text = "Micrófono" })
        sourceGroup.addView(RadioButton(this).apply { id = 2; text = "Audio interno" })
        sourceGroup.addView(RadioButton(this).apply { id = 3; text = "Ambos (micrófono + audio interno)" })
        sourceGroup.check(1)
        root.addView(sourceGroup)
        root.addView(TextView(this).apply {
            text = "La traducción se reproducirá en voz española y los controles estarán en una ventana flotante."
            textSize = 14f; setPadding(0, 18, 0, 18)
        })
        root.addView(Button(this).apply {
            text = "Activar ventana flotante"
            setOnClickListener { enableOverlay() }
        })
        root.addView(Button(this).apply {
            text = "Autorizar audio interno"
            setOnClickListener { requestInternalAudioPermission() }
        })
        root.addView(Button(this).apply {
            text = "Iniciar captura"
            setOnClickListener { startSelectedCapture() }
        })
        status = TextView(this).apply {
            text = "Estado: listo. Selecciona una fuente de audio."
            textSize = 14f; setPadding(0, 20, 0, 0)
        }
        root.addView(status)
        setContentView(root)
    }

    private fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            status.text = "Autoriza 'Mostrar sobre otras aplicaciones' y vuelve aquí."
        } else {
            startService(Intent(this, OverlayService::class.java))
            status.text = "Ventana flotante activada."
        }
    }

    private fun requestInternalAudioPermission() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), requestProjection)
    }

    private fun startSelectedCapture() {
        when (sourceGroup.checkedRadioButtonId) {
            1 -> {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    status.text = "Micrófono seleccionado. Captura preparada."
                } else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestMic)
            }
            2 -> {
                status.text = "Acepta el permiso del sistema para intentar capturar audio interno."
                requestInternalAudioPermission()
            }
            3 -> {
                status.text = "Modo combinado seleccionado. Se solicitará permiso de audio interno."
                requestInternalAudioPermission()
            }
        }
    }

    @Deprecated("Deprecated by Android, retained for compatibility with MediaProjection consent")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestProjection) {
            if (resultCode == RESULT_OK && data != null) {
                AudioSessionStore.projectionResultCode = resultCode
                AudioSessionStore.projectionData = data
                status.text = "Permiso de audio interno concedido para esta sesión."
            } else status.text = "Permiso denegado. Puedes usar el micrófono."
        }
    }
}
