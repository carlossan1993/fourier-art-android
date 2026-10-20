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

**Estado: 0.6.0 probada y aceptada por el propietario el 19/10/2026; PR #8 integrado.** Actions: 44 pruebas JVM, nueve API 29 y cuatro API 35 correctas. La confirmación no identifica dos dispositivos: la matriz física completa queda pendiente en #6. Se avanza por decisión del propietario.

## Etapa 3 — Contornos robustos (Work)
0.7.0 incorpora vista previa editable, Silueta/Detalles, limpieza, conservación de una zona, borrador de segmentos, dibujo correctivo y deshacer/rehacer. La detección aceptada sigue siendo el valor predeterminado. Seguimiento: #9 y [alcance](ETAPA_3_CONTORNOS.md).

**Estado: 0.7.0 probada y aceptada por el propietario el 20/10/2026; PR #10 integrado.** No se declara superado el banco formal de reconocibilidad >=24/30 de #3. La edición permite corregir trazos; no acredita por sí sola una mejora de detección automática.

## Etapa 4 — Motor Fourier definitivo (Work por elección del propietario)
El 20/10/2026 el propietario pidió continuar aquí en Work por disponibilidad de consumo. 0.8.0 consolida FFT, selección de componentes, simetría de trazos abiertos, error espectral, densidad de render y secuencia multitrazo determinista. Seguimiento: #11 y [alcance y validación](ETAPA_4_FOURIER.md).

**Estado: 0.8.0 probada y aceptada por el propietario el 20/10/2026; PR #12 integrado.** Se conserva la detección y la corrección aceptadas; la precisión matemática no acredita reconocibilidad fotográfica.

## Etapa 5 — Animación y controles (Work)
El propietario pidió continuar aquí. 0.9.0 añade reproducción, pausa, reinicio, avance manual, cuatro velocidades, epiciclos opcionales, colores/fondo, vista ampliada y zoom/encuadre. Reloj detenido al salir de la app o de la vista. Seguimiento: #13 y [alcance](ETAPA_5_ANIMACION.md).

**Estado: implementación en validación.** Se entrega APK para comprobar controles y fluidez en teléfono; los emuladores no acreditan rendimiento físico universal. Exportaciones corresponden a Etapa 6.

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
