# FOURIER ART — Android

Aplicación Android nativa que transformará fotografías en animaciones matemáticas mediante Series de Fourier y epiciclos.

**Estado:** Etapa 2 en desarrollo y pruebas, versión 0.6.0: cámara, preparación y recuperación de la foto. Motor 0.5.0 conservado. Pendiente de prueba física de captura/recorte en dos dispositivos.

Estado actual: [Etapa 2: captura y preparación](docs/ETAPA_2_CAPTURA.md). El propietario pidió continuar esta etapa en Work el 19/10/2026.

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
- Galería y cámara, giro/recorte antes de procesar, perfiles Auto/Sujeto/Dibujo, vista previa y Fourier. Exportación pendiente.
- Ver [motor 0.5.0 y comparaciones de trazado](docs/MOTOR_0.5.0.md).

## Trabajo por etapas
Los cambios se preparan en ramas y PR, se compilan y se prueban antes de integrar. La Etapa 0 termina cuando existe un APK instalable y una prueba real en Android.

> El identificador `com.fourierart.app` es provisional hasta decidir la identidad de publicación. No subir fotos privadas, tokens, llaves ni credenciales a este repositorio público.
