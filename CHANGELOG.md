# Changelog

Todos los cambios importantes de Lclient se documentan aquí.

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
