# Changelog

Todos los cambios importantes de Lclient se documentan aquí.

## 2.8.0 — Tactical Suite

### Advanced Recon

- Añade memoria visual de objetivo durante ~650 ms para evitar que el Target Panel parpadee por mínimos movimientos de mira.
- La memoria se limpia al soltar zoom, cambiar de mundo, desconectarse o desaparecer la entidad.
- Los waypoints siguen usando siempre un raycast fresco y nunca el objetivo recordado.
- Nueva telemetría opcional de entidades ya cargadas: velocidad, cierre relativo, movimiento lateral, tendencia acercándose/alejándose, distancia horizontal, `ΔY`, rumbo cardinal y ángulo respecto a la mira.
- Target Panel movible entre cuatro esquinas y altura dinámica según datos disponibles.

### Loot ESP

- La lista HUD añade rumbo cardinal y diferencia vertical del drop más cercano de cada grupo.
- Dirección/altura se puede desactivar.
- El panel puede moverse entre las cuatro esquinas.
- Continúa reutilizando la caché del renderer; no aparece un segundo escaneo de `ItemEntity`.

### Smart Offhand

- Nueva protección de offhand de combate, activada por defecto.
- Tótems y escudos no se reemplazan automáticamente por comida.
- El aviso de objeto protegido se deduplica para no repetirse cada tick.
- La protección se puede activar/desactivar desde Smart Offhand.

### CopyL

- Añade `{yaw}` y `{pitch}`.
- Nuevas variables de objetivo: `{targettype}`, `{targethp}`, `{targetmaxhp}`, `{targetspeed}`, `{targetdy}`, `{targetbearing}`, `{targetmotion}` y `{targetitem}`.
- Las variables tácticas usan Recon cuando está en zoom y `minecraft.hitResult` fuera de Recon.
- Nueva referencia de variables integrada, paginada y responsive.
- Abrir/cerrar la ayuda conserva los borradores del editor.

### Notification Center

- El HUD de avisos puede moverse entre las cuatro esquinas.
- Nueva alerta preventiva cuando quedan 1–2 slots del inventario principal.
- Conserva aviso separado al llegar a inventario completamente lleno.
- Las alertas siguen funcionando por transición de estado, evitando spam por tick.

### HUD y navegación

- Nuevo editor `Distribución y telemetría HUD` con previsualización.
- Avisos, Loot HUD y Recon Panel tienen posiciones persistentes independientes.
- La configuración evita/repara esquinas duplicadas para que los tres paneles no se solapen.
- Layout por defecto: Loot arriba izquierda, Recon arriba derecha, Avisos abajo derecha.
- El centro de la ruleta abre Config global/HUD.
- La ruleta muestra estado táctico resumido: HUD, `SAFE`, telemetría, teclas y alcance.

### Compatibilidad y release

- Sigue siendo 100% client-side.
- Minecraft 1.20.1, Forge 47.x, Java 17.
- JourneyMap sigue siendo opcional/`compileOnly`.
- Sin nuevas dependencias obligatorias ni cambios de networking.

## 2.7.0 — Información táctica y UX

### Notification Center

- Nuevo HUD de avisos no-chat con severidades, deduplicación, fade y entre 1–5 tarjetas simultáneas.
- Duración configurable entre 2–10 segundos.
- Historial de hasta 48 avisos por sesión, con paginación y limpieza manual; no se persiste al disco.
- Avisos de activación/desactivación de módulos y estados de Recon.
- Smart Offhand informa cuándo equipa/restaura comida o cuándo abandona una restauración por seguridad.
- Alertas críticas opcionales para vida ≤25%, inventario lleno, item de mano ≤10% de durabilidad y armadura ≤10%.
- Las alertas críticas sólo se disparan al entrar en el estado o cambiar el objeto crítico, evitando spam por tick.

### CopyL

- Variables de objetivo `{target}`, `{targetdist}`, `{targetpos}`, `{targetx}`, `{targety}` y `{targetz}`.
- Durante Recon usa el raycast largo; fuera de Recon usa el objetivo vanilla del cliente.
- Editor ultracompacto de dos líneas por slot para ventanas angostas.
- Paginación dinámica de 2/3/5 slots según espacio disponible.
- Ayuda de variables actualizada dentro del propio editor.

### Loot ESP

- Nueva lista HUD opcional de loot cercano agrupada por item, con cantidad y distancia.
- Reutiliza la caché existente; no realiza un segundo escaneo de entidades.
- Caché ordenada una vez por escaneo para servir al renderer y al HUD.
- Beacon y lista HUD se configuran de forma independiente desde Loot ESP.
- Ajustes de módulo eliminan el antiguo ancho mínimo de 180 px.

### Advanced Recon

- Target Panel muestra item de mano/offhand y cantidad de piezas de armadura visibles cuando esos datos existen en el cliente.
- Conserva fallbacks seguros ante nombres, tipos, vida o items modded defectuosos.

### Smart Offhand

- Feedback integrado en Notification Center para equipar/restaurar.
- Aviso explícito cuando una restauración se abandona porque el slot original ya no coincide.
- Mantiene restauración exacta por item/tags/cantidad y aislamiento de hooks modded.

### Configuración e interfaz

- `lclient.json` añade opciones para Notification Center, alertas críticas y HUD de Loot ESP.
- Config global adapta siete filas de controles a ventanas bajas/GUI Scale alto.
- Historial de avisos y ajustes de módulo son responsive en resoluciones estrechas.
- Texto interno de notificaciones trunca Unicode sin partir pares UTF-16.

## 2.6.0 — Hardening completo

### Estabilidad

- JourneyMap+ sigue `MAPPING_STARTED`, `MAPPING_STOPPED` y `DISPLAY_UPDATE`; no conserva objetos de sesiones viejas y vuelve a publicar el waypoint transitorio cuando JourneyMap refresca displays.
- Loot ESP libera cachés y referencias al mundo al desactivarse/salir.
- Smart Offhand valida exactamente el stack original antes de restaurarlo y puede diferir una restauración temporalmente insegura.
- CopyL sincroniza estados físicos de teclas para evitar pulsaciones fantasma después de menús o reasignaciones.
- Recon sincroniza la tecla de waypoint incluso mientras hay una GUI abierta.

### Compatibilidad modded

- Smart Offhand usa propiedades de comida por `ItemStack` y aísla hooks defectuosos de items modded.
- AUTO penaliza efectos perjudiciales sin alterar la prioridad de una comida elegida manualmente.
- Selector, ruleta y configuración usan fallbacks cuando un item modded falla al generar nombre/propiedades.
- Recon tolera hooks defectuosos de colisión/pickability, nombres, tipos y vida de entidades custom.

### Rendimiento

- Recon cachea durante una ventana mínima el raycast largo cuando la cámara/mira no cambia y fuerza un raycast fresco al crear un waypoint.
- Loot ESP mantiene escaneo corto cacheado y limita lotes visuales patológicos a los 256 drops más cercanos.
- Los cambios rápidos del zoom siguen usando guardado con debounce.

### Interfaz y entrada

- Ruleta responsive.
- CopyL usa 2 páginas de 5 slots en GUI compacta.
- Selector de comida, configuración global, ajustes de módulos y HUD Recon se adaptan a GUI Scale alto.
- La rueda de Recon acumula deltas fraccionarios para trackpads/ruedas de alta resolución.
- Recon nunca amplía el FOV por encima del FOV real del jugador.

### Configuración y CI

- `lclient.json` y `copyl-messages.json` persisten automáticamente reparaciones/migraciones válidas.
- CopyL sanea controles, Unicode UTF-16 incompleto, arrays viejos, duplicados y conflictos de teclas.
- Proyecto Gradle renombrado internamente a Lclient.
- GitHub Actions valida el JAR, clase principal, `mods.toml`, SHA-256 y evita publicaciones obsoletas de `main`.

## 2.5.0 — Estabilidad de sesiones

- Reestructuración del ciclo de vida de JourneyMap+.
- Persistencia atómica y recuperación de JSON corruptos.
- Editor CopyL transaccional.
- Caché inicial de Loot ESP.
- Recuperación de Smart Offhand al desconectar.
- Guardado del zoom Recon con debounce.

## 2.4.0 — Configuración modular

- Loot ESP x-ray configurable y toggle propio.
- Smart Offhand con selección de comida.
- CopyL con nombres, variables dinámicas y teclas únicas.
- Recon y JourneyMap con waypoints basados en objetivo.
- Protección global de conflictos de teclas.
