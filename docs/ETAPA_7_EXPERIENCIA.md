# Etapa 7 · Experiencia final V1 (0.11.0)

Carlos eligió continuar aquí en Work el 20/10/2026. Confirmó que 0.10.0 funcionó bien en teléfono y autorizó integrar el PR #16; quedó integrado en main mediante `2526d84d7446449bbfc17e4333e33fc222685413`. El PR #18 ahora apunta a main. Seguimiento #17. La versión 0.11.0 está preparada para su propia prueba física.

## Recorrido

- **Inicio:** identidad FOURIER ART con ilustración matemática nativa, cámara/galería, trabajo actual, demostración, guía y ajustes. No se pide permiso de cámara hasta usarla.
- **Preparación (1/4):** foto, giro y recorte, Siguiente fijo. Volver al inicio conserva el borrador para retomarlo.
- **Contornos (2/4):** foto con trazos superpuestos, estado comprensible, corrección manual, perfil y refinamiento, reintento, edición/cambio de foto. Ver animación se habilita solo con trazos válidos. Un resultado vacío ofrece corregirlo.
- **Animación (3/4):** lienzo, avance, pausa, reinicio, resultado completo, ampliación y acceso a parámetros. Exportar permanece fijo.
- **Parámetros y estilo:** vista previa completa, componentes, velocidad, color, fondo, epiciclos y encuadre. Cambiar aquí no vuelve a detectar la foto.
- **Exportación (4/4):** conserva los codificadores 0.10.0. Grupos de formato, encuadre, duración/resolución y opciones seleccionadas visibles, mensajes de estado, progreso y cancelación fijos.
- **Guía:** recorrido completo, consejos de fotografía y explicación de la descarga inicial del modelo Sujeto.
- **Ajustes:** español/inglés/idioma del dispositivo, privacidad y almacenamiento, gestión de la foto de trabajo y versión. Cambiar a una demostración o descartar la foto pide confirmar su eliminación.

Se conserva un trabajo local, sin galería de proyectos ni nuevas dependencias. Las muestras siguen siendo figuras matemáticas; la ilustración de inicio no simula una fotografía procesada. La detección y los formatos no se modifican.

## Navegación y accesibilidad

Navegación nativa Compose con estado guardable, flecha Volver y botón Atrás de Android, incluyendo retorno de guía, parámetros, corrección, cámara y exportación. La identidad del trabajo impide que recrear una Activity devuelva al usuario a otra pantalla. El reloj no avanza al retirar el reproductor.

Controles de al menos 48 dp, opciones seleccionadas con estado semántico y contraste, etiquetas para controles sin texto propio, encabezados y mensajes de estado anunciables. Contenido desplazable y acciones principales fijas; controles del editor ajustados a la altura disponible. Validación prevista en letra al 150%, orientación horizontal y ambas traducciones. Esto no constituye una auditoría formal de accesibilidad.

## Validación

[CI correcta y evidencias](https://github.com/carlossan1993/fourier-art-android/actions/runs/39494959745), código `26444117a0548c49e1d9ce7e7d1e4ec94d09b327`:

- Compilación de app y APK de pruebas, y suite JVM correctas.
- **24 pruebas en Android 10/API 29 y 19 en Android 15/API 35 correctas.**
- Guía, ajustes ES/EN, navegación conservada al recrear la actividad, borrador reanudable, foto → animación → PNG conservando el dibujo y resultado vacío corregible.
- Letra al 150% verificada en la densidad de un TextLayoutResult real, con navegación, parámetros y acción de exportación accesibles.
- Orientación horizontal real y recreación conservando la sesión de exportación, comprobadas en una prueba independiente de la configuración de letra. No se afirma que se haya probado la combinación de ambas condiciones simultáneamente.
- Regresiones de cámara, permisos, cancelación, preparación, editor de contornos, Fourier, reproductor, PNG/GIF/MP4, guardado nativo y compartir.
- Capturas y métricas generadas en el artefacto de evidencias; queda la revisión visual y aceptación de Carlos en teléfono.

La prueba anterior que combinaba reemplazo de contenido para la escala de letra, giro real y Atrás del sistema falló en API 35; otro intento quedó bloqueado. Se separaron las dos condiciones, se restauró el contenido al terminar la prueba de letra y se añadieron límite de 480 segundos y registros de diagnóstico a cada batería. No se modificaron el motor ni los codificadores para resolver estos problemas de validación.

[APK 0.11.0](https://github.com/carlossan1993/fourier-art-android/actions/runs/39494959745/artifacts/12298245955): descargar el ZIP y extraer `app-debug.apk`. Disponible hasta 27/10/2026 según Actions. La actualización documental posterior no cambia el código de ese APK.

La prueba física de **0.11.0** está pendiente; la aceptación de **0.10.0** queda registrada por separado. El banco de reconocibilidad #3 y la matriz de dos teléfonos #6 siguen pendientes. La Etapa 8 posterior cubre pruebas y estabilización de forma más amplia.
