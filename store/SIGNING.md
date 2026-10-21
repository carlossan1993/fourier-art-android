# Firma de publicación y custodia

La validación automática produce un AAB **sin firmar**, que no sirve para subir a Play. La APK de prueba habitual usa firma debug. El smoke test release usa el certificado debug solo dentro del runner; ese APK no se distribuye como publicación.

Para el AAB definitivo se necesitan la clave de subida privada y cuatro variables:

| Variable | Contenido |
| --- | --- |
| `FOURIER_UPLOAD_STORE_FILE` | Ruta absoluta al keystore privado |
| `FOURIER_UPLOAD_STORE_PASSWORD` | Contraseña del almacén |
| `FOURIER_UPLOAD_KEY_ALIAS` | Alias de la clave de subida |
| `FOURIER_UPLOAD_KEY_PASSWORD` | Contraseña de la clave |

Con esas variables ya configuradas de manera privada:

```bash
gradle :app:bundleRelease -PrequireReleaseSigning=true --no-daemon
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

Si falta alguna variable o se exige firma sin credenciales, Gradle falla. Nunca se sustituye por el certificado debug. No escribas contraseñas en comandos compartidos, incidencias o el repositorio; no subas keystores, contraseñas ni claves como artefactos de Actions.

## Flujo previsto con Play App Signing

1. Confirmar nombre e identificador antes del primer envío: actualmente `FOURIER ART` / `com.fourierart.app`. El identificador quedará vinculado a la ficha de Play.
2. Crear o utilizar una clave **de subida** bajo custodia del propietario; conservar una copia de seguridad privada y sus contraseñas. No se genera una clave definitiva en este entorno temporal.
3. Activar Play App Signing y dejar que Google custodie la clave de firma de distribución. La clave de subida permite autenticar futuros AAB; no es la misma obligación de custodia que la clave de distribución.
4. Verificar la firma, huella del certificado, versión y SHA256 del AAB final. Subirlo al canal de pruebas que corresponda. La publicación en producción es Etapa 10.

## Automatización opcional ya preparada

`Signed upload bundle` es una ejecución manual en Actions. Configurar primero los secretos de repositorio `FOURIER_UPLOAD_STORE_BASE64`, `FOURIER_UPLOAD_STORE_PASSWORD`, `FOURIER_UPLOAD_KEY_ALIAS` y `FOURIER_UPLOAD_KEY_PASSWORD`. La clave base64 también es un secreto. Solo un mantenedor debe ejecutar ese workflow sobre código revisado e integrado en main. No se ejecuta en pull requests y no publica en Play. El runner elimina el keystore temporal en un paso `always()`; el artefacto contiene solo el AAB firmado y su checksum.

Fuentes: https://developer.android.com/studio/publish/app-signing y https://developer.android.com/tools/bundletool .
