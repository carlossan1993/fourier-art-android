# FOURIER ART — Seguimiento de etapas

Documento de referencia: **FOURIER_ART_Documento_Oficial_y_Hoja_de_Ruta_v1.pdf**, aprobado el 14/10/2026.

## Etapa 0 — Fundación y repositorio
- [x] Repositorio GitHub público creado por el propietario.
- [x] Rama de desarrollo, esqueleto Android con Kotlin y Compose.
- [x] Interfaz con recursos inglés/español y selector de idioma.
- [x] GitHub Actions configurado para generar un APK de depuración.
- [x] Compilación confirmada en GitHub Actions.
- [x] APK instalado y verificado en un teléfono Android.
- [x] PR revisado e integrado a main.

**Alcance del selector:** inglés/español en toda pantalla V1. Preferencia conservada entre sesiones; no se utilizarán traducciones de API.

## Etapa 1 — Prueba de concepto crítica (Work)
- [x] Figuras conocidas y motor multitrazo funcionales.
- [x] Extracción de fotografías, silueta, detalles y rasgos de varias caras.
- [x] Base 0.4.0 aceptada e integrada mediante PR #2.
- [x] 0.5.0: Estructura/Textura, 40 pruebas JVM y 5 instrumentadas Android superadas.
- [x] El 19/10/2026 el propietario eligió conservar 0.5.0 y avanzar; PR #5 integrado.

**Decisión G1 de continuidad:** avanzar a Etapa 2 con la versión elegida y limitaciones conocidas. El banco formal de 30 imágenes (meta >=24 reconocibles) **no se ha completado ni se declara superado**; sigue abierto en issue #3 para evaluación/regresión. No se equipara la aceptación de una base con calidad universal. Mejoras adicionales de fidelidad aparcadas por ahora. Ver [motor 0.5.0](MOTOR_0.5.0.md).

## Etapa 2 — Captura e importación (Work por elección del propietario)
El 19/10/2026 el propietario pidió continuar esta etapa en Work por su disponibilidad de consumo. Objetivo: [issue #6](https://github.com/carlossan1993/fourier-art-android/issues/6).

0.6.0 añade CameraX, permiso al usarla, editor de rotación/recorte, conservación del original y recuperación del trabajo. Photo Picker y EXIF se mantienen; `core/` conserva el motor 0.5.0. Ver [implementación y pruebas](ETAPA_2_CAPTURA.md).

**Estado: 0.6.0 probada y aceptada por el propietario el 19/10/2026; PR #8 integrado.** Actions: 44 pruebas JVM, nueve API 29 y cuatro API 35 correctas. El 20/10/2026 Carlos omitió expresamente la matriz física de dos equipos porque no dispone de ellos; #6 cerrado como `not_planned`. No se acredita una prueba física no realizada.

## Etapa 3 — Contornos robustos (Work)
0.7.0 incorpora vista previa editable, Silueta/Detalles, limpieza, conservación de una zona, borrador de segmentos, dibujo correctivo y deshacer/rehacer. La detección aceptada sigue siendo el valor predeterminado. Seguimiento: #9 y [alcance](ETAPA_3_CONTORNOS.md).

**Estado: 0.7.0 probada y aceptada por el propietario el 20/10/2026; PR #10 integrado.** No se declara superado el banco formal de reconocibilidad >=24/30 de #3. La edición permite corregir trazos; no acredita por sí sola una mejora de detección automática.

## Etapa 4 — Motor Fourier definitivo (Work por elección del propietario)
El 20/10/2026 el propietario pidió continuar aquí en Work por disponibilidad de consumo. 0.8.0 consolida FFT, selección de componentes, simetría de trazos abiertos, error espectral, densidad de render y secuencia multitrazo determinista. Seguimiento: #11 y [alcance y validación](ETAPA_4_FOURIER.md).

**Estado: 0.8.0 probada y aceptada por el propietario el 20/10/2026; PR #12 integrado.** Se conserva la detección y la corrección aceptadas; la precisión matemática no acredita reconocibilidad fotográfica.

## Etapa 5 — Animación y controles (Work)
El propietario pidió continuar aquí. 0.9.0 añade reproducción, pausa, reinicio, avance manual, cuatro velocidades, epiciclos opcionales, colores/fondo, vista ampliada y zoom/encuadre. Reloj detenido al salir de la app o de la vista. Seguimiento: #13 y [alcance](ETAPA_5_ANIMACION.md).

**Estado: 0.9.0 probada y aceptada por el propietario el 20/10/2026; PR #14 integrado.** 83 pruebas JVM, 17 API 29 y 12 API 35 correctas; los emuladores no acreditan rendimiento físico universal.

## Etapa 6 — Exportación local (Work)
0.10.0 añade PNG, GIF y MP4, vista previa exacta de encuadre 1:1/9:16, duración, progreso/cancelación y guardado/compartir nativos. Seguimiento: #15 y [alcance](ETAPA_6_EXPORTACION.md).

**Estado: 0.10.0 probada y aceptada por Carlos el 20/10/2026; PR #16 integrado en main.** CI: 86 JVM, 20 API 29 y 15 API 35 correctas. Archivos PNG/GIF/MP4 comprobados también con Pillow/FFmpeg.

## Etapa 7 — Interfaz y experiencia final (Work por elección del propietario)
El 20/10/2026 Carlos pidió continuar aquí. 0.11.0 organiza el recorrido por pantallas, guía y ajustes, estados, recuperación y accesibilidad básica. [Alcance](ETAPA_7_EXPERIENCIA.md), seguimiento #17. Base 0.10.0 aceptada y PR #16 integrado en main. El PR #18 apunta a main.

**Estado: Carlos aprobó el APK 0.11.0 el 20/10/2026 («la apk se ve bien»); integración mediante PR #18.** La compilación y suite JVM pasaron, con 24 pruebas en Android 10 y 19 en Android 15. Esta aceptación conserva las pendientes formales #3 y #6.

## Etapa 8 — Pruebas y estabilización (Work)
Trabajo autorizado por Carlos sobre la base 0.11.0 aceptada. 0.12.0 aprobada por Carlos el 20/10/2026 e integrada mediante PR #20: reutilización acotada de puntos Fourier, liberación de temporales y sesión de exportación, cancelación y mediciones de estabilidad en emuladores. Seguimiento #19 y [alcance](ETAPA_8_ESTABILIDAD.md). La validación técnica pasó: 89 JVM, 28 API 29, 23 API 35 y siete API 24, además de doce archivos verificados externamente. La matriz física de dos dispositivos #6 fue omitida por instrucción expresa de Carlos. La [evaluación de #3](../quality-bank/INFORME.md) revisó las 30 imágenes en Dibujo: 21 reconocibles sin edición. Los perfiles primarios siguen con medición incompleta por el modelo en emulador; no se acredita la meta formal >=24/30.

**Cierre:** Carlos dio por cerrada la Etapa 8 el 21/10/2026: «bueno, algo es algo, lo mejoramos luego. Damos esta etapa por cerrado». Se cierra #19 por aceptación del propietario con excepciones: #3 queda abierto para mejoras posteriores, la prueba física #6 fue omitida y la combinación de letra al 150% con rotación queda sin verificar. El cierre no convierte esos criterios en pruebas superadas ni acredita >=24/30.

## Etapa 9 — Preparación Google Play (Work)
Siguiente etapa de la hoja de ruta, pendiente de inicio. Preparar el AAB de publicación firmado y la custodia privada de la clave de subida; revisar permisos, SDK y licencias; preparar política de privacidad pública, declaración Data Safety y ficha de tienda con icono, capturas y textos. Comprobar los requisitos de pruebas aplicables a la cuenta en Play Console. La salida prevista es un paquete de publicación preparado y los requisitos bloqueantes resueltos. El envío a producción y la verificación de descarga corresponden a la Etapa 10.

## Distribución de herramientas aprobada

| Etapa | Trabajo | Herramienta |
| --- | --- | --- |
| 0 | Fundación y repositorio | Chat |
| 1 | Prueba de concepto | Work |
| 2 | Captura e importación | Chat |
| 3 | Detección y corrección avanzada de contornos | Work |
| 4 | Motor Fourier definitivo | Chat |
| 5 | Animaciones, epiciclos y controles | Chat |
| 6 | GIF, MP4 y PNG | Work |
| 7 | Interfaz y experiencia final | Chat |
| 8 | Pruebas y estabilización | Work |
| 9 | Preparación Google Play | Work |
| 10 | Publicación y verificación | Chat |

## Regla de salida
Ninguna etapa termina solamente porque el código compila. Se requiere evidencia de la funcionalidad prevista.
