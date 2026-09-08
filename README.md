# Lclient

Lclient es un cliente modular **100% client-side** para Minecraft Forge 1.20.1.

## Módulos

La ruleta central administra cinco módulos:

- CopyL
- Loot ESP
- Smart Offhand
- Advanced Recon
- JourneyMap+

La tecla global de la ruleta no aparece en `Opciones > Controles`: se cambia desde `Mods > Lclient > Config`. Las teclas internas y opciones de módulo viven dentro de la propia ruleta.

## Lclient 2.8.0

2.8 profundiza la capa táctica de 2.7 sin reintroducir módulos descartados ni duplicar funciones de otros mods del pack.

### Advanced Recon

Recon mantiene zoom render-only, rueda de magnificación, raycast largo configurable y waypoint opcional de JourneyMap.

Novedades de 2.8:

- **TRACK estable**: conserva el último objetivo de entidad durante ~650 ms para que el Target Panel no desaparezca por un pequeño movimiento de mira;
- el TRACK se invalida al dejar de hacer zoom, cambiar de mundo, desconectarse o desaparecer la entidad;
- crear waypoint sigue obligando a un **raycast fresco**: la memoria visual nunca se usa para marcar una posición vieja;
- telemetría opcional basada únicamente en entidades ya cargadas por el cliente:
  - velocidad del objetivo en m/s;
  - velocidad de cierre/apertura relativa;
  - movimiento lateral izquierda/derecha;
  - acercándose/alejándose/estable;
  - distancia horizontal;
  - diferencia vertical `ΔY`;
  - rumbo cardinal;
  - ángulo del objetivo respecto a la mira;
- Target Panel movible entre las cuatro esquinas;
- panel dinámico: sólo reserva filas para datos realmente disponibles;
- mantiene nombre, tipo, distancia, coordenadas, HP, item visible y cantidad de piezas de armadura.

Los hooks defectuosos de entidades/items modded siguen aislados con fallbacks.

### Loot ESP

Loot ESP continúa usando sólo `ItemEntity` ya presentes en el `ClientLevel`.

- cajas `NO_DEPTH_TEST` a través de terreno;
- beacon vertical opcional;
- alcance, stack mínimo y tecla configurables;
- escaneo corto cacheado;
- prioridad para los drops más cercanos en acumulaciones grandes;
- limpieza de caché al desactivar/cambiar de sesión.

El HUD de loot ahora puede mostrar por grupo:

- nombre;
- cantidad acumulada;
- distancia del drop más cercano;
- rumbo cardinal;
- diferencia vertical `ΔY`.

El HUD reutiliza la misma caché de Loot ESP: **no ejecuta un segundo escaneo de entidades**. Puede moverse entre las cuatro esquinas y la dirección/altura se puede desactivar.

### Smart Offhand

Mantiene selección `AUTO` o comida exacta, fallback opcional y restauración transaccional segura.

2.8 añade protección de combate activada por defecto:

- si la offhand contiene un **tótem** o un **escudo**, Smart Offhand no lo reemplaza automáticamente por comida;
- emite un único aviso al detectar el objeto protegido, sin repetir cada tick;
- la protección se puede desactivar desde los ajustes de Smart Offhand;
- restaurar sigue exigiendo que item, tags y cantidad del slot original coincidan.

### CopyL

Sigue ofreciendo 10 slots con nombre, tecla única, guardado transaccional y layouts normal/compacto/ultracompacto.

Variables del jugador:

- `{pos}`, `{x}`, `{y}`, `{z}`
- `{dim}`
- `{hp}`
- `{food}`
- `{name}`
- `{yaw}`
- `{pitch}`

Variables del objetivo:

- `{target}`
- `{targettype}`
- `{targetdist}`
- `{targetpos}`, `{targetx}`, `{targety}`, `{targetz}`
- `{targethp}`, `{targetmaxhp}`
- `{targetspeed}`
- `{targetdy}`
- `{targetbearing}`
- `{targetmotion}`
- `{targetitem}`

Con Recon activo usa el raycast largo; fuera de Recon usa `minecraft.hitResult`. No obtiene entidades o bloques que el cliente no conozca.

El editor incorpora una **referencia de variables dentro del juego**, paginada y responsive. Entrar y salir de la ayuda conserva los borradores sin guardar.

### Notification Center

Mantiene tarjetas no-chat, severidad, deduplicación, fade, duración configurable, historial temporal y alertas críticas.

2.8 mejora las alertas de inventario:

- avisa al quedar **2 o 1 slots libres**;
- escala a `Inventario lleno` al llegar a 0;
- sólo avisa al entrar en cada estado, no cada tick.

Continúan las alertas para vida ≤25% y durabilidad crítica de item/armadura.

### Editor de HUD

`Mods > Lclient > Config > Distribución y telemetría HUD` permite:

- mover Avisos;
- mover Loot HUD;
- mover Recon Panel;
- activar/desactivar dirección/altura de loot;
- activar/desactivar telemetría Recon;
- restaurar el layout por defecto;
- ver una previsualización de las posiciones.

Los tres paneles no pueden terminar guardados en la misma esquina: la configuración repara automáticamente colisiones y deja una cuarta esquina libre.

Layout por defecto:

- Loot: arriba izquierda;
- Recon: arriba derecha;
- Avisos: abajo derecha.

### Ruleta como hub

- click izquierdo sobre módulo: configurar;
- click derecho: activar/desactivar;
- el centro de la ruleta abre **Configuración global + HUD**;
- el centro muestra si los avisos globales están activos;
- cada módulo resume su estado: HUD de Loot, protección `SAFE` de Smart Offhand, telemetría Recon, teclas y alcance.

### JourneyMap+

JourneyMap sigue siendo completamente opcional:

- `compileOnly`;
- display `lclient_recon`;
- waypoint transitorio;
- lifecycle `MAPPING_STARTED`, `MAPPING_STOPPED`, `DISPLAY_UPDATE`;
- no conserva objetos `Waypoint` de sesiones anteriores;
- Recon puede funcionar sin JourneyMap instalado.

## Configuración resistente a fallos

`lclient.json` y `copyl-messages.json` usan escritura temporal + reemplazo atómico cuando el sistema lo permite.

Si un JSON se corrompe:

1. se mueve a `*.broken-<timestamp>`;
2. se crea una configuración válida;
3. Lclient no vuelve a intentar leer el mismo archivo roto en cada inicio.

También se migran y reparan automáticamente rangos, keycodes, IDs, conflictos, arrays antiguos y ahora posiciones HUD inválidas/solapadas.

## Compatibilidad

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- JourneyMap 1.20.1-5.10.x para JourneyMap+
- JourneyMap opcional

## Build y publicación

```bash
./gradlew build
```

GitHub Actions:

- compila con Java 17;
- selecciona exactamente `lclient-<version>.jar`;
- valida que el JAR se pueda abrir;
- comprueba `META-INF/mods.toml` y la clase principal;
- calcula SHA-256;
- cancela builds obsoletos de la misma rama;
- evita que un build viejo de `main` publique encima de uno nuevo;
- publica en `build-output` `lclient-latest.jar.b64`, `version.txt`, `sha256.txt` y `source-commit.txt`.

El despliegue final puede limitarse a descargar ese payload, comprobar SHA-256 y copiar el JAR a la instancia; no requiere compilar localmente.
