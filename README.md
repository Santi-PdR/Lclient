# CopyL

Mod **client-side** para Minecraft Forge 1.20.1 que permite guardar y enviar rápidamente hasta 10 mensajes o comandos mediante teclas configurables.

## Funciones

- 10 espacios de mensajes independientes.
- Cada espacio aparece en **Opciones → Controles → CopyL** y puede tener su propia tecla.
- Los 10 atajos empiezan sin tecla asignada para evitar conflictos.
- `-` abre por defecto la interfaz de edición de CopyL.
- Cada espacio admite hasta 256 caracteres.
- Texto normal se envía al chat exactamente como fue escrito, por ejemplo `**Congelar**`.
- Si el contenido empieza con `/`, se envía como comando.
- Los mensajes se guardan localmente en `config/copyl-messages.json`.
- No requiere instalar CopyL en el servidor.
- Los atajos no se disparan mientras hay otra pantalla abierta, evitando mensajes accidentales al escribir en chat, inventario o menús.

## Requisitos

- Minecraft 1.20.1
- Forge 47.x
- Java 17

El proyecto compila contra Forge **1.20.1-47.4.10** (versión recomendada de Forge para 1.20.1).

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

## Uso

1. Instala el JAR en la carpeta `mods` de tu cliente Forge 1.20.1.
2. Entra a **Opciones → Controles → CopyL** y asigna teclas a `Enviar espacio 1` ... `Enviar espacio 10`.
3. Dentro de un mundo o servidor, pulsa `-` para abrir el editor.
4. Escribe el texto de cada espacio y guarda.
5. Pulsa la tecla asignada a un espacio para enviarlo automáticamente al chat.
