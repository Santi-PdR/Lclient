# CopyL

CopyL es un mod **100% client-side** para Minecraft Forge 1.20.1 dedicado únicamente a mensajes y comandos rápidos.

## CopyL 3.2.0

CopyL 3.2 mantiene el alcance mínimo de 3.x —solo mensajes/comandos rápidos— pero mejora bastante la robustez, los atajos y la experiencia del editor.

### Qué hace

CopyL ofrece **10 slots** independientes. Cada slot tiene:

- nombre;
- mensaje o comando;
- atajo propio de teclado o mouse.

Si el texto empieza con `/`, CopyL lo envía como comando. En cualquier otro caso lo envía como mensaje de chat.

El contenido se envía **exactamente como fue escrito**. No existen variables, placeholders ni lectura de coordenadas/vida/objetivos.

Los atajos se leen directamente y **no aparecen en `Opciones > Controles`**.

## Atajos de teclado y mouse

Desde 3.2 tanto la tecla global como los 10 slots aceptan:

- teclas del teclado;
- botones del mouse soportados por GLFW.

La tecla de apertura por defecto sigue siendo `Alt derecho`.

Se cambia desde:

`Mods > CopyL > Config`

Si un atajo ya pertenece a otro slot, al reasignarlo se mueve al slot nuevo en lugar de dejar dos acciones superpuestas.

La tecla usada para abrir CopyL queda reservada y no puede activar un mensaje al mismo tiempo.

## Editor 3.2

La interfaz mantiene el diseño de tarjetas de 3.1 y añade más feedback útil:

- tarjetas `01–10` con hover;
- estado visual distinto para slot completo, incompleto o vacío;
- etiqueta `CHAT` / `CMD` según el contenido;
- botón `×` para limpiar rápidamente mensaje + atajo de un slot sin borrar su nombre;
- dos columnas en pantallas amplias;
- paginación automática en ventanas pequeñas;
- soporte para GUI Scale alto;
- resumen de mensajes y atajos activos;
- avisos de conflictos dentro de la propia pantalla.

Atajos del editor:

- `Ctrl+S` — guardar;
- `Ctrl+Enter` — guardar;
- `Backspace/Delete` durante captura — quitar el atajo;
- `Esc` durante captura — cancelar la captura.

### Protección de cambios sin guardar

Si modificaste algo y pulsás `Cancelar` o `Esc`, CopyL **no descarta inmediatamente** el trabajo. Primero avisa; hay que repetir la acción durante unos segundos para confirmar el descarte.

## Guardado transaccional

El editor ya no modifica primero el estado en memoria para luego intentar escribirlo.

En 3.2:

1. se normalizan los 10 slots;
2. se valida que no existan atajos duplicados/conflictivos;
3. se escribe el JSON de forma atómica;
4. sólo si esa escritura funciona se reemplaza la configuración viva.

Si Windows, un antivirus u otro proceso bloquea temporalmente el archivo, el editor permanece abierto y muestra un error para poder reintentar. No aparenta haber guardado algo que en realidad falló.

Los archivos son:

- `config/copyl.json` — atajo global;
- `config/copyl-messages.json` — nombres, mensajes y atajos de los 10 slots.

Si un JSON está corrupto, CopyL intenta moverlo a `*.broken-<timestamp>` y reconstruir una configuración válida.

## Idiomas

La interfaz usa traducciones reales en vez de texto español hardcodeado.

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

Desde CopyL 3.0 el proyecto dejó de ser un cliente modular. No contiene:

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

`build/libs/copyl-3.2.0.jar`

GitHub Actions:

- compila con Java 17;
- valida que el JAR se abra;
- verifica `META-INF/mods.toml` y `CopyL.class`;
- calcula SHA-256;
- publica el artifact `CopyL-<version>`;
- en `main`, publica `copyl-latest.jar.b64`, `version.txt`, `sha256.txt` y `source-commit.txt` en `build-output`.

## Deploy

El PowerShell de deploy no compila nada localmente. Lee `version.txt` y `sha256.txt`, descarga `copyl-latest.jar.b64`, verifica su SHA-256 y reemplaza únicamente versiones anteriores de CopyL/Lclient en la instancia configurada. Por eso el mismo script puede instalar futuras versiones publicadas sin editar el número de versión a mano.
