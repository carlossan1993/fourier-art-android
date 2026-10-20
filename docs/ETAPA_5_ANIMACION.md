# Etapa 5 — Animación y controles (0.9.0)

El propietario probó 0.8.0, indicó «lo veo bien» y pidió avanzar el 20/10/2026. PR #12 integrado. Se continúa en Work por su elección. Objetivo: #13.

## Comportamiento

- Pausar conserva exactamente el punto; Reproducir continúa desde allí. Reiniciar vuelve al comienzo y reproduce. El avance manual pausa para revisar un instante preciso. Resultado completo permite inspeccionar todos los trazos; volver a la animación conserva su posición anterior.
- Velocidades Lenta (0,5x), Normal (1x), Rápida (2x) y Muy rápida (4x), sin volver al comienzo al cambiarlas. A velocidad normal se dibuja en 12 segundos, se mantiene el resultado 2,4 segundos y se repite. Las velocidades escalan dibujo y espera juntos.
- Círculos y guías se pueden ocultar para ver solo el trazo. Cuatro paletas (Tinta, Turquesa, Dorado y Rosa), con variantes legibles para fondos claro/oscuro.
- Vista ampliada con zoom 1–4x y desplazamiento mediante pellizco/arrastre; Restablecer encuadre vuelve al centro. El zoom del panel normal está disponible mediante control; para desplazar se amplía la vista. Se conserva el encuadre al entrar/salir y recrear la actividad.
- Los cambios visuales reutilizan los mismos contornos y coeficientes de 0.8.0. Los rastros y sus Paths completos se preparan en un worker al cambiar dibujo o N; cada fotograma solo construye el Path parcial activo. Los trazos completos usan Paths precalculados. Se conserva DrawingTimeline y no se añaden puentes entre trazos.

## Reloj y recuperación

PlaybackClock es una función pura del progreso anterior, delta, velocidad y estado de pausa. La pantalla toma deltas del reloj de fotogramas y funciona solo en estado RESUMED, con lienzo visible y resultado disponible. Al abrir cámara/editor, ocultar el lienzo o pasar a segundo plano se detiene; al volver usa un nuevo origen temporal para no sumar el tiempo ausente. La vista ampliada sustituye al panel: nunca hay dos relojes avanzando el mismo trabajo.

El progreso en memoria se conserva con ViewModel al recrear la actividad. Velocidad/paleta/fondo/epiciclos se guardan también como preferencias locales, incluso en las demostraciones. Con una foto de trabajo se guardan además progreso, pausa y encuadre dentro de sus metadatos. Se guarda al cambiar controles (con debounce) y se solicita un checkpoint al pausar la actividad o cerrar la vista ampliada. No se escribe a disco por fotograma. Tras terminar el proceso se recupera el último checkpoint completado; no se promete capturar un cierre forzado instantáneo.

Nueva foto, preparación aplicada o contorno reprocesado reinician la posición del dibujo; los estilos se conservan. Nuevo encuadre fotográfico/foto/demostración restablece el zoom de reproducción. No hay exportadores, formatos de salida ni nuevos permisos/dependencias.

## Verificación

JVM: velocidades, pausa exacta, cambio de velocidad, independencia de fraccionamiento temporal, espera/repetición, límites, zoom/ancla/desplazamiento y contraste de las paletas. Se mantienen las 71 pruebas anteriores.

Android 10/15: controles, avance, estilos, zoom/desplazamiento, vista ampliada y recreación sin reprocesamiento; reloj real de Compose con avance controlado, pausa, segundo plano y reanudación sin salto; JSON y recuperación desde una nueva instancia de ViewModel con foto privada. Se conservan pruebas de cámara, editor y motor. Produce capturas clara/oscura. Consultar Actions para el resultado efectivo.

Pendiente de prueba física de 0.9.0 y fluidez en dispositivos del propietario. La matriz de dos teléfonos #6 y reconocibilidad formal #3 siguen pendientes. Estos controles no mejoran por sí mismos la detección automática.

Referencias: https://developer.android.com/develop/ui/compose/animation/value-based y https://developer.android.com/guide/components/activities/activity-lifecycle .
