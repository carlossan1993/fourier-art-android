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

Pendiente de completar: JVM (timeline, dimensiones, LZW decodificado por ImageIO), Android 10 y 15 (PNG/GIF/MP4 reales en ambos encuadres, decodificación final, tiempos, cancelación, UI, guardar/compartir), inspección externa de evidencias. Tras CI se entrega APK para prueba física. Las comprobaciones de reconocimiento #3 y dos teléfonos #6 siguen pendientes; emuladores no las sustituyen.
