# 0.4.0 — Detalle ajustable, continuidad y varias caras (G1 abierto)

## Problema

Las capturas de 0.3.1 muestran un contador próximo a 100/102: la ruta genérica retenía 100 detalles más siluetas; retrato, 64 detalles de contexto más rasgos. Ese presupuesto es una decisión del motor, no un límite de Fourier. También se conservaba solo el componente mayor de la máscara y solo se refinaba una cara.

## Cambios

- Control independiente **Simple / Medio / Fino**: hasta 100 / 220 / 400 detalles. Siluetas y rasgos faciales protegidos se añaden aparte. Medio es el valor inicial. La cantidad final depende de los contornos disponibles; no se generan líneas para alcanzar el presupuesto.
- Selección por contraste observado, persistencia tras suavizado y cobertura espacial suave. El fondo y las texturas pueden seguir compitiendo en Dibujo: esta clasificación no equivale a comprender semánticamente un objeto.
- Reconexión por extremos y tangentes compatibles, con búsqueda espacial y cadenas completas. Evita saltos laterales entre líneas paralelas y ciclos artificiales; ya no depende de dos pasadas de unión.
- Fondo plano y máscara de segmentación conservan hasta 16 componentes significativos; se eliminan puntos diminutos. Se admiten más contornos exteriores y huecos. Un objeto muy pequeño respecto al sujeto mayor todavía puede quedar fuera.
- Detección de cajas de todas las caras sin modo contorno; recorte y extracción de contornos por cara. Límite operativo de 8 caras y 30 segundos para esa fase. La interfaz muestra contornos obtenidos frente a caras detectadas. Un recorte fallido no borra las otras caras. La detección incompleta se informa como retrato parcial.
- Composición conjunta de los rostros sin duplicar el contexto; en un rostro incompleto se conservan bordes raster para no borrar rasgos que el modelo no entregó. Dibujo conserva el contexto de la escena; Auto restringe el fondo a regiones próximas a las cabezas si falta máscara.
- Raster máximo 1024 px (antes 768). Muestreo Fourier por longitud: 64–1024 puntos por trazo. Hasta 800 componentes en el control, 400 inicialmente; cada trazo utiliza como máximo los coeficientes disponibles.
- FFT en lugar de DFT cuadrática y evaluación recurrente de trazas. Preparación de las trazas de animación fuera del hilo principal. Solo líneas, sin sombreado o rellenos.

## Verificación y evidencia

33 pruebas JVM del núcleo: equivalencia FFT/DFT independiente, trazado recurrente, contornos densos, cadenas largas desordenadas, ausencia de puentes laterales, preservación de dos sujetos separados, ranking de contraste, presupuestos anidados y composición con dos pares de ojos. Se ejecutan también en Actions.

La prueba Android usa el modelo facial real con imágenes públicas fijadas por SHA-256:

1. Retrato NASA: ojos y zona nasal preservados; comparación Simple/Medio/Fino en Fourier.
2. Composición de tres copias del retrato a diferentes tamaños: prueba técnica de detección, refinamiento y coordenadas de múltiples caras. **No sustituye una foto familiar real con distintas edades, poses u oclusiones.**
3. Gato Chelsea: comparación de los tres presupuestos y comprobación de que Fino supera 200 trazos.
4. Dos sujetos separados sobre blanco: ambas siluetas deben sobrevivir.

`portrait-quality-evidence` contiene imágenes comparativas y métricas de número de trazos y tiempo en emulador. Retención de 3 días; APK 7 días. Consultar la ejecución del commit para el resultado efectivo. Las imágenes de prueba no forman parte del APK de usuario. No se publican fotografías personales.

La comparación local del gato permite revisar rasgos y textura, pero no acredita calidad universal ni rendimiento en el Samsung. El modo Fino puede ser demasiado denso: usar Simple cuando se busque simplificación extrema. G1 y PR #2 permanecen abiertos hasta evaluar las fotografías originales y el banco acordado; no se declara GO por compilar.

## Fuentes de los modelos y fixtures

- ML Kit: el modo contorno devuelve solo la cara prominente; por ello se detectan cajas primero y se procesa cada recorte: https://developers.google.com/ml-kit/vision/face-detection/android
- Retrato Eileen Collins, NASA; gato Chelsea, Stefan van der Walt, CC0: https://scikit-image.org/docs/0.25.x/api/skimage.data.html
- Fuentes fijadas en scikit-image v0.25.2 y hashes en `.github/workflows/android-ci.yml`.
