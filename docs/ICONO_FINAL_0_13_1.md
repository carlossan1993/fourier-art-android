# Icono final — 0.13.1 (17)

La versión 0.13.0 (16) seguía incluyendo la curva celeste de cuatro puntas.
La corrección usa el PNG final aprobado por Carlos, adjunto el 29/10/2026,
como fuente única en `store/graphics/icon-512.png`.

- SHA256 de la fuente: `ec0cf5c4b7cb986fd70df4e466a2f51d3e57184e528aaaeb580b2e560c6e853d`.
- Icono clásico en cinco densidades; adaptativo con margen para los recortes
  del launcher; monocromático derivado del mismo diseño para iconos temáticos.
- `python3 tools/prepare_launcher_icon.py` regenera los recursos de Android.
  `prepare_store_assets.py` también usa esta fuente y no restaura la estrella.
- Se conserva `com.fourierart.app` y la clave de subida existente.

Validación local del AAB entregado: compilación APK/AAB correcta, 89 pruebas
unitarias sin fallos, lint sin errores (48 advertencias), bundletool y firma
comprobados. Los siete PNG generados coinciden en sus píxeles visibles y transparencia con los
recursos del AAB validado. Evidencia: `evidence/icon-v0.13.1/validation.json`.
Publicación e integración en GitHub autorizadas explícitamente el 29/10/2026.

La instalación desde Google Play requiere cargar el AAB firmado 0.13.1 (17)
en la prueba cerrada y actualizar la app cuando esa versión esté disponible.
Cambiar la imagen de la ficha por sí solo no sustituye el icono instalado.
