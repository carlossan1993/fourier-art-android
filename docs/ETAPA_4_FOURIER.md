# Etapa 4 — Motor Fourier definitivo (0.8.0)

El propietario aprobó 0.7.0 y PR #10 fue integrado. El 20/10/2026 pidió continuar esta etapa en Work, cambiando el reparto original. Objetivo: #11.

## Cálculo y precisión

Se conserva la FFT radix-2 local: transformada compleja con exponente negativo y normalización 1/M; síntesis con exponente positivo. Se validan puntos finitos, perímetro, tamaño de muestras y parámetros de render. El remuestreo sigue siendo uniforme por longitud de arco, entre 64 y 1024 muestras por trazo según su longitud, con una transformación común para todos los trazos.

La componente DC coloca la cadena y queda primera. Para curvas cerradas, el resto se ordena por energía descendente, con desempates explícitos. Así se retienen los coeficientes que minimizan error cuadrático discreto para un mismo presupuesto (DC fija). En trazos abiertos se priorizan grupos +/-k por energía media; se selecciona el grupo completo para conservar la simetría de la extensión de ida/vuelta. Nyquist es un grupo individual: en curvas reflejadas su interpolación es cosenoidal y se dibuja mediante dos semivectores contrarrotantes, conservando la simetría también entre las muestras. La cantidad efectiva puede ser menor que el máximo elegido y se muestra en pantalla. La vuelta matemática de un trazo abierto permanece oculta; no se añade un cierre recto.

El error RMS mediante Parseval suma la energía de coeficientes omitidos. Se compara con medición directa frente a las muestras de entrada. Aumentar el presupuesto conserva o reduce ese error; a veces hay mesetas por grupos o porque el trazo ya usa todos sus coeficientes. Esto mide reconstrucción del contorno remuestreado, no parecido con la foto ni recuperación de detalles que no fueron detectados.

## Render determinista

DrawingTimeline convierte un progreso 0–1 en trazos completados y un trazo activo, ponderando por longitud visible e independientemente de N. En cada frontera se levanta el lápiz; no se genera un segmento hacia el siguiente trazo. Al terminar no queda una cadena activa. La pantalla usa esa misma secuencia, apta como base para futura exportación, sin introducir aún exportadores ni controles de velocidad.

La vista previa usa al menos cuatro intervalos por ciclo de la frecuencia retenida más alta y un mínimo de 128. Las curvas actuales requieren como máximo 2048 intervalos cerrados o 1024 abiertos. El cálculo de los rastros se mantiene fuera del hilo UI; cambiar N no vuelve a detectar la foto. La punta y el extremo parcial se evalúan en el mismo tiempo, sin limitar la punta al último segmento precalculado. Los extremos del rastro se anclan a evaluación directa para evitar deriva de la recurrencia.

## Verificación

Pruebas JVM: FFT frente a DFT independiente con varios tamaños/frecuencias; Parseval frente a error medido, monotonía, selección por energía, reconstrucción completa, simetría abierta por presupuesto, límites, repetibilidad, muestreo y cronología sin puentes. Se mantienen las pruebas previas de geometría y contornos.

Android 10/15: cambiar N desde el control, conservar la misma instancia de dibujo, revisar componentes efectivos, pasar de resultado completo a animación y producir capturas con N=10 y N=800. Una curva sintética densa genera paneles N=10/50/200/800 y métricas RMS repetibles. Se mantienen cámara, edición/recuperación, EXIF y calidad del detector. Consultar Actions del commit para resultados efectivos.

Pendiente: probar 0.8.0 en teléfono. El banco formal de reconocibilidad >=24/30 de #3 y la matriz física de dos teléfonos de #6 siguen pendientes. No se declaran satisfechos por las pruebas matemáticas.

Referencias: https://www.fftw.org/fftw3_doc/What-FFTW-Really-Computes.html (convenciones); https://developer.android.com/develop/ui/compose/side-effects (cancelación del trabajo de UI). No se incorpora FFTW ni otra dependencia.
