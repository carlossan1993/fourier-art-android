# Etapa 7 · Experiencia final V1 (0.11.0)

Carlos eligió continuar aquí en Work el 20/10/2026. Base: 0.10.0 del PR #16. Como no hay confirmación explícita de su prueba física, esta etapa se prepara en una rama y PR dependientes de ese PR, sin declarar aceptada físicamente la exportación. Seguimiento #17.

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

Pendiente de completar CI y revisión visual: 86 JVM de regresión, flujos existentes adaptados a la nueva navegación, pruebas nuevas de guía/idioma/recreación, recorrido foto → exportación, estado vacío, borrador reanudable y letra grande/horizontal. Ejecutar API 29 y 35, revisar capturas y decodificar externamente los PNG/GIF/MP4 de regresión. Después entregar APK para prueba del propietario.

La aceptación física de #15, el banco de reconocibilidad #3 y la matriz de dos teléfonos #6 siguen pendientes. La Etapa 8 posterior cubre pruebas y estabilización de forma más amplia.
