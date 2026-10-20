# Etapa 6 · Exportación local (0.10.0)

Base: 0.9.0 aceptada por Carlos, PR #14 integrado. Seguimiento #15.

## Comportamiento

Desde el reproductor, **Exportar** abre una vista previa del encuadre exacto. Conserva dibujo, N, color, fondo, epiciclos y zoom/desplazamiento; **Ajustar dibujo** permite recuperar el encuadre completo sin modificar el reproductor. Los formatos son cuadrado 1:1 y vertical 9:16.

| Salida | Cuadrado | Vertical | Tiempo |
|---|---|---|---|
| PNG | 720 o 1080 px | 720×1280 o 1080×1920 | Dibujo completo |
| GIF | 480×480 | 360×640 | 6/10 s, 12 fps, bucle |
| MP4 | 720×720 | 720×1280 | 6/10 s, H.264 24 fps, sin audio |

La duración elegida reemplaza explícitamente la velocidad del reproductor: una pasada, 85% de dibujo y 15% de permanencia final. El control de posición de la vista previa sirve para revisar el encuadre, y no cambia el inicio de la exportación.

El archivo se crea primero en caché privado. Solo un archivo completado habilita **Guardar** (selector de documentos de Android) y **Compartir** (menú nativo con URI temporal de solo lectura). Guardar permite conservarlo en la ubicación elegida. Cancelar limpia los resultados incompletos y mantiene el último resultado terminado. Salir durante una operación la cancela. Los archivos temporales de más de 24 horas se eliminan al crear una nueva sesión de exportación.

## Implementación

Renderizado Canvas fuera de pantalla, con `DrawingTimeline` y las mismas curvas reconstruidas del reproductor. No recaptura la pantalla, no reanaliza la fotografía y no conecta trazos separados. Vista previa y archivo usan `ExportRenderer` y un snapshot de los ajustes; cambios posteriores no mutan un archivo ya creado.

Un bitmap reutilizado por trabajo, sin acumular fotogramas. PNG usa el codificador de Android; GIF89a usa paleta de dos rampas adaptadas al fondo/tinta/guías y LZW con diccionario acotado; MP4 usa textura GLES/EGL sobre Surface de MediaCodec y MediaMuxer, con marcas de tiempo calculadas, sin espera en tiempo real. Libera codecs, EGL, bitmap y archivos parciales al cancelar o fallar. El motor de detección aceptado no cambia.

No se agregan dependencias, servicios, permisos de almacenamiento ni acceso a rutas amplias. FileProvider expone únicamente `cache/exports/`. La instancia de ViewModel conserva un trabajo durante recreación de Activity; no promete continuar tras cerrar el proceso.

## Validación

Completada el 20/10/2026 sobre el código `5fe71eab7d46f40db7715d5117071f1a5ca739ba`:

- **86 pruebas JVM correctas**: timeline, dimensiones, marcas de tiempo, LZW con crecimiento/reinicio de diccionario y todos los píxeles decodificados por ImageIO del JDK.
- **20 pruebas API 29 y 15 API 35 correctas**: incluye las regresiones previas y tres nuevas pruebas de exportación (PNG/GIF/MP4 reales en ambos encuadres, último fotograma comparado, dimensiones, duración y PTS, cancelación sin archivos parciales, conservación de resultado previo, recreación, FileProvider, guardar de verdad en el selector nativo y abrir el menú para compartir sin seleccionar destinatario).
- **Verificación externa correcta** con `tools/verify_exports.py` sobre los seis archivos de cada Android: Pillow decodifica 72 fotogramas GIF, confirma bucle y duración exacta de 6000 ms; ffprobe identifica H.264, 24 fps, 144 fotogramas, 6 s y una sola pista; FFmpeg decodifica el vídeo entero sin errores. PNG alcanza 1080×1080 y 1080×1920. Inspección visual del encuadre y del fotograma final sin inversión vertical.
- El render de vista previa se serializa con un mutex para evitar acceso simultáneo a Canvas/Path al arrastrar rápidamente el control. Progreso y cancelación permanecen fijos fuera del panel desplazable.

[CI final y evidencias](https://github.com/carlossan1993/fourier-art-android/actions/runs/39475764600) · [PR #16](https://github.com/carlossan1993/fourier-art-android/pull/16).

APK: `fourier-art-0.10.0-debug.apk`, 51.766.088 bytes. SHA-256: `b82b9244e866beaf8f50f86bd31fbc33abc0f80fd18a568bbdc0c320e75a8b0a`.

**Pendiente: prueba y aceptación del propietario en teléfono.** Probar una fotografía propia en PNG, GIF y MP4, los dos encuadres y 6/10 segundos; guardar, abrir el archivo fuera de la app y compartir desde el menú nativo. Confirmar el resultado visual con color/fondo/epiciclos/encuadre elegidos. El tiempo de exportación de figuras sintéticas en emulador no acredita rendimiento con todas las fotos en teléfonos.

Las comprobaciones de reconocimiento #3 y dos teléfonos #6 siguen pendientes; emuladores no las sustituyen.
