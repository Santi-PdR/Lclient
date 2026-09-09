# Changelog

## 3.1.0 — visual refresh + literal messages

### Eliminado

- Sistema completo de variables/placeholders.
- Lectura de coordenadas, vida, hambre, orientación y datos del objetivo para mensajes.
- `CopyLVariablesScreen` y todos los botones/textos relacionados con variables.

Los slots ahora envían exactamente el texto o comando que el usuario escribió.

### Apariencia

- Editor rediseñado con tarjetas visuales por slot.
- Numeración `01–10` y acento visual para distinguir slots configurados.
- Nombre, tecla y mensaje agrupados de forma más clara.
- Encabezado con cantidad de slots configurados y atajos asignados.
- Diseño de dos columnas en resoluciones amplias.
- Paginación responsive en resoluciones pequeñas.
- Mejor fondo, jerarquía visual y separación entre contenido/acciones.
- Config general rediseñada como panel compacto con resumen de estado.
- Mejor integración con GUI Scale alto.

### Conservado

- 10 slots de mensajes/comandos.
- Nombre por slot.
- Tecla independiente por slot.
- Tecla global para abrir el editor.
- Teclas raw, sin entradas en `Opciones > Controles`.
- Guardado transaccional.
- `Ctrl+Enter` para guardar.
- Configuración atómica y recuperación de JSON corruptos.
- Migración de la antigua tecla de apertura desde `lclient.json`.

## 3.0.0 — CopyL-only reset

El proyecto volvió a ser exclusivamente CopyL.

### Eliminado completamente

- Loot ESP.
- Smart Offhand.
- Advanced Recon.
- Recon Telemetry.
- Target/HUD táctico.
- Notification Center e historial.
- JourneyMap+ y su plugin/bridge.
- Ruleta de módulos.
- Editor de distribución HUD.
- Selector de comida.
- Configuración modular de Lclient.
- Dependencia `compileOnly` de JourneyMap.

### Identidad y build

- `modId` volvió a ser `copyl`.
- Display name: `CopyL`.
- Proyecto Gradle: `CopyL`.
- Publicación en `build-output`: `copyl-latest.jar.b64`.
