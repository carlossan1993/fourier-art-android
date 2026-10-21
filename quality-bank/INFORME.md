# Evaluación visual de FOURIER ART 0.12.0

**Diagnóstico completo en Dibujo: 21/30 reconocibles automáticamente (70%). La meta formal >=24/30 no queda acreditada.** El banco con los perfiles primarios Auto/Sujeto/Dibujo está incompleto por un bloqueo del modelo de segmentación en los emuladores. Se conservan ambos resultados por separado.

Carlos omitió la prueba física en dos teléfonos por no disponer de ellos; #6 cerrado como `not_planned`. No se registra esa prueba como realizada. La base 0.12.0 fue aceptada por Carlos y PR #20 está integrado.

## Resultado visual del diagnóstico

| Categoría | Reconocibles sin edición | Insuficientes automáticos | Total |
| --- | ---: | ---: | ---: |
| Personas / grupos | 9 | 1 | 10 |
| Mascotas | 2 | 8 | 10 |
| Objetos / dibujos | 10 | 0 | 10 |
| **Total** | **21** | **9** | **30** |

Los nueve casos insuficientes son P10, M01–M05, M07, M08 y M10. No se hicieron correcciones manuales: cero acciones y cero segundos de edición; no se asigna puntuación 1 ni se presume que una corrección hipotética funcionaría. Un 0 refleja el resultado automático observado, no que sea imposible mejorarlo con edición.

Los retratos conservan ojos, nariz y boca, aunque aparecen narices deformadas, cabello incompleto y líneas sobrantes. El grupo conserva sus siete caras y su disposición. Los objetos mantienen partes que permiten distinguirlos, pero también quedan trazos duplicados o incompletos. En mascotas, la silueta sola no basta: varios resultados pierden ojos o hocico, o el fondo consume el trazado. El perro con lengua visible M05 se revisó también a 1024 px y no pasó por la pérdida de definición de nariz/hocico. M07 y M09 también se revisaron ampliados.

[Las 30 comparaciones y motivos](GALERIA_DIBUJO.md) · [CSV del diagnóstico](DRAWING_DIAGNOSTIC.csv) · [Notas estructuradas](drawing-ratings.json) · [Mediciones nativas](evidence/drawing-results.json) · [Resumen](evidence/drawing-summary.json).

## Banco primario: medición incompleta

La primera ejecución conservó los perfiles fijados antes de medir: Auto para personas, Sujeto para mascotas y fotografías de objetos, Dibujo para ilustraciones. Intentó las 30 entradas: produjo 16 dibujos, de los que 15 fueron reconocibles automáticamente y P10 resultó insuficiente. Los otros 14 casos devolvieron `MODEL_UNAVAILABLE`; quedan **sin puntuación**, porque no permiten juzgar la calidad visual del algoritmo con su modelo disponible. No se calcula ni extrapola un porcentaje de éxito de los perfiles primarios a partir de esas 16 salidas.

[CSV primario](../docs/BANCO_RECONOCIBILIDAD.csv) · [Salidas PNG primarias](GALERIA_PRIMARIA.md) · [Resultados originales](evidence/initial-results.json) · [Entorno inicial](evidence/initial-environment.json).

En el segundo entorno, con Play Store, el módulo sí se descargó. Su ejecución falló con `MediaPipeException` / `Tensor should have 2, 3, or 4 dims, received: 0`, seguido de cierre del proceso. El fallo se reprodujo con SwiftShader, ANGLE/SwiftShader y Mesa; los dos últimos anunciaron OpenGL ES 3.1. La ejecución final 39548038419 intentó procesar una imagen real antes de cerrar el cliente preparatorio y también devolvió `Failed to run thin subject segmenter`, además del fallo nativo. Así se descartó que cerrar el cliente sin imagen fuera la única causa. [Error de inferencia final](evidence/warmup-environment.json) · [Extracto nativo](evidence/warmup-runtime-excerpt.txt). La bandera de disponibilidad de ese intento fue sobrescrita por el error de inferencia; se conserva el registro literal y el recolector posterior separa instalación y calentamiento. Es un bloqueo observado en estos emuladores, **no una prueba de que falle el teléfono del propietario**. La disponibilidad del módulo por sí sola no acredita que pueda procesar una imagen.

El diagnóstico adicional usa Dibujo para las mismas 30 imágenes, con los otros parámetros idénticos y sin cambiar código de producción. Ese perfil evita el modelo de sujeto y permite revisar las salidas completas; **no sustituye ni completa el resultado primario**. Tampoco se mezclan sus mejores resultados con los del primer intento.

## Método y trazabilidad

- 30 imágenes públicas diferentes, con CC0 o dominio público indicado en su fuente: diez personas/grupos, diez mascotas, cinco fotos de objetos y cinco ilustraciones. Selección de conveniencia, no muestreo estadístico; los retratos proceden de NASA. [Fuentes y límites](README.md).
- Selección y parámetros fijados antes de obtener salidas. Manifiesto SHA256 `1a60f1d20c4e17cf62e951b9ff18da1ecab17f0b29713157d1ab0dc93c65ecfe`.
- Las 30 entradas normalizadas están versionadas y se comprueba su SHA256 antes de procesar. No se sustituyeron casos difíciles. Solo se redujo hasta 1024 px, conservando proporción y orientación, sin recorte ni mejora selectiva.
- Producción `5d2f3c8f4679d637a6a17642e41a49e43540ab36` (0.12.0), sin cambios en `app/src/main/`. Se ejecutaron `PhotoContours` y el `ExportRenderer` reales, a dibujo completo y 400 componentes máximos.
- Misma rúbrica visual 0/1/2 previamente documentada. Evaluador: Codex, revisión visual por IA junto al original; no es un panel humano independiente ni acredita fidelidad universal o identificación personal.
- Diagnóstico Android 10/API29 debug: 30 salidas, sin errores del recolector; mediana de análisis/extracción/render de 1993,5 ms y máximo de 4993 ms. No son tiempos de interfaz ni de un teléfono físico.

## Evidencia de ejecución

| Ejecución | Resultado |
| --- | --- |
| [39544852641](https://github.com/carlossan1993/fourier-art-android/actions/runs/39544852641) | Colección primaria: 30 intentos, 16 salidas, 14 bloqueadas por módulo ausente. Un recolector correcto no equivale a aprobar calidad. |
| [39545783179](https://github.com/carlossan1993/fourier-art-android/actions/runs/39545783179) | Módulo disponible; fallo nativo en emulador con Play Store/SwiftShader. |
| [39546195341](https://github.com/carlossan1993/fourier-art-android/actions/runs/39546195341) | Descarga remota distinta rechazada por hash antes de medir. Se versionaron las entradas congeladas. |
| [39546310407](https://github.com/carlossan1993/fourier-art-android/actions/runs/39546310407) | Fallo nativo con ANGLE/SwiftShader y GLES 3.1. |
| [39546563070](https://github.com/carlossan1993/fourier-art-android/actions/runs/39546563070) | Repite el bloqueo en el entorno ANGLE con las entradas versionadas. |
| [39546908229](https://github.com/carlossan1993/fourier-art-android/actions/runs/39546908229) | Fallo nativo con Mesa y GLES 3.1. |
| [39548038419](https://github.com/carlossan1993/fourier-art-android/actions/runs/39548038419) | El modelo falla también al procesar una imagen real antes del cierre del cliente preparatorio. |
| [39546562978](https://github.com/carlossan1993/fourier-art-android/actions/runs/39546562978) | Diagnóstico Dibujo completo: 30 PNG reales, 21 reconocibles y nueve insuficientes según esta revisión. |

Las entradas, PNG y comparaciones quedan versionados para conservar la evidencia después de la caducidad de los artefactos de Actions. Los workflows de banco quedan disponibles bajo ejecución manual; no se repite automáticamente un entorno que ya mostró esta limitación.

## Estado

La evaluación visual de las 30 imágenes en Dibujo está realizada y documentada. El criterio formal de #3 permanece **sin acreditar**, tanto por el bloqueo de los perfiles primarios como porque este diagnóstico automático quedó por debajo de 24/30. El 21/10/2026 Carlos aceptó el resultado y cerró la Etapa 8: «bueno, algo es algo, lo mejoramos luego. Damos esta etapa por cerrado». #19 se cierra por aceptación del propietario con excepciones; #3 permanece abierto para mejoras posteriores. Este trabajo no mejora el motor ni modifica la versión 0.12.0 aceptada. La omisión de los dos teléfonos y la combinación de letra al 150% con rotación sin verificar se conservan como excepciones, sin atribuir pruebas no realizadas.
