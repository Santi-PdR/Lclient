# Lclient

Lclient es un cliente modular **100% client-side** para Minecraft Forge 1.20.1.

## Diseño

Todo se controla desde una única ruleta. Los cinco módulos actuales son:

- CopyL
- Loot ESP
- Smart Offhand
- Advanced Recon
- JourneyMap+

La tecla de apertura de la ruleta **no se registra en Opciones > Controles**. Se cambia desde `Mods > Lclient > Config`. Las teclas internas de CopyL, Loot ESP y Recon se editan desde la propia ruleta.

Lclient protege teclas reservadas, sincroniza el estado físico de las teclas y mantiene las funciones informativas separadas del chat.

## Lclient 2.7.0

2.7 añade una capa de información táctica sobre la base endurecida de 2.6 sin reintroducir módulos eliminados ni duplicar funciones que ya cubren otros mods del pack.

### Notification Center

Nuevo centro de avisos no invasivo:

- tarjetas discretas en la esquina inferior derecha;
- severidades INFO, SUCCESS, WARNING y ERROR;
- deduplicación de eventos rápidos;
- duración configurable entre 2 y 10 segundos;
- entre 1 y 5 avisos simultáneos;
- se oculta mientras otra pantalla está abierta;
- historial de hasta 48 eventos durante la sesión;
- el historial no se persiste al disco;
- puede desactivarse por completo desde `Mods > Lclient > Config`.

Alertas críticas opcionales:

- vida ≤25%;
- inventario principal sin slots libres;
- item de mano con ≤10% de durabilidad;
- pieza de armadura más dañada con ≤10% de durabilidad.

Las alertas se generan al **entrar** en un estado crítico o al cambiar el objeto crítico; no se repiten en cada tick.

También recibe cambios de estado de módulos, resultados de Recon/waypoints y feedback de Smart Offhand.

### CopyL

- 10 slots de mensajes o comandos con nombre propio.
- Editor transaccional: **Guardar** aplica todo; **Cancelar** descarta el borrador completo.
- `Ctrl+Enter` guarda rápidamente.
- Redimensionar conserva cambios sin guardar.
- Teclas duplicadas/reservadas se reparan y protegen.
- No dispara mensajes fantasma al cerrar pantallas o reasignar controles.
- En ventanas angostas usa un layout ultracompacto de **dos líneas por slot**.
- La paginación se calcula dinámicamente: 2, 3 o 5 slots por página según tamaño/GUI Scale.

Variables del jugador:

- `{pos}`
- `{x}` `{y}` `{z}`
- `{dim}`
- `{hp}`
- `{food}`
- `{name}`

Variables nuevas de objetivo:

- `{target}` — nombre de entidad o ID del bloque;
- `{targetdist}` — distancia aproximada;
- `{targetpos}` — coordenadas completas;
- `{targetx}` `{targety}` `{targetz}`.

Si Advanced Recon está haciendo zoom, CopyL usa su **raycast largo** como objetivo. Fuera de Recon usa el `hitResult` vanilla. Nunca inventa entidades/bloques no recibidos por el cliente.

El texto final mantiene el límite de 256 caracteres y no corta pares UTF-16/emoji a la mitad.

### Loot ESP

- Render `NO_DEPTH_TEST`: cajas visibles a través de paredes y terreno.
- Beacon vertical x-ray opcional.
- Alcance, stack mínimo y tecla configurables; `X` por defecto.
- Sólo usa `ItemEntity` existentes en el `ClientLevel`.
- Escaneo cacheado ~100 ms en vez de recorrer el área cada frame.
- Caché ordenada por distancia y limitada a los 256 drops más cercanos en acumulaciones patológicas.
- Limpieza de caché al desactivar/salir para no retener mundos anteriores.

Nuevo HUD de loot cercano:

- **opcional** e independiente del beacon;
- agrupa drops cercanos por tipo de item;
- muestra nombre, cantidad total del grupo y distancia del drop más cercano;
- hasta 5 grupos visibles;
- reutiliza la caché de Loot ESP: no lanza otro `getEntitiesOfClass`;
- resumen actualizado a intervalos cortos, no cada frame.

### Smart Offhand

- `AUTO` o comida exacta por registry ID, incluyendo modded.
- Fallback AUTO opcional.
- Umbrales de colocar/restaurar configurables.
- AUTO considera nutrición, saturación, cantidad y penaliza efectos perjudiciales.
- Usa propiedades del `ItemStack` real.
- Antes de restaurar verifica item, tags y **cantidad exacta** del stack original.
- Si el cursor está ocupado puede diferir la restauración.
- Si otro sistema modificó el slot original, abandona antes de mover un stack incorrecto.

Nuevo feedback por Notification Center:

- comida equipada automáticamente;
- offhand restaurada;
- restauración cancelada porque el slot original cambió.

Los hooks defectuosos de alimentos/nombres modded siguen aislados para no tirar el cliente.

### Advanced Recon

Teclas por defecto:

- Zoom: `C` (mantener).
- Waypoint: `V` (durante zoom).

Recon mantiene:

1. zoom render-only que no sobrescribe Video Settings;
2. rueda de magnificación sin mover hotbar;
3. soporte de rueda fraccionaria/trackpad;
4. FOV que nunca se abre más que el FOV real;
5. guardado con debounce;
6. raycast largo cacheado brevemente;
7. raycast fresco obligatorio al crear waypoint;
8. tolerancia a hooks rotos de entidades/bloques modded;
9. HUD y Target Panel sólo durante zoom;
10. JourneyMap+ opcional para waypoint táctico.

El Target Panel ahora añade, cuando el cliente lo conoce:

- item de mano principal;
- offhand cuando la mano principal está vacía;
- número de piezas de armadura visibles (`N/4`);
- además de nombre, tipo, distancia, coordenadas y HP.

Los nombres/tipos/items/vida usan fallbacks para entidades o items modded defectuosos.

### JourneyMap+

La integración opcional sigue preparada para JourneyMap 1.20.1-5.10.x / API 1.9:

- `MAPPING_STARTED`, `MAPPING_STOPPED` y `DISPLAY_UPDATE`;
- display ID estable `lclient_recon`;
- waypoint no persistente reconstruible;
- no conserva objetos `Waypoint` entre sesiones;
- diagnósticos de estado reales;
- bridge reflejado cacheado;
- JourneyMap sigue siendo `compileOnly`/no obligatorio.

## Configuración resistente a fallos

`lclient.json` y `copyl-messages.json` usan archivo temporal + reemplazo atómico cuando el sistema lo permite.

Si un JSON está corrupto:

1. se mueve a `*.broken-<timestamp>`;
2. se crea una configuración válida;
3. Lclient no vuelve a leer el mismo archivo roto en cada inicio.

Configs legibles pero antiguas/incorrectas se sanea y migran una sola vez: rangos, keycodes, conflictos, IDs y estructuras incompletas.

## Ruleta e interfaces

- Click izquierdo: configurar módulo.
- Click derecho: activar/desactivar.
- Ruleta responsive.
- Ajustes de módulo sin el antiguo mínimo fijo de 180 px.
- CopyL tiene layout normal, compacto y ultracompacto.
- Selector de comida ajusta paginación a la altura.
- Notification History pagina según espacio disponible.
- Recon oculta el panel lateral si invadiría la retícula.
- Configuración global adapta alturas, botones y texto a GUI Scale alto.

## Compatibilidad

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- JourneyMap 1.20.1-5.10.x para JourneyMap+
- JourneyMap es opcional

## Build y publicación

```bash
./gradlew build
```

GitHub Actions:

- compila con Java 17;
- selecciona exactamente `lclient-<version>.jar`;
- valida que el JAR se abra y contenga `META-INF/mods.toml` y la clase principal;
- calcula SHA-256 del mismo archivo;
- cancela builds anteriores de la misma rama cuando aparece uno nuevo;
- evita que un build viejo de `main` sobrescriba uno más reciente;
- publica en `build-output` `lclient-latest.jar.b64`, `version.txt`, `sha256.txt` y `source-commit.txt`.

El PowerShell de despliegue sólo necesita descargar el payload publicado, verificar SHA-256 y copiar el JAR a la instancia; no necesita compilar localmente.
