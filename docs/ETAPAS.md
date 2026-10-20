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

## Etapa 1 — Prueba de concepto crítica
0.4.0 aceptada por el propietario en su teléfono e integrada mediante PR #2 el 19/10/2026. Esta aceptación establece la base de trabajo; el banco formal de 30 imágenes (meta >=24 reconocibles) continúa pendiente en #3.

0.5.0 incorpora un trazado Estructura con límites de color y reducción de textura; conserva Textura como ruta de extracción de 0.4.0. Está en validación, sin extender la aceptación anterior a la nueva ruta. Ver [motor 0.5.0, comparaciones y limitaciones](MOTOR_0.5.0.md).

## Regla de salida
Ninguna etapa termina solamente porque el código compila. Se requiere evidencia de la funcionalidad prevista.
