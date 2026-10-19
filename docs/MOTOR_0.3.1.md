# 0.3.1 — Calidad del retrato (G1 abierto)

## Fallo observado

El usuario probó un retrato nítido de fondo blanco. La superposición en Dibujo ya omitía un ojo antes de Fourier. El filtro retenía solo los 40 trazos más largos y descartaba fragmentos cortos. Además, Auto devolvía solo los contornos faciales y omitía cabello/cuerpo; el puente nasal de ML Kit, de **2 puntos**, se descartaba por exigir al menos 3.

## Correcciones

- Rasgos semánticos protegidos tanto en Auto como en Dibujo, sin aplicarles el presupuesto o filtro de longitud de los bordes. Ambos ojos y nariz/boca se conservan si el modelo los entrega. Un rostro incompleto se identifica como parcial.
- Decodificación hasta 1800px; detección facial en la imagen orientada y segunda pasada en un recorte de la cara. Puntos transformados al raster común de 768px. Esto evita destruir detalle antes de inferir, pero no recupera información ausente en la foto.
- Composición de rasgos con cabello, orejas, silueta y contexto. Se evita duplicar sombras/bordes genéricos en el interior del óvalo.
- Cejas y labios compuestos en contornos; puente nasal de 2 puntos preservado; base nasal ordenada pasando por el punto central. Iris solo cuando hay una región oscura observada dentro del ojo, sin inventar pupilas.
- Presupuesto repartido por regiones, rasgos faciales fuera de ese presupuesto. Reconexión de fragmentos únicamente si extremos, distancia y tangentes son compatibles; suavizado leve de bordes de píxeles.
- Fondo liso: color dominante y flood-fill desde el borde, con comprobación de área. Ruta local sin descarga para fondos blancos favorables. Fondo complejo: segmentación de sujeto; si no hay máscara y sí rostro, limitar contexto a cabeza/cuello para reducir fondo.
- Siluetas recortadas por hombros: eliminar los tramos del marco sin rechazar toda la persona. Los fragmentos resultantes son abiertos, no se cierran con líneas artificiales.
- Reconstrucción inicial con 200 componentes; sensibilidad ajusta el contexto sin eliminar rasgos protegidos.

## Validación

Pruebas JVM: núcleo matemático, rostro incompleto, permanencia de ojos/nariz frente a muchas líneas de ropa, geometría de nariz, iris observado, reconexión conservadora, cobertura por regiones, ausencia de truncado a 40, fondo plano/complejo y hombros recortados. Resultado y APK se verifican en Actions antes de entregar.

Se añade prueba instrumentada Android API 29 con el modelo facial **real** y una fotografía pública de NASA. Comprueba presencia de ambos ojos, puente nasal y boca en la geometría, protección de los trazos durante composición, valores finitos y generación de una comparación visual anterior/nueva/Fourier. La prueba produce `portrait-quality-evidence` y métricas, con retención de 3 días. El APK se genera tras pasar la prueba.

La foto pública se descarga de una versión fijada de scikit-image y se verifica por SHA-256; no se incluye en el APK ni se sube ninguna foto personal. La captura del usuario identifica el fallo, pero no sustituye a su foto original para evaluar fidelidad. Revisar el resultado del emulador tampoco sustituye al Samsung ni al banco de 30 fotografías. G1 sigue abierto.

## Fuentes

- ML Kit: contornos faciales, incluido puente nasal de 2 puntos y base nasal con punto central primero: https://developers.google.com/ml-kit/vision/face-detection/face-detection-concepts
- Requisitos de resolución: https://developers.google.com/ml-kit/vision/face-detection/android
- Fotografía Eileen Collins, NASA, dominio público según documentación de scikit-image: https://scikit-image.org/docs/0.25.x/api/skimage.data.html#skimage.data.astronaut
- Fuente fijada y hash en `.github/workflows/android-ci.yml`; fixture excluida en `.gitignore`.
