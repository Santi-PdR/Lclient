# CopyL

CopyL es un mod **100% client-side** para Minecraft Forge 1.20.1 que permite guardar y enviar rápidamente hasta 10 mensajes o comandos mediante teclas configurables.

## Funciones

- 10 espacios de mensajes independientes.
- Cada espacio aparece en **Opciones → Controles → CopyL — Mensajes rápidos** y puede tener su propia tecla.
- Los 10 atajos empiezan sin tecla asignada para evitar conflictos.
- `-` abre por defecto la interfaz de edición de CopyL.
- El editor muestra junto a cada espacio la tecla que tiene asignada actualmente.
- Desde el editor puedes abrir directamente la pantalla de Controles.
- También puedes abrir el editor desde **Mods → CopyL → Config**.
- Cada espacio admite hasta 256 caracteres.
- Texto normal se envía al chat exactamente como fue escrito, por ejemplo `**Congelar**`.
- Si el contenido empieza con `/`, se envía como comando.
- Los mensajes se guardan localmente en `config/copyl-messages.json`.
- No requiere instalar CopyL en el servidor.
- Los atajos no se disparan mientras hay otra pantalla abierta, evitando mensajes accidentales al escribir en chat, inventario o menús.

## Cómo se usa

1. Instala el JAR en la carpeta `mods` de tu cliente Forge 1.20.1.
2. Entra a **Opciones → Controles → CopyL — Mensajes rápidos**.
3. Asigna una tecla a los espacios que quieras utilizar.
4. Dentro de un mundo o servidor, pulsa `-` para abrir el editor.
5. Escribe un mensaje normal o un `/comando` en cada espacio.
6. Guarda y pulsa la tecla asignada para enviarlo automáticamente.

La propia pantalla de CopyL muestra qué tecla corresponde a cada espacio para que no tengas que memorizarlo.

## Acceso desde la lista de Mods

En **Mods → CopyL** aparece una descripción completa del funcionamiento del mod. El botón **Config** abre directamente el editor de mensajes rápidos.

## Requisitos

- Minecraft 1.20.1
- Forge 47.x
- Java 17

El proyecto compila contra Forge **1.20.1-47.4.10**.

## Compilar

Windows:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

En la primera ejecución el script obtiene automáticamente el pequeño `gradle-wrapper.jar` oficial de Gradle 8.1.1 si todavía no existe. El JAR final queda en `build/libs/`.
