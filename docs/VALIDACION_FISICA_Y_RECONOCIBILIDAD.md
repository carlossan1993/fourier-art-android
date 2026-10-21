# Validación física y reconocibilidad

La aceptación de una APK permite continuar con esa base. Se registran por separado la decisión sobre #6 y la evaluación visual de #3; los resultados del asistente no se atribuyen al propietario.

## Dos teléfonos (#6)
> Decisión de Carlos, 20/10/2026: «omitiré lo de los dos teléfonos, no los tengo».

Requisito omitido por instrucción expresa del propietario. #6 cerrado como `not_planned`; deja de bloquear la continuidad. Se conserva la aceptación funcional del APK y la evidencia de emuladores. **No se afirma que se hayan probado dos teléfonos físicos.**

## Banco de 30 imágenes (#3): rúbrica previa a la medición
Usar diez rostros/grupos, diez mascotas y diez objetos/logotipos/dibujos, con casos favorables y difíciles. Debe existir permiso o licencia. Mantener las fotos privadas fuera del repositorio; usar IDs y referencia privada en el registro.

Fijar antes de medir: APK, perfil Auto para personas, Sujeto para mascotas/objetos fotografiados y Dibujo para ilustraciones; Estructura, Medio, sensibilidad 50 y máximo 400 componentes. Registrar cualquier desviación y conservar el primer resultado. Evaluar el dibujo completo junto a su original: se debe identificar el sujeto y sus rasgos principales visibles, no solo que hay líneas.

- **2 — reconocible automático:** conserva forma y rasgos que distinguen el sujeto sin corrección manual.
- **1 — reconocible con corrección simple:** cumple lo anterior después de como máximo tres acciones correctivas y 60 segundos de edición. Cada gesto de dibujo/borrador, selección de zona o cambio de limpieza cuenta como una acción; no se reconstruye manualmente todo el sujeto.
- **0 — insuficiente:** sujeto o rasgos principales ambiguos, resultado vacío, o requiere superar ese límite de corrección.

Para caras, comprobar ojos, nariz, boca y disposición relativa cuando sean visibles. Para mascotas, revisar ojos, hocico/nariz y boca cuando aparezcan. Para objetos, forma y partes que permitan distinguirlos. Un rostro parcialmente oculto se evalúa sobre lo visible; anotar la limitación. Registrar evaluador, puntuación, acciones, tiempo y motivo. La valoración sigue siendo visual y no acredita fidelidad universal.

La meta conservada es **al menos 24 de 30 con puntuación 1 o 2**. No asignar puntos por compilar, por métricas geométricas o por aprobación general de una versión. El CSV tiene 30 filas vacías preparadas; no representa pruebas realizadas.
