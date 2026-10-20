# 0.5.0 — Formas principales y textura (experimental)

## Base aceptada

0.4.0 fue aceptada en dispositivo por el propietario el 19/10/2026 y se integró mediante PR #2. Las referencias satisfactorias incluyen escenas con rostros, grupo familiar y mascota. La foto de un perro claro mantiene la silueta, pero mezcla pelaje, hocico y boca. Esa pérdida aparece ya en la superposición, antes de Fourier.

## Cambio de extracción

Se añaden dos tipos de trazado independientes del perfil Auto/Sujeto/Dibujo y del nivel Simple/Medio/Fino:

- **Estructura** (inicial): suavizado con pesos espaciales y de diferencia RGB, gradiente vectorial de color, contornos a resolución completa y media, prioridad para límites persistentes y eliminación de tramos duplicados cercanos. Se añaden límites observados de regiones oscuras compactas para recuperar formas que las cadenas de bordes fragmentan.
- **Textura**: conserva la ruta de extracción de 0.4.0 para comparar y recuperar sus resultados. El grosor de visualización se reduce en dibujos densos; la geometría de esta ruta se conserva.

El suavizado es una aproximación bilateral separable, no una implementación Lab del artículo de referencia. El gradiente usa el tensor RGB y su dirección dominante; puede detectar una transición de color que casi desaparece al pasar a luminancia. La escala media ayuda a evitar gastar el presupuesto en detalle fino. La escala completa complementa esas líneas sin duplicarlas. Las uniones en cruces siguen únicamente píxeles existentes con continuidad de dirección. La simplificación geométrica elimina escalones con tolerancia acotada.

Los límites de las regiones oscuras salen de los píxeles de la imagen, con apertura conservadora, comprobaciones de tamaño/compacidad y exclusión del marco. No se dibujan rellenos. **No se añade un modelo anatómico de mascotas**: una sombra conectada a la nariz aún puede impedir separar bien su borde. No se inventa una nariz ideal.

Los presupuestos siguen siendo máximos de 100/220/400 detalles, más siluetas y rasgos humanos. Estructura puede producir menos trazos incluso en Fino: no se rellena el presupuesto con textura descartada. Los rasgos humanos de todas las caras continúan protegidos mediante el compositor existente.

## Validación

40 pruebas JVM: las 33 existentes y 7 adicionales sobre color de luminancia similar, suavizado que conserva límites fuertes, imágenes planas/transparencia, respeto de máscara, regiones compactas, continuidad en cruces sin dibujar huecos y simplificación de curvas cerradas.

Actions genera comparaciones reales en Android con ambos tipos de trazado y el modelo facial incluido. Conserva las pruebas de retrato NASA, tres caras compuestas y dos sujetos sobre blanco; amplía la comparación del gato y añade una referencia pública de perro del repositorio PyTorch Hub. Las descargas se fijan por revisión y SHA-256; las fotos no se incluyen en el APK de usuario.

La foto original del perro aportada por el usuario se usa **solo localmente** para comparar la etapa raster. Esa comparación no usa la máscara de Google Play services y no debe confundirse con una validación completa del perfil Sujeto en el teléfono. Se observa recuperación parcial del límite boca/lengua y menos pelaje; no se declara resuelto el parecido del hocico. Las referencias humanas recibidas como capturas anotadas no sustituyen a sus originales para una regresión exacta.

Consultar la ejecución de Actions del commit para el resultado efectivo, imágenes y tiempos. El 19/10/2026 el propietario eligió conservar 0.5.0, integrar PR #5 y pasar a la siguiente etapa. Se registra esa decisión de continuidad con las limitaciones conocidas. G1 formal (24/30) continúa sin medir en issue #3; no se declara cumplido. Las mejoras adicionales del motor quedan aparcadas por ahora.

## Referencias

- Di Zenzo, gradiente de imagen multicanal: https://people.csail.mit.edu/tieu/notebook/imageproc/dizenzo86.pdf
- Tomasi y Manduchi, suavizado bilateral: https://users.cs.duke.edu/~tomasi/papers/tomasi/tomasiIccv98.pdf
- Foto de perro usada como ejemplo público por PyTorch Hub: https://github.com/pytorch/hub/blob/3b54eaafd931b0c0f3ee2df391bb4be7ba6f1ca3/images/dog.jpg
- Retrato NASA y gato Chelsea CC0: fuentes en MOTOR_0.4.0.md y hashes en el workflow.
