# Changelog

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
