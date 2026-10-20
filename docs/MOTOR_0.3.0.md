# Motor experimental 0.3.0 — revisión G1

## Problema y cambio

0.2.2 comparaba el color de cada píxel con el promedio del borde de una foto de 192 px y retenía una sola silueta. No disponía de información facial ni separación semántica de fondo. Aumentar armónicos no puede recuperar esos rasgos.

0.3.0 usa tres perfiles explícitos:

- **Auto:** ML Kit Face Detection con contornos del rostro principal: óvalo, ojos, cejas, nariz y labios. Si no obtiene rasgos suficientes, intenta segmentación del sujeto.
- **Sujeto:** ML Kit Subject Segmentation, componente principal de máscara y detalles internos mediante Sobel suavizado, supresión de no máximos e histéresis. Los detalles se restringen al interior erosionado de la máscara. Máximo 5 contornos y 24 trazos internos.
- **Dibujo:** bordes y trazos sin modelos, disponible sin Play services. No separa un sujeto de un fondo fotográfico. Máximo 40 trazos.

Todos los trazos comparten origen y escala, conservan la proporción y tienen DFT independiente. Un trazo abierto usa extensión reflejada de ida/vuelta: solo se anima la mitad de ida, sin cerrar una ceja o nariz con una línea artificial. La animación levanta el lápiz entre trazos y distribuye el tiempo según longitud.

La vista previa superpone los trazos originales sobre la foto orientada. «Ver resultado completo» permite evaluar la reconstrucción estática sin epiciclos; la animación también mantiene tres segundos el dibujo terminado antes de reiniciar. Al fallar una foto se borra la reconstrucción anterior para evitar confundirla con el nuevo resultado. Se conserva la corrección EXIF de 0.2.2. La inferencia se almacena solo en memoria para no repetirla al ajustar sensibilidad.

## Modelos y dependencias

- `com.google.mlkit:face-detection:16.1.7`: modelo facial incluido, disponible sin descarga posterior. Guía: https://developers.google.com/ml-kit/vision/face-detection/android
- `com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1`: modelo distribuido por Play services; puede necesitar conexión y espera inicial. Guía: https://developers.google.com/ml-kit/vision/subject-segmentation/android
- Los SDK de Google están sujetos a las condiciones de ML Kit/Google APIs indicadas en sus páginas oficiales. No son código del proyecto ni se redistribuyen sus fuentes. No se añade servicio de pago.
- No se incorpora OpenCV todavía: máscaras, bordes y separación de trazos están en Kotlin puro y se prueban sin librerías nativas adicionales. Esta decisión se puede revisar si el banco de fotografías muestra que hace falta.

La imagen se procesa localmente; el código no contiene cliente de subida de fotos ni servidor del proyecto. La descarga del modelo de sujeto depende de Google Play services. Las declaraciones de privacidad definitivas deberán reflejar las dependencias/manifest fusionado antes de publicar. No se asume que añadir SDK conserve automáticamente las declaraciones anteriores.

## Verificación automatizada

13 pruebas JUnit superadas en JVM local antes del commit; CI repetirá las pruebas con la configuración Android del proyecto y compilará el APK.

JUnit comprueba Fourier, reconstrucción discreta, extensión de trazos abiertos (incluido un segmento de dos puntos), transformación compartida, concavidades y agujeros, contactos diagonales, rechazo del marco, eliminación de ruido y separación de ramas/trazos. Las imágenes sintéticas se generan en las pruebas: no se publican fotografías personales.

## Verificación pendiente en dispositivo (G1 sigue abierto)

1. Instalar APK 0.3.0 y confirmar círculo/estrella.
2. Foto frontal cercana: seleccionar Auto y comprobar ojos, nariz y boca en la superposición y animación.
3. Foto de mascota/objeto con fondo complejo: Sujeto; esperar descarga inicial si hace falta y reintentar. Revisar silueta y detalles, ajustar sensibilidad.
4. Dibujo/logotipo: Dibujo; comprobar trazos y separación.
5. Foto vertical EXIF, imagen reflejada, imagen sin sujeto, modelo ausente y funcionamiento offline después de descarga.
6. Registrar tiempo de procesamiento y claridad en el Samsung del usuario; después ampliar a dos dispositivos y banco oficial de 30 fotografías.

No se declara calidad satisfactoria de retratos/mascotas ni G1 superado por compilar. Pelo, gafas, rostro lateral y pelaje/fondos parecidos siguen siendo casos difíciles. La edición manual y recorte siguen pendientes en sus etapas.
