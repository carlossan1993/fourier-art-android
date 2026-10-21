# Dependencias y derechos — revisión de publicación

Las dependencias resueltas exactas se conservan en `dependencies.txt` de la auditoría release; no se limita la revisión a las dependencias directas.

| Componente | Uso | Referencia de licencia/condiciones |
| --- | --- | --- |
| AndroidX, Compose, AppCompat, CameraX y Lifecycle | Interfaz, cámara y ciclo de vida | Apache License 2.0; https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt |
| Kotlin y bibliotecas de soporte | Lenguaje y ejecución | Apache License 2.0; https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt |
| ML Kit Face Detection y Subject Segmentation / Google Play services | Rasgos y máscara de sujeto | Condiciones de Google y ML Kit; https://developers.google.com/ml-kit/terms y https://developers.google.com/terms |
| Código del proyecto e icono/gráficos propios | Aplicación y ficha | No se añade una licencia de código abierto por suposición. El propietario decide los derechos de distribución de su proyecto. |
| Imágenes del banco y fixtures de CI | Pruebas, no contenido de usuario de la app | Fuentes y derechos en `quality-bank/README.md`; están fuera de `src/main` y no se incluyen intencionadamente en release. |

La app no incluye FFmpeg, Pillow o CairoSVG: son herramientas de comprobación/generación fuera del APK. Sus licencias no convierten el motor Android en una dependencia de esas bibliotecas.

Antes del envío final, revisar los avisos exigidos por las dependencias transitivas y los términos de distribución de modelos. Esta tabla es un inventario de revisión, no sustituye los textos de licencia o avisos aplicables ni acredita que se hayan cumplido todos los requisitos de terceros.
