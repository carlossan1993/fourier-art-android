# Etapa 9 — Preparación Google Play

Autorizada por Carlos el 21/10/2026 sobre 0.12.0 aceptada. Seguimiento: [#22](https://github.com/carlossan1993/fourier-art-android/issues/22). Propuesta de prueba: 0.13.0, versionCode 16. La Etapa 8 permanece cerrada con sus excepciones y #3 continúa aplazado.

## Entregables

- AAB release reproducible; firma de subida por variables privadas y fallo explícito si se exige firma sin credenciales. [Custodia y workflow manual](../store/SIGNING.md).
- Icono adaptativo, heredado y monocromático, más PNG 512×512 y gráfico 1024×500 ES/EN. Son dibujos vectoriales propios: una curva definida por tres vectores rotatorios. Fuente y generador en git.
- Privacidad completa ES/EN dentro de la app, disponible sin conexión. `store/privacy.json` genera también HTML estático en `docs/site/`, sin rastreadores ni dependencias remotas. La política actual ofrece el canal público de incidencias. Carlos confirmó `csdigital.apps@gmail.com` como correo público de soporte de la ficha el 21/10/2026; queda registrado en `store/contact.json`.
- [Textos de ficha ES](../store/listing-es.json) y [EN](../store/listing-en.json), dentro de los límites 30/80/4000 caracteres. No prometen fidelidad universal ni funcionamiento del perfil Sujeto sin descargar su modelo.
- [Inventario de terceros](../store/THIRD_PARTY.md), con revisión final de avisos/licencias pendiente antes del envío.
- [Propuesta Data Safety](../store/DATA_SAFETY.md): distingue el procesamiento local de fotos de la telemetría técnica de ML Kit. Debe contrastarse con el formulario final; no se ha enviado.
- Auditoría del AAB real, manifest fusionado, permisos, árbol de dependencias, alineación ELF de librerías de 64 bits y configuración ZIP de 16 KB. Un smoke test instala un APK derivado del AAB release con certificado temporal de pruebas; no se distribuye como APK de publicación.
- Capturas reales ES/EN mediante prueba instrumentada: Inicio, animación, controles, exportación y privacidad. No son maquetas ni imágenes generadas. Ejecutadas y revisadas visualmente; los PNG RGB completos están en la entrega y las vistas reducidas/procedencia en [capturas](../store/SCREENSHOTS.md).

## Acciones del propietario pendientes

1. Nombre aprobado por Carlos el 21/10/2026: **Fourier Art**. Frase descriptiva: «Tus fotos, dibujadas con matemáticas». Correo público de soporte confirmado: `csdigital.apps@gmail.com`. Identificador técnico actual: `com.fourierart.app`, que se verificará al crear la ficha. La aprobación del nombre no acredita una nueva prueba del APK 0.13.0.
2. Descargar y custodiar el respaldo privado de la clave de subida RSA 3072 y sus credenciales, generado y conservado por separado. No se incorpora al repositorio. Configurar los secretos de Actions es opcional para automatizar futuras compilaciones firmadas.
3. Publicar `docs/site/` en una URL HTTPS estable. El HTML está preparado; no se declara activa una URL que aún no se ha comprobado. Se puede usar GitHub Pages sin coste: en Settings → Pages seleccionar GitHub Actions y ejecutar `Publish privacy policy` desde main una vez integrado el PR. El workflow publica solo `docs/site/`. Después comprobar ambas URL públicas sin iniciar sesión.
4. Completar la configuración de cuenta, países, público objetivo y cuestionario de contenido en Play Console con las respuestas reales. No se asigna una clasificación de edad por suposición.
5. Verificar el canal de pruebas requerido. Para cuentas personales creadas después del 13/11/2023, la guía vigente exige al menos 12 testers inscritos continuamente durante 14 días para solicitar acceso a producción. No se presupone aquí el tipo o fecha de la cuenta ni que las pruebas internas de otra app lo sustituyan.

## Estado y salida

Preparación técnica verificada en el [release CI 39552473338](https://github.com/carlossan1993/fourier-art-android/actions/runs/39552473338), commit `7ff78118cdcfe88421ebc6865e592b7fddda1f62`: bundletool valida el AAB, lint release sin errores (51 advertencias conservadas), ocho librerías de 64 bits alineadas estáticamente a 16 KB y configuración ZIP 16 KB, firma exigida rechazada sin credenciales y comprobada con credenciales de prueba, y prueba de interfaz release ES/EN correcta. No se ha probado un dispositivo en modo de páginas de 16 KB ni se acredita aceptación por Play Console.

La [suite Android del mismo commit](https://github.com/carlossan1993/fourier-art-android/actions/runs/39552473328) terminó correctamente: 89 JVM; instrumentación informa 31 casos API29 (dos recolectores del banco omitidos sin assets), 24 API35 y siete API24, más doce exportaciones verificadas. [Resumen](evidence/stage9/android-validation.json).

El AAB entregado se firmó después con la clave de subida privada: firma y huella verificadas, conservando idénticos todos los contenidos del AAB validado. [Hashes y firma](evidence/stage9/signed-bundle.json), [auditoría](evidence/stage9/audit.json), [lint](evidence/stage9/lint-summary.json). Las credenciales privadas no están en estas evidencias.

La etapa sigue abierta hasta la aceptación del propietario y los pendientes de URL pública, ficha/consola y revisión final de avisos de terceros. El AAB automático de CI es de validación sin firma; el ZIP de entrega separado contiene el AAB firmado. El envío a producción, revisión y comprobación de descarga corresponden a la Etapa 10.

## Fuentes oficiales consultadas — 21/10/2026

- Firma: https://developer.android.com/studio/publish/app-signing
- Target API 36 para nuevas apps/actualizaciones desde 31/08/2026: https://support.google.com/googleplay/android-developer/answer/11926878
- Páginas de memoria de 16 KB: https://developer.android.com/guide/practices/page-sizes
- Gráficos de ficha: https://support.google.com/googleplay/android-developer/answer/9866151
- Privacidad: https://support.google.com/googleplay/android-developer/answer/10144311
- ML Kit: https://developers.google.com/ml-kit/android-data-disclosure y https://developers.google.com/ml-kit/terms
- Pruebas según cuenta: https://support.google.com/googleplay/android-developer/answer/14151465
