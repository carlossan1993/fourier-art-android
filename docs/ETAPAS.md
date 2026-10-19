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
En curso en PR #2. Círculo/estrella verificados por el usuario; fotografías complejas fallaron con el detector de color inicial. 0.3.0 incorpora rasgos faciales, máscara de sujeto y múltiples trazos. Ver [revisión del motor y validación pendiente](MOTOR_0.3.0.md). G1 continúa abierto hasta comprobar fotografías reales; no ampliar producto ni declarar GO por compilar.

## Regla de salida
Ninguna etapa termina solamente porque el código compila. Se requiere evidencia de la funcionalidad prevista.
