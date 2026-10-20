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

## Etapa 2 — Captura e importación (Chat)
**Siguiente etapa; todavía sin implementar ni validar por completo.** Objetivo: [issue #6](https://github.com/carlossan1993/fourier-art-android/issues/6).

Photo Picker y orientación EXIF ya existen. Faltan cámara, permiso al usarla, rotación manual, recorte y robustez del flujo/estado. Se conserva el motor 0.5.0. Salida según el plan: captura/selección sin fallos y preparación correcta, probadas en dos dispositivos, además de APK y pruebas relevantes.

El punto de continuación está en [CONTINUAR_EN_CHAT_ETAPA_2.md](CONTINUAR_EN_CHAT_ETAPA_2.md).

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
