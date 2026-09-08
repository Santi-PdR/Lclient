# Changelog

## 3.0.0 — CopyL-only reset

El proyecto vuelve a ser exclusivamente CopyL.

### Conservado

- 10 slots de mensajes/comandos.
- Nombre por slot.
- Tecla independiente por slot.
- Tecla global para abrir el editor.
- Teclas raw, sin entradas en `Opciones > Controles`.
- Editor transaccional y responsive.
- `Ctrl+Enter` para guardar.
- Configuración atómica y recuperación de JSON corruptos.
- Variables del jugador.
- Variables del objetivo bajo la mira vanilla.
- Migración de la antigua tecla de apertura desde `lclient.json`.

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

- `modId` vuelve a ser `copyl`.
- Display name: `CopyL`.
- Proyecto Gradle: `CopyL`.
- JAR: `copyl-3.0.0.jar`.
- Artifact de CI: `CopyL-3.0.0`.
- Publicación en `build-output`: `copyl-latest.jar.b64`.
- El workflow elimina el payload heredado `lclient-latest.jar.b64` al publicar 3.x en `main`.

### Migración

- `config/lclient.json` se usa una sola vez como fuente de migración cuando no existe `config/copyl.json`.
- La antigua `wheelKey` se convierte en `openKey`.
- Tras una migración correcta se elimina `lclient.json`.
- `copyl-messages.json` se conserva para no perder los mensajes ni sus teclas.
