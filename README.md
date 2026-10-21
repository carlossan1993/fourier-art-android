# FOURIER ART — Android

Aplicación Android nativa que transformará fotografías en animaciones matemáticas mediante Series de Fourier y epiciclos.

**Estado:** 0.12.0 aprobada e integrada en main mediante PR #20. Etapa 8 cerrada por aceptación expresa de Carlos el 21/10/2026, con las limitaciones documentadas. Incluye reutilización de curvas y mediciones reproducibles. Motor y contornos conservados.

Estado actual: [Etapa 8 cerrada y siguiente etapa](docs/ETAPAS.md). La [preparación para Google Play](docs/ETAPA_9_GOOGLE_PLAY.md) (Etapa 9, Work) está en curso: propuesta 0.13.0, privacidad ES/EN, icono y configuración release. Firma definitiva y Play Console pendientes. La matriz de dos teléfonos fue omitida expresamente por Carlos. El [banco visual](quality-bank/INFORME.md) tiene 30 casos revisados en Dibujo (21 reconocibles); la meta formal de #3 sigue sin acreditar y sus mejoras se aplazan. La combinación de letra al 150% con rotación queda sin verificar como excepción aceptada del cierre.

## Alcance V1
- Android nativo con **Kotlin** y **Jetpack Compose**.
- Idiomas **español e inglés**, según el dispositivo y con selector en la aplicación.
- Procesamiento de fotos **local**; sin backend, cuentas, anuncios ni compras en V1.
- Próximas etapas: preparación para Google Play y publicación/verificación.
- Infraestructura recurrente prevista: **US$ 0**.

## Desarrollo
- JDK 17, Gradle 8.13, Android SDK API 36.
- Android Gradle Plugin 8.13.2; Kotlin 2.3.21.
- En un equipo con Gradle instalado: `gradle :app:assembleDebug`.
- En GitHub: la pestaña **Actions** ejecuta la compilación y permite descargar el **APK de pruebas** en el artefacto `fourier-art-debug-apk`.
- Galería y cámara, giro/recorte antes de procesar, perfiles Auto/Sujeto/Dibujo, vista previa y Fourier; exportación PNG/GIF/MP4 con guardado y compartir.
- Ver [motor 0.5.0 y comparaciones de trazado](docs/MOTOR_0.5.0.md).

## Trabajo por etapas
Los cambios se preparan en ramas y PR, se compilan y se prueban antes de integrar. La Etapa 0 termina cuando existe un APK instalable y una prueba real en Android.

> El identificador `com.fourierart.app` es provisional hasta decidir la identidad de publicación. No subir fotos privadas, tokens, llaves ni credenciales a este repositorio público.
