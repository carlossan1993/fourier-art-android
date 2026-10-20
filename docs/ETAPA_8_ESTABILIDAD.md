# Etapa 8 · Pruebas y estabilización (0.12.0)

Autorizada por Carlos en Work sobre 0.11.0 aceptada y PR #18 integrado. Seguimiento #19.

## Correcciones acotadas
- Una sola configuración de puntos reconstruidos por trazo, sincronizada y ligada al dibujo. Reutiliza puntos entre reproductor, parámetros y exportaciones; las rutas de dibujo mutables siguen separadas. Cambiar componentes sustituye la entrada y no crea un historial de caché.
- Libera el bitmap girado temporal cuando el recorte devuelve otro bitmap. Nunca recicla el original ni el resultado visible.
- El límite de decodificación de 1800 px usa comparación sin truncamiento para no admitir un píxel adicional al submuestrear.
- Cerrar exportación cancela el trabajo y suelta la referencia del dibujo y resultado en el modelo. Un archivo ya compartido permanece disponible durante su vigencia normal.
- Comprueba cancelación entre trazos durante la preparación de exportación y evita publicar progreso antiguo en otra sesión.
- Detección, presupuestos y fórmulas de Fourier conservados.

## Validación reproducible
La CI compila y ejecuta JVM, la batería completa en API 29, la batería de flujos en API 35 y siete comprobaciones de estabilidad/preparación en el mínimo API 24. Registra diagnósticos y limita cada instrumentación a 480 segundos. Validación final correcta: [Actions 39503709155](https://github.com/carlossan1993/fourier-art-android/actions/runs/39503709155), código `b3a3d70b1e38458d1d7a646249042d18a55ae2e9`. **89 JVM (sin omitidas), 28 pruebas API 29, 23 API 35 y siete API 24 correctas**. Los doce archivos PNG/GIF/MP4 pasaron la verificación independiente.

Pruebas nuevas:
- Equivalencia de todos los puntos con Fourier directo, configuración única, recuentos equivalentes y acceso concurrente.
- Imagen JPEG de 3601×2401, ocho combinaciones de giro/recorte y reemplazos inválido e inexistente conservando el trabajo.
- Cuatro ciclos de parámetros, ampliación y PNG, misma instancia de dibujo y puntos, liberación al cerrar y recreación.
- Dibujo sintético de 220 trazos: comparación de cálculo directo/caché, coste software de 60 fotogramas de 360 px, exportación GIF y latencia observada del hilo principal.
- Cierre durante una exportación, ausencia de parciales y nueva exportación compartible después de cerrar.

Pillow y FFmpeg verifican en el host los doce archivos PNG/GIF/MP4 producidos por API 29/35. `tools/report_stability.py` conserva conteos de pruebas y observaciones en `stage8-summary.json`.

## Límites
Las mediciones de una APK debug en emuladores no acreditan FPS, picos máximos de memoria, ausencia universal de fugas o rendimiento en todos los teléfonos. Los datos de memoria son muestras PSS/heap antes y después, no un perfil continuo. El banco visual >=24/30 (#3), la matriz física de dos teléfonos (#6) y la combinación simultánea de letra al 150% con rotación siguen pendientes. No se marca esta etapa cerrada sin registrar los resultados y la aceptación del propietario.

## Mediciones observadas
Dibujo sintético de 220 trazos y máximo 400 componentes; 60 fotogramas Canvas software a 360 px. Los números de caché corresponden a veinte pasadas completas, frente a una sola pasada directa; no deben compararse como cargas iguales ni convertirse en una promesa de FPS. Se verificó equivalencia exacta de puntos y reutilización por identidad.

| Android | Trazado directo, 1 pasada (ms) | Caché, 20 pasadas (ms) | Render software p95 (ms) | GIF 6 s (s) | PSS antes → después (MiB) |
| --- | ---: | ---: | ---: | ---: | ---: |
| 7 | 81.31 | 9.65 | 31.81 | 1.96 | 63.5 → 78.9 |
| 10 | 129.32 | 11.85 | 27.97 | 1.76 | 148.9 → 170.8 |
| 15 | 104.33 | 44.28 | 39.46 | 2.41 | 163.8 → 174.4 |

La exportación GIF tuvo 108–149 pulsos del hilo principal durante el trabajo, con separación máxima observada de 17–19 ms. El cierre y cancelación midió 21,75–26,77 ms y no dejó parciales. Cuatro ciclos de parámetros/ampliación/PNG conservaron la misma instancia de dibujo y puntos, y soltaron la referencia de exportación. La imagen 3601×2401 se decodificó a 901×601 (2.166.004 bytes); entradas inválida e inexistente conservaron el trabajo.

El PSS antes/después es una muestra del proceso completo, con modelos, vistas, datos de prueba y memoria que puede quedar pendiente de GC. No es una comparación de consumo contra 0.11.0 ni una prueba de ausencia de fugas. El coste Canvas software tampoco representa el lienzo acelerado en pantalla. No se cambia el heap declarado de la aplicación.

[Resumen íntegro](evidence/stage8-summary.json) · [Evidencias CI](https://github.com/carlossan1993/fourier-art-android/actions/runs/39503709155/artifacts/12301644806) · [APK 0.12.0](https://github.com/carlossan1993/fourier-art-android/actions/runs/39503709155/artifacts/12301259877).

El primer intento pasó Android pero no encontró ffprobe en el host. La CI ahora instala FFmpeg/Pillow antes de compilar y la verificación final completa es correcta. Los commits documentales posteriores conservan el código del APK verificado.

## Salida pendiente
Parte técnica preparada para prueba del propietario; PR #20 todavía sin integrar. La aceptación de esta APK y los pendientes físicos/visuales se registran por separado. [Ficha de dos teléfonos y rúbrica](VALIDACION_FISICA_Y_RECONOCIBILIDAD.md) · [Banco de 30 filas sin resultados](BANCO_RECONOCIBILIDAD.csv). #3 y #6 siguen abiertos: no se declara cerrada toda la Etapa 8 ni lista la publicación por pasar estos emuladores.
