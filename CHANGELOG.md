## 3.9.1 — Plain, reliable lettering

CEDA Dispatch settings and message cards now use Minecraft's default font instead of a custom font provider that could render every label as empty boxes. The text stays unshadowed and keeps its paper-and-ink palette.

## 3.9.0 — Clear type for field reports

- Give CEDA Dispatch its own Atkinson Hyperlegible typeface, sized for quick reading over parchment.
- Use the same font for buttons, headings, notices, and editable field copy; preserve cursor, selection, and horizontal scrolling.
- Bundle the original SIL Open Font License with the font.
- Keep Minecraft's other screens and gameplay typography unchanged.

## 3.8.0 — A clear mark for the next hand

- Give the focused keyboard control a still, two-tone CEDA outline so the next action is easy to spot.
- Keep focus visible on each paper button without changing its size or adding movement.
- Show an ellipsis when a narrow button cannot fit its label, while preserving the full accessible label.
- Keep gameplay and inventory screens untouched.

## 3.7.0 — Clear ink, field-worn paper

- Flush the editor's first text pass before clearing its shadow, then draw the editable copy once in clean ink.
- Give field-message cards a paper-cut edge, ruled writing line, and a quiet slot divider.
- Soften the ink color and replace the clipped settings explanation with a short bilingual hint.
- Keep all changes still and limited to the CEDA menu.

# Changelog

## 3.6.0 — Clean ink, clear controls

- Replace the default grey controls with paper, sepia ink, and Ghoul teal buttons; give the main action a clear visual priority.
- Draw button labels, page marks, slot labels, and editable text without the heavy default drop shadow.
- Preserve normal text entry, cursor placement, selection, and horizontal scrolling while keeping the menu static.

## 3.5.0 — compact field desk

- Fit the settings panel to the available window and keep its backup actions usable in compact layouts.
- Paginate the message editor by the rows that actually fit; keep save and cancel below the visible cards, including short and narrow windows.

## 3.4.0 — CEDA dispatch paper

- Rename the visible mod and settings-screen header to CEDA Dispatch while retaining the copyl mod ID, Java package, JSON schema, and config paths.
- Restyle the settings and message editor around static parchment, sepia ink, and restrained Ghoul teal accents.
- Add an optional, quiet shortcut from GripeVerde's title screen when both mods are installed; no required dependency is added.

## 3.3.0 — portable profiles + release hardening

### Backup / restauración

- Nuevo **backup completo por portapapeles** desde `Mods > CopyL > Config`.
- El backup incluye atajo global y los 10 nombres, mensajes y atajos.
- Formato JSON portable con identificador `copyl-profile` y esquema versionado.
- La importación valida estructura, cantidad de slots y bindings antes de tocar la configuración.
- `Importar backup` exige una segunda confirmación para evitar reemplazos accidentales.
- Conflictos entre el atajo global y slots se limpian antes de persistir.
- Si falla la persistencia, la interfaz informa el error en lugar de mostrar un éxito falso.

### Interfaz / idiomas

- La pantalla Config incorpora `Copiar backup` e `Importar backup` sin sacar los controles existentes.
- Layout ajustado para mantener buen comportamiento con GUI Scale alto.
- Nuevos mensajes de backup/restauración en inglés y todas las variantes de español incluidas por CopyL.

### Build / deploy

- `build-output` ahora se publica desde un **worktree aislado**.
- La publicación limpia el árbol y deja únicamente `copyl-latest.jar.b64`, `version.txt`, `sha256.txt` y `source-commit.txt`.
- Archivos generados por Gradle ya no pueden contaminar la rama de payload.
- Los workflows de una misma ref se serializan sin cancelar el build del HEAD actual por eventos retrasados.
- Se ignora explícitamente `gradle/wrapper/gradle-wrapper.jar` cuando el wrapper lo genera localmente.

## 3.2.0 — input + persistence hardening

### Atajos

- Soporte de **teclado y botones del mouse** para abrir CopyL y para los 10 slots.
- Nuevo codec de bindings que conserva compatibilidad con las teclas numéricas guardadas por 3.0/3.1.
- Un atajo reasignado se mueve del slot anterior al nuevo en lugar de duplicarse.
- El atajo global sigue reservado para abrir CopyL y no puede disparar un mensaje simultáneamente.
- El runtime mantiene sincronizado el estado físico aunque haya una pantalla abierta para evitar pulsaciones fantasma al volver al juego.

### Guardado y seguridad

- Guardado de los 10 slots ahora transaccional: el estado vivo sólo cambia después de escribir el JSON correctamente.
- `MessageConfig.save()` y `CopyLConfig.save()` reportan fallos reales a la interfaz.
- Si falla el guardado del editor, la pantalla permanece abierta con los cambios listos para reintentar.
- Al cambiar el atajo global, un fallo de disco restaura el atajo anterior en memoria.
- Limpieza de conflictos de slots también usa persistencia segura.
- `Cancelar`/`Esc` requieren confirmación cuando existen cambios sin guardar.

### Editor / apariencia

- Hover visual en las tarjetas.
- Colores distintos para slots listos, incompletos y vacíos.
- Indicadores `CHAT`, `CMD` y `KEY`.
- Botón `×` por slot para limpiar mensaje + atajo sin borrar el nombre.
- `Ctrl+S` se suma a `Ctrl+Enter` para guardar.
- Feedback más claro para conflictos, guardados fallidos y reasignaciones.
- Config general actualizada para capturar teclado/mouse y mostrar errores de persistencia.

### Idiomas

- La interfaz deja de depender de textos españoles hardcodeados.
- Inglés `en_us`.
- Español: `es_es`, `es_ar`, `es_cl`, `es_ec`, `es_mx`, `es_uy`, `es_ve`.

### Mantenimiento

- Se limita localmente el warning de removal de `ModLoadingContext#get()` que Forge 47.x todavía requiere para la pantalla Config.
- Sigue sin variables, ESP, Recon, JourneyMap, Smart Offhand ni módulos adicionales.

## 3.1.0 — visual refresh + literal messages

### Eliminado

- Sistema completo de variables/placeholders.
- Lectura de coordenadas, vida, hambre, orientación y datos del objetivo para mensajes.
- `CopyLVariablesScreen` y todos los botones/textos relacionados con variables.

Los slots ahora envían exactamente el texto o comando que el usuario escribió.

### Apariencia

- Editor rediseñado con tarjetas visuales por slot.
- Numeración `01–10` y acento visual para distinguir slots configurados.
- Nombre, tecla y mensaje agrupados de forma más clara.
- Encabezado con cantidad de slots configurados y atajos asignados.
- Diseño de dos columnas en resoluciones amplias.
- Paginación responsive en resoluciones pequeñas.
- Mejor fondo, jerarquía visual y separación entre contenido/acciones.
- Config general rediseñada como panel compacto con resumen de estado.
- Mejor integración con GUI Scale alto.

## 3.0.0 — CopyL-only reset

El proyecto volvió a ser exclusivamente CopyL.

### Eliminado completamente

- Loot ESP.
- Smart Offhand.
- Advanced Recon.
- Recon Telemetry.
- Target/HUD táctico.
- Notification Center e historial.
- JourneyMap+ y su plugin/bridge.
- Ruleta de módulos.
- Editor de distribución HUD.
- Selector de comida.
- Configuración modular de Lclient.
- Dependencia `compileOnly` de JourneyMap.

### Identidad y build

- `modId` volvió a ser `copyl`.
- Display name: `CopyL`.
- Proyecto Gradle: `CopyL`.
- Publicación en `build-output`: `copyl-latest.jar.b64`.
