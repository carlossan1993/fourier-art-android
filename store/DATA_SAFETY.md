# Data Safety — propuesta para revisar en Play Console

Fecha de revisión: 21/10/2026. Código 0.13.0, `com.fourierart.app`. No es un formulario importable ni una declaración ya enviada. Revisar las preguntas vigentes contra el informe del AAB final y las mismas versiones de SDK.

## Respuestas base

| Campo | Propuesta y evidencia |
| --- | --- |
| ¿Recoge datos? | **Sí, por SDK**. No marcar «no recoge datos» basándose solo en el procesamiento local de fotografías. |
| Fotografías y vídeos | El procesamiento de ML Kit y el motor es local. La app no los sube a un backend. El usuario puede exportar o compartir por acción explícita; revisar las excepciones del formulario para esas transferencias solicitadas. |
| Dispositivo u otros identificadores | Declarar los identificadores de instalación y, según el SDK descargable, dispositivo documentados por ML Kit. Finalidad propuesta: análisis/diagnóstico. |
| Información y rendimiento de la app | Declarar diagnósticos y otros datos de rendimiento (latencia, errores, configuración y tamaños técnicos). Finalidad propuesta: análisis/diagnóstico. |
| Actividad en la app → Interacciones | Propuesta conservadora: declarar eventos de uso de API/modelos. Confirmar el encaje de esta categoría con la guía de datos del formulario. |
| Datos compartidos | Google dice que ML Kit no transfiere esos datos a terceros. Verificar si la transferencia al proveedor cumple la excepción de proveedor de servicios antes de marcar «no compartidos»; no confundir compartir fotos con telemetría. |
| Cifrado en tránsito | HTTPS para las métricas declaradas por Google. El manifest impide tráfico HTTP en claro. |
| Recogida opcional | No hay un interruptor propio que desactive métricas del SDK. No marcar como opcional ni atribuir una opción de exclusión que la app no implementa. |
| Procesamiento efímero | No acreditar: no se ha demostrado eliminación en memoria inmediata del proveedor. |
| Cuentas | La app no crea cuentas. No hay flujo de eliminación de cuenta aplicable. |
| Solicitud de eliminación de datos del SDK | No acreditar una función que no existe. La app puede borrar el trabajo local, pero no controla la conservación de las métricas por Google. |

No hay código de anuncios, pagos, chat, ubicación, contactos, micrófono ni servidor de fotos. El permiso de cámara se pide al usarla; el selector concede acceso a la imagen elegida. El inventario definitivo de permisos es `audit.json`, obtenido del manifest fusionado release, no solo del manifest fuente.

## Dependencias revisadas

- `com.google.mlkit:face-detection:16.1.7` (modelo incluido).
- `com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1` (modelo descargable).
- CameraX 1.5.3, AndroidX/Compose. No se añade analítica propia. El árbol resuelto completo se guarda en la evidencia CI.
- La página oficial de versiones consultada mantiene ambas versiones de ML Kit como últimas para estas funciones. No se cambian para esta etapa ni se presupone que una versión mayor mejore el reconocimiento.

Fuentes oficiales consultadas el 21/10/2026:
- https://developers.google.com/ml-kit/android-data-disclosure
- https://developers.google.com/ml-kit/terms
- https://developers.google.com/ml-kit/release-notes
- https://support.google.com/googleplay/android-developer/answer/10787469

La política se genera desde `store/privacy.json` para la app y `docs/site/`; describe la conservación local observada y distingue la política de Google. No se publica una afirmación de «cero datos».
