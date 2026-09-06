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

Lclient protege teclas reservadas y sincroniza el estado físico de las teclas para evitar dobles acciones o activaciones fantasma al cerrar menús/reasignar controles.

## Lclient 2.6.0

2.6 es una pasada completa de hardening, compatibilidad con packs modded, rendimiento y UI responsive.

### JourneyMap+

Integración opcional preparada para JourneyMap 1.20.1-5.10.x / API 1.9:

- Sigue `MAPPING_STARTED`, `MAPPING_STOPPED` y `DISPLAY_UPDATE`.
- No conserva objetos `Waypoint` de una sesión anterior.
- Recon usa un display ID estable (`lclient_recon`).
- El waypoint es transitorio y se reconstruye cuando JourneyMap pide refrescar displays.
- El estado de mapping/permiso se consulta de forma limitada, no cada frame.
- El bridge opcional cachea reflexión y puede reintentar si JourneyMap todavía no terminó de iniciar.
- La ruleta muestra diagnósticos reales de la integración.
- Desactivar JourneyMap+, Recon o el waypoint de Recon limpia el marcador de Lclient.
- JourneyMap sigue siendo `compileOnly`/opcional: Lclient puede iniciar sin él.

### CopyL

- 10 slots de mensajes o comandos con nombre propio.
- Editor transaccional: **Guardar** aplica el conjunto completo; **Cancelar** no deja cambios parciales.
- En GUI compacta los 10 slots se reparten en 2 páginas de 5 para evitar solapamientos.
- `Ctrl+Enter` guarda.
- Redimensionar conserva borradores.
- Teclas duplicadas o reservadas se reparan automáticamente.
- Al reasignar una tecla, Lclient primero sincroniza su estado físico: mantenerla pulsada no dispara el mensaje al cerrar el editor.
- Abrir/cerrar inventario u otras pantallas manteniendo un atajo tampoco genera un envío fantasma.
- Variables: `{pos}`, `{x}`, `{y}`, `{z}`, `{dim}`, `{hp}`, `{food}`, `{name}`.
- El mensaje final vuelve a limitarse después de expandir variables.
- El límite respeta pares UTF-16 para no partir emojis/símbolos a la mitad.
- `copyl-messages.json` migra automáticamente arrays incompletos, teclas inválidas/duplicadas/conflictivas, controles invisibles y texto mal formado.

### Loot ESP

- Render propio `NO_DEPTH_TEST`: cajas visibles **a través de paredes y terreno**.
- Marcador vertical x-ray opcional.
- Alcance, stack mínimo y tecla configurables; `X` por defecto.
- Sólo representa `ItemEntity` que ya existen en el `ClientLevel` recibido por el cliente.
- Escaneo cacheado durante ~100 ms en lugar de recorrer el área cada frame.
- La caché se libera al desactivar el módulo o salir del mundo/servidor para no retener un `ClientLevel` viejo.
- En acumulaciones extremas (granjas, explosiones, cientos de drops), el render prioriza hasta 256 items cercanos para proteger FPS.

### Smart Offhand

- `AUTO` o una comida exacta por registry ID, incluyendo alimentos modded.
- Fallback AUTO opcional cuando falta la seleccionada.
- Umbrales de colocar/restaurar configurables.
- Usa propiedades de comida del **ItemStack real**, importante para items con NBT/capabilities.
- AUTO considera nutrición, saturación, cantidad y penaliza efectos perjudiciales.
- Una selección manual sigue teniendo prioridad: el ranking AUTO no reemplaza tu elección.
- Hooks defectuosos de `isEdible`, propiedades de comida o nombres de items modded se aíslan para no tirar Lclient.
- Antes de restaurar verifica item, tags **y cantidad exacta** del stack original.
- Si restaurar es temporalmente inseguro porque el cursor del inventario está ocupado, espera en vez de olvidar la transacción.
- Si otro mod/jugador modificó el slot original, Lclient abandona la restauración antes que mover un stack equivocado.
- Al desconectarse hace un último intento seguro y limpia todo estado transitorio.

### Advanced Recon

Teclas por defecto:

- Zoom: `C` (mantener).
- Waypoint: `V` (sólo mientras Recon está haciendo zoom).

Mientras Recon está activo:

1. La rueda cambia magnificación y no mueve la hotbar.
2. Ruedas de alta resolución/trackpads acumulan deltas fraccionarios antes de cambiar un paso de zoom.
3. Recon nunca aumenta el FOV respecto al FOV real del jugador; un preset viejo no puede convertirse en "zoom-out".
4. El zoom es render-only: no sobrescribe Video Settings.
5. Los cambios rápidos del zoom se guardan con debounce.
6. El raycast largo se cachea unos milisegundos cuando cámara/mira no cambian, reduciendo búsquedas repetidas de hasta 512 bloques.
7. Crear un waypoint **fuerza un raycast fresco**, por lo que marca donde estás mirando en ese momento.
8. Entidades con hooks modded defectuosos de colisión/pickability se ignoran de forma segura; Recon conserva el resultado de bloques/dirección.
9. Nombres, tipos y vida del Target Panel tienen fallbacks seguros para entidades custom.
10. El HUD/Target Panel sólo existen durante el zoom y se adaptan a GUI Scale alto/pantallas compactas.

## Configuración resistente a fallos

`lclient.json` y `copyl-messages.json` usan archivo temporal + reemplazo atómico cuando el sistema lo permite.

Si un JSON está corrupto:

1. Se mueve a `*.broken-<timestamp>`.
2. Se crea una configuración válida.
3. El cliente no vuelve a fallar leyendo el mismo archivo en cada inicio.

Además, si una config es legible pero contiene rangos, keycodes, conflictos o IDs inválidos, Lclient los sanea **y persiste la reparación una sola vez**.

## Ruleta e interfaces

- Click izquierdo: configurar módulo.
- Click derecho: activar/desactivar.
- Radios/zonas de selección de la ruleta se adaptan al tamaño GUI.
- CopyL usa paginación automática cuando no caben dos columnas.
- Selector de comida reduce opciones por página en ventanas bajas.
- Recon oculta el panel lateral cuando invadiría la retícula y conserva la información esencial en la línea central.
- Configuración global y pantallas de módulos ajustan anchura/altura a GUI compacta.

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
- valida que el JAR se pueda abrir y contenga `META-INF/mods.toml` y la clase principal;
- calcula SHA-256 del mismo archivo validado;
- cancela builds anteriores de la misma rama cuando aparece uno nuevo;
- impide que un build viejo de `main` sobrescriba un payload más reciente;
- publica en `build-output` `lclient-latest.jar.b64`, `version.txt`, `sha256.txt` y `source-commit.txt`.

El PowerShell de despliegue sólo necesita descargar ese payload, verificar SHA-256 y copiar el JAR a la instancia; no necesita compilar localmente.
