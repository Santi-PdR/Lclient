# CopyL

CopyL es un mod **100% client-side** para Minecraft Forge 1.20.1 dedicado únicamente a mensajes y comandos rápidos.

## CopyL 3.3.0

CopyL 3.3 mantiene el alcance mínimo de 3.x —solo mensajes/comandos rápidos— y mejora la portabilidad de la configuración y la fiabilidad del deploy.

### Qué hace

CopyL ofrece **10 slots** independientes. Cada slot tiene:

- nombre;
- mensaje o comando;
- atajo propio de teclado o mouse.

Si el texto empieza con `/`, CopyL lo envía como comando. En cualquier otro caso lo envía como mensaje de chat.

El contenido se envía **exactamente como fue escrito**. No existen variables, placeholders ni lectura de coordenadas/vida/objetivos.

Los atajos se leen directamente y **no aparecen en `Opciones > Controles`**.

## Atajos de teclado y mouse

La tecla global y los 10 slots aceptan:

- teclas del teclado;
- botones del mouse soportados por GLFW.

La tecla de apertura por defecto es `Alt derecho` y se cambia desde:

`Mods > CopyL > Config`

Si un atajo ya pertenece a otro slot, al reasignarlo se mueve al slot nuevo. La tecla usada para abrir CopyL queda reservada y no puede activar un mensaje al mismo tiempo.

## Editor

La interfaz usa tarjetas `01–10` y mantiene:

- estado visual distinto para slot completo, incompleto o vacío;
- etiqueta `CHAT` / `CMD` / `KEY`;
- botón `×` para limpiar mensaje + atajo sin borrar el nombre;
- dos columnas en pantallas amplias;
- paginación automática en ventanas pequeñas;
- soporte para GUI Scale alto;
- resumen de mensajes y atajos activos;
- avisos de conflictos dentro de la propia pantalla;
- `Ctrl+S` y `Ctrl+Enter` para guardar;
- confirmación antes de descartar cambios sin guardar.

## Backup y restauración 3.3

Desde `Mods > CopyL > Config` hay dos acciones nuevas:

- **Copiar backup** — coloca en el portapapeles un JSON portable con el atajo global y los 10 nombres, mensajes y atajos.
- **Importar backup** — restaura ese JSON en otra instancia.

La importación:

1. exige una segunda confirmación para no sobrescribir la configuración por accidente;
2. valida formato, versión del esquema, cantidad de slots y bindings;
3. elimina conflictos con el atajo global;
4. usa el guardado transaccional de CopyL.

El backup no contiene información externa al mod: sólo la configuración de CopyL.

## Guardado transaccional

Los archivos son:

- `config/copyl.json` — atajo global;
- `config/copyl-messages.json` — nombres, mensajes y atajos de los 10 slots.

CopyL normaliza y valida el estado antes de escribirlo. Si Windows, un antivirus u otro proceso bloquea temporalmente un archivo, la interfaz conserva los cambios abiertos y muestra el error en vez de aparentar que se guardó correctamente.

Si un JSON está corrupto, CopyL intenta moverlo a `*.broken-<timestamp>` y reconstruir una configuración válida.

## Idiomas

Incluye:

- `en_us`;
- `es_es`;
- `es_ar`;
- `es_cl`;
- `es_ec`;
- `es_mx`;
- `es_uy`;
- `es_ve`.

## Qué NO incluye

CopyL no contiene:

- Loot ESP;
- Smart Offhand;
- Advanced Recon;
- HUD táctico;
- Notification Center;
- JourneyMap+;
- ruleta de módulos;
- editor de HUD;
- variables/placeholders;
- dependencias de JourneyMap.

## Compatibilidad

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- sin componentes server-side
- sin dependencias adicionales

## Build

```bash
./gradlew build
```

El JAR final es:

`build/libs/copyl-3.3.0.jar`

GitHub Actions:

- compila con Java 17;
- valida que el JAR se abra;
- verifica `META-INF/mods.toml` y `CopyL.class`;
- calcula SHA-256;
- publica el artifact `CopyL-<version>`;
- en `main`, publica únicamente `copyl-latest.jar.b64`, `version.txt`, `sha256.txt` y `source-commit.txt` en `build-output`;
- publica desde un worktree aislado para que `.gradle/`, `build/` o el wrapper generado nunca contaminen `build-output`;
- serializa builds de una misma rama sin permitir que eventos viejos cancelen el HEAD actual.

## Deploy

El PowerShell de deploy no compila nada localmente. Lee `version.txt` y `sha256.txt`, descarga `copyl-latest.jar.b64`, verifica su SHA-256 y reemplaza únicamente versiones anteriores de CopyL/Lclient en la instancia configurada. El mismo script puede instalar futuras versiones publicadas sin editar el número de versión a mano.
