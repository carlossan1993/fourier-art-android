# FOURIER ART — Android

Aplicación Android nativa que transformará fotografías en animaciones matemáticas mediante Series de Fourier y epiciclos.

**Estado:** base 0.5.0 elegida por el propietario e integrada en main. Prueba de concepto cerrada como decisión de continuidad; siguiente: **Etapa 2 — captura e importación, en Chat**. El banco formal de reconocibilidad sigue pendiente.

Punto de continuación: [Etapa 2 en Chat](docs/CONTINUAR_EN_CHAT_ETAPA_2.md).

## Alcance V1
- Android nativo con **Kotlin** y **Jetpack Compose**.
- Idiomas **español e inglés**, según el dispositivo y con selector en la aplicación.
- Procesamiento de fotos **local**; sin backend, cuentas, anuncios ni compras en V1.
- Próximas etapas: prueba de concepto con contornos, Fourier, cámara/galería, animación, exportación GIF/MP4/PNG, pruebas y Google Play.
- Infraestructura recurrente prevista: **US$ 0**.

## Desarrollo
- JDK 17, Gradle 8.13, Android SDK API 36.
- Android Gradle Plugin 8.13.2; Kotlin 2.3.21.
- En un equipo con Gradle instalado: `gradle :app:assembleDebug`.
- En GitHub: la pestaña **Actions** ejecuta la compilación y permite descargar el **APK de pruebas** en el artefacto `fourier-art-debug-apk`.
- Importación desde galería, perfiles Auto/Sujeto/Dibujo, vista previa de trazos y reconstrucción Fourier. Cámara y exportación pendientes.
- Ver [motor 0.5.0 y comparaciones de trazado](docs/MOTOR_0.5.0.md).

## Trabajo por etapas
Los cambios se preparan en ramas y PR, se compilan y se prueban antes de integrar. La Etapa 0 termina cuando existe un APK instalable y una prueba real en Android.

> El identificador `com.fourierart.app` es provisional hasta decidir la identidad de publicación. No subir fotos privadas, tokens, llaves ni credenciales a este repositorio público.
