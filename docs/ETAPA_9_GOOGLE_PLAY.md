# Etapa 9 — Preparación Google Play

Autorizada por Carlos el 21/10/2026 sobre 0.12.0 aceptada. Seguimiento: [#22](https://github.com/carlossan1993/fourier-art-android/issues/22). Propuesta de prueba: 0.13.0, versionCode 16. La Etapa 8 permanece cerrada con sus excepciones y #3 continúa aplazado.

## Entregables

- AAB release reproducible; firma de subida por variables privadas y fallo explícito si se exige firma sin credenciales. [Custodia y workflow manual](../store/SIGNING.md).
- Icono adaptativo, heredado y monocromático, más PNG 512×512 y gráfico 1024×500 ES/EN. Son dibujos vectoriales propios: una curva definida por tres vectores rotatorios. Fuente y generador en git.
- Privacidad completa ES/EN dentro de la app, disponible sin conexión. `store/privacy.json` genera también HTML estático en `docs/site/`, sin rastreadores ni dependencias remotas. El contacto previsto es el canal público de incidencias; falta el correo de soporte requerido por la ficha de Play.
- [Textos de ficha ES](../store/listing-es.json) y [EN](../store/listing-en.json), dentro de los límites 30/80/4000 caracteres. No prometen fidelidad universal ni funcionamiento del perfil Sujeto sin descargar su modelo.
- [Inventario de terceros](../store/THIRD_PARTY.md), con revisión final de avisos/licencias pendiente antes del envío.
- [Propuesta Data Safety](../store/DATA_SAFETY.md): distingue el procesamiento local de fotos de la telemetría técnica de ML Kit. Debe contrastarse con el formulario final; no se ha enviado.
- Auditoría del AAB real, manifest fusionado, permisos, árbol de dependencias, alineación ELF de librerías de 64 bits y configuración ZIP de 16 KB. Un smoke test instala un APK derivado del AAB release con certificado temporal de pruebas; no se distribuye como APK de publicación.
- Capturas reales ES/EN mediante prueba instrumentada: Inicio, animación, controles, exportación y privacidad. No son maquetas ni imágenes generadas. Pendiente de ejecución CI y revisión visual.

## Acciones del propietario pendientes

1. Confirmar nombre e identificador antes de crear/enviar la ficha: `FOURIER ART` / `com.fourierart.app`; proporcionar correo público de soporte.
2. Custodiar clave de subida y configurar sus secretos. No se genera una clave definitiva en un almacenamiento temporal ni se incorpora al repositorio.
3. Publicar `docs/site/` en una URL HTTPS estable. El HTML está preparado; no se declara activa una URL que aún no se ha comprobado. Se puede usar GitHub Pages sin coste: en Settings → Pages seleccionar GitHub Actions y ejecutar `Publish privacy policy` desde main una vez integrado el PR. El workflow publica solo `docs/site/`. Después comprobar ambas URL públicas sin iniciar sesión.
4. Completar la configuración de cuenta, países, público objetivo y cuestionario de contenido en Play Console con las respuestas reales. No se asigna una clasificación de edad por suposición.
5. Verificar el canal de pruebas requerido. Para cuentas personales creadas después del 13/11/2023, la guía vigente exige al menos 12 testers inscritos continuamente durante 14 días para solicitar acceso a producción. No se presupone aquí el tipo o fecha de la cuenta ni que las pruebas internas de otra app lo sustituyan.

## Estado y salida

Preparación en curso; no se cierra la etapa hasta verificar la evidencia y resolver los requisitos de firma, URL pública y consola. El AAB de validación sin firma no se debe subir a Play. El envío a producción, revisión y comprobación de descarga corresponden a la Etapa 10.

## Fuentes oficiales consultadas — 21/10/2026

- Firma: https://developer.android.com/studio/publish/app-signing
- Target API 36 para nuevas apps/actualizaciones desde 31/08/2026: https://support.google.com/googleplay/android-developer/answer/11926878
- Páginas de memoria de 16 KB: https://developer.android.com/guide/practices/page-sizes
- Gráficos de ficha: https://support.google.com/googleplay/android-developer/answer/9866151
- Privacidad: https://support.google.com/googleplay/android-developer/answer/10144311
- ML Kit: https://developers.google.com/ml-kit/android-data-disclosure y https://developers.google.com/ml-kit/terms
- Pruebas según cuenta: https://support.google.com/googleplay/android-developer/answer/14151465
