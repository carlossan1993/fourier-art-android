# Etapa 2 — Captura y preparación de fotos (0.6.0)

El propietario pidió el 19/10/2026 continuar esta etapa aquí en Work por disponibilidad de consumo, aunque la distribución inicial la asignaba a Chat. Se conserva el motor 0.5.0: no cambian los archivos de `core/`.

## Flujo implementado

- Elegir foto mediante Android Photo Picker o capturar con cámara integrada CameraX. Permiso CAMERA solicitado solo al pulsar Tomar foto, sin permisos generales de galería/almacenamiento. Cámara opcional; ante denegación, ausencia o error, se ofrece elegir una foto.
- Captura privada, sin escribir en la galería. Previsualización y cambio de cámara cuando ambas están disponibles. El encuadre se prepara sobre la foto capturada antes de procesar; la previsualización de cámara no promete un recorte idéntico al sensor.
- Pantalla de preparación: giro en ambos sentidos, recorte por esquinas, desplazamiento del recorte, controles horizontal/vertical alternativos, vuelta al original y aplicación explícita.
- Orientación EXIF normalizada una vez al importar. El giro manual transforma también la selección normalizada. El mismo bitmap preparado alimenta la inferencia, vista previa y Fourier, sin alterar el motor.
- Importar/capturar abre el editor; Aplicar y procesar calcula los contornos. Editar permite regresar al original sin acumular recortes. Cancelar una edición mantiene la preparación aplicada. Descartar una foto aún no aplicada vuelve a la demostración; cancelar el selector o la cámara mantiene el trabajo previo.
- ViewModel conserva foto, preparación y análisis al recrear la actividad. Una única fuente normalizada y metadatos se guardan como trabajo privado en noBackupFilesDir para recuperar una sesión tras recrear el proceso. No es una galería de proyectos. Reemplazar o descartar elimina la foto anterior; las capturas temporales se limpian al importar, cancelar/fallar o volver a iniciar.
- Manejo de trabajos cancelados y escrituras serializadas para evitar que una operación anterior sobrescriba un resultado nuevo. Fotografías ilegibles o problemas de memoria no deben borrar la imagen previa.
- Español/inglés en controles y errores. Barras del sistema con fondo/iconos coherentes con el tema oscuro y áreas seguras, también en Android 15. Límites de decodificación y parámetros del motor conservados.

## Dependencias acotadas

CameraX 1.5.3 (camera2/lifecycle/view), Lifecycle 2.10.0 (ViewModel/runtime Compose) y ExifInterface 1.4.2, requerida también por CameraX 1.5.3 por su corrección de JPEGs. Sin backend, SDK publicitario ni servicio de pago.

## Verificación preparada

- Cuatro pruebas JVM de coordenadas, giros, límites y recorte mínimo, además de las 40 pruebas del motor existente.
- Android: ocho orientaciones EXIF reales en JPEG, giro/recorte con imagen de cuadrantes, recuperación de fuente/preparación en un nuevo almacén y limpieza de versiones anteriores.
- Flujo UI en emuladores API 29 y 35: importar, arrastrar recorte, girar, recrear actividad conservando ViewModel, aplicar, cancelar selector, denegar permiso, concederlo, cancelar cámara conservando la foto previa y capturar realmente con CameraX y cámara emulada. Produce capturas en la evidencia de Actions.
- Se conservan las cinco pruebas instrumentadas de calidad del motor.

Consultar Actions del commit para los resultados efectivos. La cámara emulada no sustituye cámaras físicas ni fabricantes distintos. La salida de Etapa 2 requiere probar captura/selección, giros y recorte en dos teléfonos según el plan; no se declara completada esa validación por compilar o por las pruebas del emulador. G1 formal 24/30 sigue sin medir en #3.

## Fuentes oficiales consultadas

- https://developer.android.com/media/camera/camerax/take-photo
- https://developer.android.com/jetpack/androidx/releases/camera (1.5.3 y actualización EXIF)
- https://developer.android.com/jetpack/androidx/releases/lifecycle
- https://developer.android.com/develop/ui/compose/state-saving
