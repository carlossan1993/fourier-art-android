# Etapa 3 — Contornos editables (0.7.0)

## Alcance

Se añaden herramientas de corrección sobre la detección 0.5.0 aceptada, sin cambiar sus umbrales, presupuestos ni prioridades. El resultado predeterminado sigue siendo Detalles, sin limpieza adicional ni comandos. No hay dependencias nuevas, backend ni carga de fotografías.

- Detalles conserva el resultado detectado completo; Silueta usa el contorno de máscara ya obtenido por el motor, o los óvalos faciales cuando no hay máscara. No se inventa un perímetro si ninguna fuente es fiable.
- Limpiar elimina trazos detectados cortos según la diagonal del raster (cero por defecto). Los dibujos correctivos posteriores no se eliminan por ese filtro.
- Conservar zona recorta los trazos a un rectángulo elegido sobre la fotografía preparada. Es una selección geométrica manual, no identifica personas ni vuelve a ejecutar segmentación semántica.
- Borrador corta segmentos con un disco barrido entre eventos táctiles, incluyendo segmentos largos y el cierre de un contorno. Las partes restantes son trazos abiertos independientes; no se dibuja un puente entre ellas.
- Dibujar añade trazos abiertos. Zoom 1–4x y Mover permiten corregir detalles; ocultar foto facilita revisar las líneas. Las coordenadas de imagen y raster son las mismas con zoom/desplazamiento.
- Deshacer/rehacer conserva hasta 20 estados; Volver a la detección también se puede deshacer. Aplicar calcula Fourier a partir del contorno corregido, Cancelar mantiene el aplicado anterior. Un resultado intencionadamente vacío borra la animación previa.
- La receta aplicada se guarda junto al trabajo privado de Etapa 2 y se vuelve a aplicar al recuperar la foto. Los gestos aún no aplicados son un borrador en memoria; al terminar el proceso se conserva la última corrección aplicada. Al volver a procesar la misma foto se conserva la receta; al aplicar un nuevo recorte/giro o importar otra foto se reinicia, porque cambian las coordenadas.
- Sin resultado automático también puede abrirse el editor y dibujar líneas. Permite una alternativa manual cuando no hay modelo de sujeto; no constituye detección automática offline de cualquier sujeto.

## Límites de recursos

Máximo 100 gestos por receta y 512 puntos por gesto, reduciendo muestras intermedias si es necesario. Radio del borrador relativo al lado corto de la imagen, independiente del zoom. El cálculo geométrico y Fourier se ejecutan fuera del hilo UI. La vista previa geométrica no recalcula Fourier en cada evento táctil; lo hace al aplicar. El esquema JSON se valida y, si está dañado o tiene versión desconocida, se recupera la foto con la detección predeterminada.

## Validación

Pruebas JVM: conservación del resultado predeterminado, cortes en trazos abiertos/cerrados, barrido entre eventos, ausencia de puentes, conservación de región sin añadir bordes, coordenadas no cuadradas, limpieza, historial y valores inválidos.

Pruebas Android 10/15: dibujar/borrar/seleccionar mediante gestos, deshacer/rehacer, aplicar, recrear actividad, recuperar en un ViewModel nuevo desde archivos privados, cancelar, restaurar y editar una foto sin contornos detectados. JSON: ida/vuelta y metadatos inválidos. Las pruebas previas de cámara, EXIF y calidad se mantienen. Consultar Actions del commit para resultados efectivos.

Pendientes de aceptación: prueba en teléfono de 0.7.0 y reconocibilidad formal >=24/30 en #3. La aceptación de 0.6.0 no equivale a cumplir la matriz física de dos dispositivos; permanece en #6. Seguimiento de esta etapa: #9.

Referencia de gestos: https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures
