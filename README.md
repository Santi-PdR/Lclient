# Lclient

Lclient es un cliente modular **100% client-side** para Minecraft Forge 1.20.1.

## Diseño

Todo se controla desde una única ruleta. CopyL es un módulo más del mismo cliente, no un menú/mod separado.

La tecla de apertura de la ruleta **no se registra en Opciones > Controles**. Se cambia desde `Mods > Lclient > Config`. Las teclas internas de CopyL, Loot ESP y Recon se editan desde la propia ruleta.

Lclient protege sus teclas reservadas para evitar que un mismo botón ejecute accidentalmente dos acciones distintas.

## Lclient 2.4.0

La 2.4 profundiza la simplificación iniciada en 2.3 y mejora los cinco módulos que siguen formando el cliente.

### CopyL

- 10 slots de mensajes/comandos.
- Cada slot tiene **nombre propio**.
- Una misma tecla no puede quedar asignada a dos slots de CopyL: al reasignarla, se libera automáticamente del anterior.
- Los atajos internos de Lclient (ruleta, Loot ESP y Recon) quedan protegidos.
- Variables dinámicas disponibles dentro del texto:
  - `{pos}`
  - `{x}` / `{y}` / `{z}`
  - `{dim}`
  - `{hp}`
  - `{food}`
  - `{name}`
- Los mensajes que empiezan por `/` se envían como comando; el resto como chat normal.

### Loot ESP

- Render propio con `NO_DEPTH_TEST`; no depende del glow vanilla.
- Las cajas se dibujan **a través de paredes y terreno**.
- Marcador vertical x-ray opcional para localizar mejor objetos detrás de estructuras gruesas.
- Color según tamaño del stack.
- Alcance configurable.
- Stack mínimo configurable.
- Tecla configurable para activar/desactivar inmediatamente; por defecto: `X`.
- Sólo puede representar `ItemEntity` que el servidor ya haya enviado/cargado en el cliente.

### Smart Offhand

- Conserva la restauración segura del objeto original de la offhand.
- No actúa mientras arrastras un objeto o ya estás usando uno.
- Umbral de hambre para colocar comida configurable.
- Umbral para restaurar la offhand configurable.
- Selección de comida:
  - `AUTO`.
  - Elegir una comida detectada en los 36 slots del inventario.
  - Usar directamente la comida de la mano principal.
  - Escribir un registry ID, incluyendo comidas de mods, por ejemplo `minecraft:golden_carrot`.
- Si eliges una comida concreta puedes decidir si el comportamiento es estricto o si debe usar `AUTO` cuando esa comida no esté disponible.

### Advanced Recon

Teclas por defecto:

- Zoom: `C` (mantener pulsado).
- Waypoint: `V` (sólo mientras el zoom está activo).

Mientras el zoom está activo:

1. La rueda del mouse cambia la magnificación sin mover la hotbar.
2. El zoom se aplica únicamente al render; no sobrescribe el FOV guardado en Video Settings.
3. El HUD muestra zoom, FOV, alcance, objetivo y distancia.
4. Los bloques muestran su registry ID real.
5. Si apuntas a una entidad aparece el Target Panel con nombre, tipo, posición, distancia y vida conocida, incluyendo una barra de vida.
6. El waypoint utiliza el raycast largo real de Recon, no el reach vanilla.

### JourneyMap+

- Integración opcional con JourneyMap 1.20.1-5.10.x / API 1.9.
- El waypoint temporal de Recon usa ahora el nombre real de la entidad o bloque apuntado.
- Puede limpiarse desde la ruleta.
- Lclient sigue iniciando normalmente sin JourneyMap instalado.

## Ruleta

La ruleta muestra los cinco módulos:

- CopyL
- Loot ESP
- Smart Offhand
- Advanced Recon
- JourneyMap+

Al seleccionar un módulo muestra información útil en vivo: cantidad de mensajes configurados, tecla de Loot ESP, comida elegida, teclas/rango de Recon y estado de JourneyMap.

- Click izquierdo: abrir configuración.
- Click derecho: activar/desactivar el módulo.

## Compatibilidad

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- JourneyMap 1.20.1-5.10.x para JourneyMap+
- JourneyMap es opcional

## Build

```bash
./gradlew build
```

GitHub Actions compila con Java 17. Los pushes a `main` publican además el payload de despliegue (`lclient-latest.jar.b64`, versión y SHA-256) usado por el PowerShell de instalación.
