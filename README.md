# CopyL

CopyL es un mod **100% client-side** para Minecraft Forge 1.20.1 dedicado únicamente a mensajes y comandos rápidos.

## CopyL 3.1.0

CopyL 3.1 simplifica todavía más el mod y mejora su presentación visual.

### Qué hace

CopyL ofrece **10 slots** independientes. Cada slot tiene:

- nombre;
- mensaje o comando;
- tecla propia.

Si el texto empieza con `/`, CopyL lo envía como comando. En cualquier otro caso lo envía como mensaje de chat.

El contenido se envía **exactamente como fue escrito**. CopyL ya no sustituye variables, placeholders ni datos del jugador/objetivo.

Las teclas se leen directamente con GLFW y **no aparecen en `Opciones > Controles`**.

## Abrir CopyL

La tecla de apertura por defecto es `Alt derecho`.

Se puede cambiar desde:

`Mods > CopyL > Config`

La pantalla de configuración muestra:

- tecla para abrir CopyL;
- resumen de slots configurados y atajos asignados;
- acceso directo al editor.

## Editor 3.1

El editor fue rediseñado con una interfaz más limpia y consistente:

- tarjetas visuales por slot;
- numeración clara `01–10`;
- indicador visual de slots configurados;
- nombre, tecla y mensaje agrupados en la misma tarjeta;
- dos columnas en pantallas amplias;
- paginación automática en ventanas pequeñas;
- soporte para GUI Scale alto;
- encabezado con estado general;
- guardado y cancelación claramente separados;
- avisos de conflictos de teclas integrados en la interfaz.

El editor mantiene:

- guardado transaccional;
- `Cancelar` sin aplicar cambios;
- `Ctrl+Enter` para guardar;
- nombres de hasta 24 caracteres;
- mensajes de hasta 256 caracteres;
- prevención de teclas duplicadas;
- prevención de conflicto con la tecla global de apertura.

La configuración de mensajes se guarda en:

`config/copyl-messages.json`

La configuración general se guarda en:

`config/copyl.json`

## Configuración resistente a fallos

`copyl.json` y `copyl-messages.json` usan escritura temporal y reemplazo atómico cuando el sistema lo permite.

Si un JSON está corrupto:

1. se mueve a `*.broken-<timestamp>`;
2. se genera una configuración válida;
3. CopyL puede volver a iniciar sin releer indefinidamente el archivo roto.

`copyl-messages.json` también repara automáticamente arrays antiguos, teclas inválidas, duplicados y conflictos con la tecla global.

Si venías de Lclient 2.x, CopyL intenta conservar la antigua tecla de la ruleta como nueva tecla de apertura durante la migración.

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
- dependencias de JourneyMap.

En 3.1 también se eliminó completamente el antiguo sistema de variables y su pantalla de ayuda.

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

`build/libs/copyl-3.1.0.jar`

GitHub Actions:

- compila con Java 17;
- valida que el JAR se abra;
- verifica `META-INF/mods.toml` y `CopyL.class`;
- calcula SHA-256;
- publica el artifact `CopyL-<version>`;
- en `main`, publica `copyl-latest.jar.b64`, `version.txt`, `sha256.txt` y `source-commit.txt` en `build-output`.
