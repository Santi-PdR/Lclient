# Lclient

Lclient es un cliente modular **100% client-side** para Minecraft Forge 1.20.1.

## Diseño

Todo se controla desde una única ruleta. CopyL es un módulo más del mismo cliente.

La tecla de apertura de la ruleta **no se registra en Opciones > Controles**. Se cambia desde `Mods > Lclient > Config`. Las teclas internas de CopyL, Loot ESP y Recon se editan desde la propia ruleta.

Lclient protege las teclas reservadas para evitar dobles acciones accidentales.

## Lclient 2.5.0

La 2.5 es una revisión de estabilidad, recuperación y rendimiento de los cinco módulos actuales.

### JourneyMap+

La integración fue reestructurada para JourneyMap 1.20.1-5.10.x / API 1.9:

- Se suscribe al ciclo real `MAPPING_STARTED` / `MAPPING_STOPPED`.
- No conserva objetos `Waypoint` pertenecientes a una sesión anterior.
- Recon usa un único display ID estable (`lclient_recon`). `IClientAPI.show()` reemplaza el marcador anterior sin necesidad de eliminar un objeto viejo primero.
- El estado de mapping se comprueba de forma limitada y segura; no se consulta la API docenas de veces por frame.
- El bridge opcional cachea sus métodos reflejados y puede reintentar el enlace si JourneyMap todavía no terminó de iniciar.
- Los errores ya no se ocultan: la ruleta muestra si JourneyMap no está mapeando, no acepta waypoints o produjo un error real.
- Desactivar JourneyMap+, desactivar Recon o desactivar el waypoint de Recon limpia los marcadores de Lclient.
- Lclient sigue pudiendo iniciar sin JourneyMap instalado.

### CopyL

- 10 slots de mensajes/comandos con nombre propio.
- Editor **transaccional**: `Guardar` aplica todos los cambios; `Cancelar` los descarta realmente, incluidas las reasignaciones de teclas.
- `Ctrl+Enter` guarda rápidamente.
- Redimensionar la ventana no pierde cambios sin guardar.
- Una misma tecla no puede quedar asignada a dos slots.
- Los atajos internos de Lclient quedan protegidos.
- Variables dinámicas: `{pos}`, `{x}`, `{y}`, `{z}`, `{dim}`, `{hp}`, `{food}`, `{name}`.
- El mensaje final se limita también después de expandir variables para evitar paquetes de chat/comando demasiado largos.

### Loot ESP

- Render propio con `NO_DEPTH_TEST`, visible **a través de paredes y terreno**.
- Marcador vertical x-ray opcional.
- Alcance, stack mínimo y tecla de toggle configurables; `X` por defecto.
- Sólo representa `ItemEntity` que el servidor ya haya enviado/cargado en el cliente.
- La búsqueda de entidades se cachea durante intervalos muy cortos en lugar de recorrer el área cada frame, reduciendo trabajo en zonas con mucho loot sin perder respuesta visual apreciable.

### Smart Offhand

- `AUTO` o selección de una comida concreta, incluyendo registry IDs de mods.
- Fallback AUTO opcional.
- Umbral de hambre para colocar/restaurar configurable.
- Restauración segura del objeto anterior de la offhand.
- Al desconectarse, Lclient intenta restaurar el intercambio gestionado antes de que desaparezca el jugador local.
- El selector de comida conserva página y feedback después de elegir una opción y prioriza visualmente la comida seleccionada.

### Advanced Recon

Teclas por defecto:

- Zoom: `C` (mantener pulsado).
- Waypoint: `V` (sólo mientras el zoom está activo).

Mientras el zoom está activo:

1. La rueda cambia la magnificación sin mover la hotbar.
2. El zoom es render-only y no sobrescribe Video Settings.
3. Los cambios rápidos de rueda se guardan con debounce, evitando una escritura de disco por cada paso del zoom.
4. El HUD muestra zoom, FOV, alcance, objetivo y distancia.
5. El Target Panel sólo aparece durante Recon.
6. El waypoint utiliza el raycast largo real y sólo informa éxito cuando JourneyMap confirma que el marcador se mostró.

## Configuración resistente a fallos

`lclient.json` y `copyl-messages.json` se escriben mediante archivo temporal + reemplazo atómico cuando el sistema lo permite.

Si al iniciar se detecta un JSON ilegible/corrupto:

1. Lclient mueve el archivo roto a `*.broken-<timestamp>`.
2. Regenera una configuración válida.
3. No queda atrapado intentando leer el mismo archivo dañado en cada inicio.

También se validan rangos numéricos, IDs y keycodes al cargar configuraciones antiguas.

## Ruleta

Módulos actuales:

- CopyL
- Loot ESP
- Smart Offhand
- Advanced Recon
- JourneyMap+

La ruleta muestra estado útil en vivo. En JourneyMap+ muestra además el estado real de la API cuando hay un problema.

- Click izquierdo: configurar.
- Click derecho: activar/desactivar.

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

GitHub Actions compila con Java 17. Los pushes a `main` publican además el payload de despliegue (`lclient-latest.jar.b64`, versión, commit origen y SHA-256) usado por el PowerShell de instalación.
