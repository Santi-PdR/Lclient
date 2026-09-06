# CopyL

Mod **100% client-side** para Minecraft Forge 1.20.1 que permite guardar y enviar rápidamente hasta 10 mensajes o comandos mediante teclas configurables.

## CopyL 1.2.0

- 10 mensajes rápidos independientes.
- Cada mensaje tiene su propio keybind.
- La tecla y el texto se pueden editar **directamente desde la pantalla de CopyL**.
- También siguen disponibles en **Opciones → Controles → CopyL**.
- `-` abre por defecto el editor.
- `Backspace` o `Delete` mientras editas una tecla la deja sin asignar.
- `Esc` cancela la captura de una tecla.
- Se admiten botones del ratón como keybind.
- Interfaz con animaciones suaves de entrada y hover.
- Feedback sonoro al pasar por slots y confirmar cambios.
- Texto normal se envía al chat tal cual fue escrito.
- Si el contenido empieza con `/`, se envía como comando.
- Hasta 256 caracteres por mensaje.
- Los mensajes se guardan en `config/copyl-messages.json`.
- Los keybinds se guardan mediante las opciones normales de Minecraft.
- Acceso desde **Mods → CopyL → Config**.
- No requiere instalar CopyL en el servidor.

## Uso

1. Instala el JAR en la carpeta `mods` de tu cliente Forge 1.20.1.
2. Entra a un mundo o servidor.
3. Pulsa `-` para abrir CopyL.
4. Escribe el contenido de cada mensaje.
5. Pulsa el botón de tecla del mismo slot y luego la tecla que quieras asignar.
6. Guarda y cierra.
7. Al pulsar esa tecla durante el juego, CopyL envía el mensaje automáticamente.

Ejemplos:

- `**Congelar**` → se envía como mensaje.
- `/spawn` → se envía como comando.

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

El JAR final queda en `build/libs/`.
