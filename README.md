# Traductor de TikTok LIVE (Android)

Proyecto Android nativo en Kotlin.

## Incluido en esta primera base
- Interfaz de selección de fuente: micrófono, audio interno o ambos.
- Solicitud de permiso RECORD_AUDIO.
- Solicitud de consentimiento MediaProjection para audio interno.
- Servicio de ventana flotante sobre otras aplicaciones.

## Estado real
Esta es la base inicial de la aplicación y sus permisos. Todavía no realiza traducción de voz de extremo a extremo: faltan el reconocimiento de voz en streaming, el motor de traducción al español, la síntesis de voz y la conexión del audio capturado con esos motores. No se debe considerar todavía un APK terminado.

## Límites de captura
- Micrófono: requiere permiso RECORD_AUDIO.
- Audio interno: requiere consentimiento MediaProjection. Android solo permite capturar audio de otras aplicaciones cuando se cumplen sus políticas de captura; la aplicación origen puede bloquearlo.
- Ambos: requerirá capturar ambas fuentes y mezclarlas/sincronizarlas antes del reconocimiento.
- MediaProjection es temporal y Android puede exigir consentimiento nuevo para cada sesión.

## Compilar
Abrir en Android Studio con JDK 17 y Android Gradle Plugin 8.7.3. Compilar el módulo app.

## Próxima fase
Implementar captura PCM real para cada fuente, mezcla, reconocimiento de baja latencia, traducción al español y Text-to-Speech. También falta validar el comportamiento en el dispositivo Android objetivo.
