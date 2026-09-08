# CopyL

CopyL es un mod **100% client-side** para Minecraft Forge 1.20.1 dedicado únicamente a mensajes y comandos rápidos.

## CopyL 3.0.0

A partir de 3.0 el proyecto deja de ser un cliente modular. Se eliminaron completamente del código y del JAR:

- Loot ESP
- Smart Offhand
- Advanced Recon
- HUD táctico
- Notification Center
- JourneyMap+
- ruleta de módulos
- editor de HUD
- cualquier dependencia de JourneyMap

No están ocultos ni desactivados: sus clases, pantallas, configuración y dependencias fueron borradas.

## Función principal

CopyL ofrece **10 slots** independientes. Cada slot tiene:

- nombre;
- mensaje o comando;
- tecla propia.

Si el texto empieza con `/`, CopyL lo envía como comando. En cualquier otro caso lo envía como mensaje de chat.

Las teclas de CopyL se leen directamente con GLFW y **no aparecen en `Opciones > Controles`**.

## Abrir el editor

La tecla de apertura por defecto es `Alt derecho`.

Se puede cambiar desde:

`Mods > CopyL > Config`

La pantalla de configuración sólo contiene:

- tecla para abrir CopyL;
- botón para abrir el editor de mensajes.

Si venías de Lclient 2.x, CopyL intenta conservar la antigua tecla de la ruleta como nueva tecla de apertura. Después migra `config/lclient.json` a `config/copyl.json` y elimina el archivo viejo.

## Editor

El editor mantiene:

- guardado transaccional;
- Cancelar sin aplicar cambios;
- `Ctrl+Enter` para guardar;
- nombres de hasta 24 caracteres;
- mensajes de hasta 256 caracteres;
- prevención de teclas duplicadas;
- prevención de conflicto con la tecla global de apertura;
- layouts adaptativos para ventanas pequeñas y GUI Scale alto;
- paginación automática;
- ayuda de variables integrada.

La configuración de mensajes se guarda en:

`config/copyl-messages.json`

## Variables

### Jugador

- `{pos}` — coordenadas completas
- `{x}` `{y}` `{z}` — coordenadas separadas
- `{dim}` — dimensión
- `{hp}` — vida
- `{food}` — hambre
- `{name}` — nombre
- `{yaw}` — giro horizontal de cámara
- `{pitch}` — giro vertical de cámara

### Objetivo actual

Estas variables usan **únicamente `minecraft.hitResult`**, es decir, el objetivo que ya está bajo la mira vanilla del cliente.

- `{target}` — nombre del objetivo
- `{targettype}` — tipo/registry ID
- `{targetdist}` — distancia
- `{targetpos}` — coordenadas completas
- `{targetx}` `{targety}` `{targetz}` — coordenadas separadas
- `{targethp}` `{targetmaxhp}` — vida actual/máxima conocida
- `{targetspeed}` — velocidad aproximada en m/s
- `{targetdy}` — diferencia de altura respecto al jugador
- `{targetbearing}` — rumbo cardinal
- `{targetmotion}` — izquierda/derecha/acercándose/alejándose/estable
- `{targetitem}` — item visible en mano u offhand

Para bloques, las variables que sólo aplican a entidades devuelven `-`.

## Configuración resistente a fallos

`copyl.json` y `copyl-messages.json` usan escritura temporal y reemplazo atómico cuando el sistema lo permite.

Si un JSON está corrupto:

1. se mueve a `*.broken-<timestamp>`;
2. se genera una configuración válida;
3. CopyL puede volver a iniciar sin releer indefinidamente el archivo roto.

`copyl-messages.json` también repara automáticamente arrays antiguos, teclas inválidas, duplicados y conflictos con la tecla global.

## Compatibilidad

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- sin dependencia de JourneyMap
- sin componente server-side

## Build

```bash
./gradlew build
```

El JAR final es:

`build/libs/copyl-3.0.0.jar`

GitHub Actions:

- compila con Java 17;
- valida que el JAR se abra;
- verifica `META-INF/mods.toml` y `CopyL.class`;
- calcula SHA-256;
- publica el artifact `CopyL-<version>`;
- en `main`, publica `copyl-latest.jar.b64`, `version.txt`, `sha256.txt` y `source-commit.txt` en `build-output`;
- elimina el antiguo payload `lclient-latest.jar.b64` al publicar CopyL 3.x.
