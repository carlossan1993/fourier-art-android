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
La CI compila y ejecuta JVM, la batería completa en API 29, la batería de flujos en API 35 y siete comprobaciones de estabilidad/preparación en el mínimo API 24. Registra diagnósticos y limita cada instrumentación a 480 segundos. Estado inicial: validación pendiente; completar con el enlace de ejecución y resultados.

Pruebas nuevas:
- Equivalencia de todos los puntos con Fourier directo, configuración única, recuentos equivalentes y acceso concurrente.
- Imagen JPEG de 3601×2401, ocho combinaciones de giro/recorte y reemplazos inválido e inexistente conservando el trabajo.
- Cuatro ciclos de parámetros, ampliación y PNG, misma instancia de dibujo y puntos, liberación al cerrar y recreación.
- Dibujo sintético de 220 trazos: comparación de cálculo directo/caché, coste software de 60 fotogramas de 360 px, exportación GIF y latencia observada del hilo principal.
- Cierre durante una exportación, ausencia de parciales y nueva exportación compartible después de cerrar.

Pillow y FFmpeg verifican en el host los doce archivos PNG/GIF/MP4 producidos por API 29/35. `tools/report_stability.py` conserva conteos de pruebas y observaciones en `stage8-summary.json`.

## Límites
Las mediciones de una APK debug en emuladores no acreditan FPS, picos máximos de memoria, ausencia universal de fugas o rendimiento en todos los teléfonos. Los datos de memoria son muestras PSS/heap antes y después, no un perfil continuo. El banco visual >=24/30 (#3), la matriz física de dos teléfonos (#6) y la combinación simultánea de letra al 150% con rotación siguen pendientes. No se marca esta etapa cerrada sin registrar los resultados y la aceptación del propietario.
