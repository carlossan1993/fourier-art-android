# Privacidad en Firebase Hosting

Carlos eligió Firebase Hosting el 21/10/2026 para la URL pública de privacidad
de Fourier Art. La configuración está preparada; el despliegue y su URL
permanecen pendientes hasta disponer de acceso al proyecto de Firebase.

## Contenido y destino

`firebase.json` publica exclusivamente `docs/site/`: índice, política en español
y política en inglés. Las páginas incluyen el correo aprobado
`csdigital.apps@gmail.com`, no usan JavaScript, cookies ni recursos externos.
El HTML se genera con `python tools/prepare_store_assets.py --privacy-only`,
sin instalar dependencias gráficas.

Se usa el target explícito `fourier-privacy`. Hay que asociarlo a un sitio
dedicado a Fourier Art, dentro de un proyecto nuevo o existente. Antes de
desplegar se verificará el ID real del proyecto y del sitio; no se presupone
disponible ningún nombre. No se debe asociar este target al sitio de otra app.

## Despliegue

Usar Firebase CLI desde Google Cloud Shell o un equipo con una sesión autorizada,
ejecutando desde la raíz del repositorio. Sustituir
`PROJECT_ID` y `SITE_ID` por sus valores reales comprobados:

```sh
firebase login
firebase projects:list
firebase hosting:sites:list --project PROJECT_ID
```

Si aún no existe el sitio dedicado, crearlo una sola vez:

```sh
firebase hosting:sites:create SITE_ID --project PROJECT_ID
```

Asociar el sitio y publicar solo ese target:

```sh
firebase target:apply hosting fourier-privacy SITE_ID --project PROJECT_ID
firebase deploy --only hosting:fourier-privacy --project PROJECT_ID
```

La asociación local `.firebaserc` y la caché de Firebase están excluidas de git.
No hacen falta base de datos, Authentication ni Functions para estas páginas.
No ejecutar `firebase init` sobre las páginas existentes ni un despliegue
general del proyecto.

## Comprobación y Google Play

Después del despliegue comprobar sin iniciar sesión:

- `https://SITE_ID.web.app/privacy-es.html`: HTTP 200, HTML en español.
- `https://SITE_ID.web.app/privacy-en.html`: HTTP 200, HTML en inglés.
- Navegación entre idiomas, correo de contacto y lectura en móvil.

Pegar en Play Console la URL española comprobada y guardar el formulario.
`SITE_ID` es un marcador, no una URL activa. La configuración antigua de
GitHub Pages queda como alternativa manual y no se ejecuta con este cambio.

Este alojamiento publica la política; no modifica el AAB 0.13.0, no aloja
fotografías y no configura por sí solo la sección Seguridad de los datos.

Documentación oficial: https://firebase.google.com/docs/hosting/multisites
