# FOURIER ART — Alcance inicial de Etapa 2

**Actualización 19/10/2026:** el propietario decidió continuar esta etapa en Work. La implementación 0.6.0 y su estado se siguen en `ETAPA_2_CAPTURA.md` y en issue #6. Los apartados siguientes describen el punto de partida 0.5.0, no el estado final de la captura.

## Decisión del propietario

El 19/10/2026 eligió conservar la versión 0.5.0 y avanzar según el plan. PR #5 está integrado en `main`; código del motor validado en `a8cdf1c6a2c974b66510fa9722811078c053dbc4`, merge `1daee931144429e809f57365f5f2c946cdb44bd9`.

Se cierra la prueba de concepto como decisión de continuidad del propietario. **No se declara medido el objetivo >=24/30**: el banco de 30 imágenes sigue pendiente en issue #3. El hocico de algunas mascotas y las sombras siguen siendo limitaciones conocidas. No seguir iterando ahora sobre fidelidad del motor.

## Herramienta y objetivo siguiente

La distribución aprobada asigna **Etapa 2 — captura e importación — a Chat**. Objetivo abierto: https://github.com/carlossan1993/fourier-art-android/issues/6

Leer `docs/ETAPAS.md`, `docs/MOTOR_0.5.0.md`, este archivo y el código actual de `main` antes de modificar. Trabajar en una nueva rama y PR para Etapa 2. No continuar desde una copia antigua de Etapa 0/1 ni reconstruir la app desde cero.

## Estado técnico

- Repositorio: https://github.com/carlossan1993/fourier-art-android
- App Android nativa Kotlin/Compose, paquete provisional `com.fourierart.app`.
- Versión 0.5.0, versionCode 8. Inglés/español; procesamiento local, sin backend ni APIs de pago.
- Perfiles Auto/Sujeto/Dibujo; trazados Estructura/Textura; detalle Simple/Medio/Fino; Fourier hasta 800 componentes en el control; varios trazos con levantamiento del lápiz.
- Photo Picker ya integrado. `decodePhoto` interpreta las ocho orientaciones EXIF y limita la decodificación. No hay cámara, recorte interactivo ni rotación manual.
- El estado principal de foto/análisis está en Compose `remember`; revisar su conservación al girar o volver de cámara/galería.
- Rostros: ML Kit incluido. Sujeto: modelo de Play services con descarga inicial; la falta del modelo tiene alternativa y mensaje. No cambiar este comportamiento dentro de la captura.
- Build: JDK 17, Gradle 8.13, SDK 36; AGP 8.13.2 y Kotlin 2.3.21 fijados en el repositorio. Revisar documentación oficial si se añade CameraX u otra dependencia, sin actualizar el resto indiscriminadamente.

## Archivos que leer primero

- `app/src/main/java/com/fourierart/app/FourierExperimentScreen.kt`: importación, EXIF, estado y vista previa.
- `app/src/main/java/com/fourierart/app/MainActivity.kt`: actividad y tema.
- `app/src/main/AndroidManifest.xml`: actualmente sin permiso CAMERA.
- `app/build.gradle.kts`: actualmente sin CameraX.
- `app/src/main/res/values/strings.xml` y `values-es/strings.xml`: paridad de idiomas.
- `app/src/main/java/com/fourierart/app/core/`: conservar la extracción y matemática aceptadas.

## Alcance de Etapa 2

1. Conservar selección de galería, cancelación y errores del Photo Picker, sin acceso general al almacenamiento.
2. Añadir captura local de cámara (CameraX según el plan), permiso solo al usarla y alternativa de galería si se deniega o no hay cámara.
3. Añadir preparación previa: rotación manual y recorte. Mantener EXIF, evitar aplicar orientación dos veces y asegurar que foto y contornos comparten coordenadas.
4. Conservar la imagen fuente para volver al original; invalidar/recalcular análisis cuando cambia el recorte o la rotación. Usar la misma imagen preparada para inferencia, vista previa y Fourier.
5. Manejar archivos temporales, vuelta de captura/selector, segundo plano y cambio de orientación sin perder el flujo ni bloquear la app. No guardar fotos en galería automáticamente.
6. Mantener español/inglés y las dos rutas del motor. Sin nuevas mejoras de detector, Fourier, exportación o diseño final en este PR.

## Validación de salida

Compilar APK en Actions, ejecutar pruebas relevantes de importación/preparación y mantener la batería del motor. Comprobar captura, selección, cancelación, permiso denegado, EXIF, rotación, recorte, regreso al original y recuperación del flujo. El plan requiere probar captura/selección en **dos dispositivos**; todavía no está acreditado para Etapa 2. La etapa no termina solo por compilar.

## Evidencia de la base

40 pruebas JVM y 5 instrumentadas Android API 29 superadas en:
https://github.com/carlossan1993/fourier-art-android/actions/runs/39363643585

APK de esa versión:
https://github.com/carlossan1993/fourier-art-android/actions/runs/39363643585/artifacts/12238347098

Los artefactos caducan: APK 7 días, imágenes/métricas 3 días. Si ya no están disponibles, generar un build de la revisión correcta. No publicar las fotos privadas del usuario ni meterlas en pruebas públicas.

## Punto de retorno a Work

Después de cerrar y probar Etapa 2, la **Etapa 3 — detección y corrección avanzada de contornos — corresponde a Work**. Avisar al propietario antes de iniciarla. Las etapas 4 y 5 regresan a Chat según la distribución aprobada.
