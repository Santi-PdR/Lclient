# Lclient

Lclient es un cliente modular **100% client-side** para Minecraft Forge 1.20.1.

## Diseño

Todo se controla desde una única ruleta. CopyL es un módulo más del mismo cliente, no un menú/mod separado.

La tecla de apertura de la ruleta **no se registra en Opciones > Controles**. Se cambia desde `Mods > Lclient > Config`. Las teclas internas de CopyL, Recon y el resto de ajustes se editan desde la propia ruleta.

## Lclient 2.2.0

- **CopyL / Mensajes rápidos:** 10 mensajes o comandos con teclas propias, configurados dentro de la ruleta.
- **Sound Radar:** deja de usar una lista superior. Los sonidos relevantes aparecen alrededor de la mira según la dirección real desde la que llegan, con distancia y diferencia vertical. Agrupa repeticiones y puede ignorar sonidos generados por el jugador local.
- **Loot ESP:** resalta `ItemEntity` dentro del radio configurado y recuerda/restaura el estado de glow previo de cada item.
- **Combat + Notificaciones:** el target panel sigue disponible al apuntar a una entidad, pero el historial de golpes y los avisos ya no aparecen flotando durante el gameplay. La ruleta muestra un contador de elementos nuevos y al abrir el módulo aparece un centro con historial de avisos y combate, scroll y opciones para limpiar cada vista.
- **Smart Offhand:** mueve automáticamente una comida adecuada a la offhand cuando baja el hambre y restaura el objeto anterior sólo si puede hacerlo con seguridad. Si se desactiva mientras administra la offhand, intenta restaurar correctamente antes de abandonar el estado.
- **Entity Alerts:** un chunk debe permanecer cargado/estable antes de que una entidad nueva pueda generar aviso. Items, rayos y el jugador local siguen ignorados. Los avisos se guardan en el centro de notificaciones.
- **Advanced Recon:** ahora es un zoom táctico real. Mantener la tecla de Zoom reduce temporalmente el FOV y sólo durante ese zoom se muestran coordenadas/distancia del objetivo. Una segunda tecla crea el waypoint de Recon y sólo funciona mientras el zoom está activo. Ambas teclas y la potencia del zoom se configuran desde la ruleta.
- **JourneyMap+:** integración opcional con JourneyMap 1.20.1-5.10.x / API 1.9. Crea waypoints temporales para el último atacante y Recon, permite desactivar ambos por separado y limpiar los waypoints tácticos desde la ruleta.

## Teclas de Recon por defecto

- Zoom táctico: `C` (mantener pulsado).
- Waypoint de Recon: `V` (sólo mientras el zoom está abierto).

Estas teclas no aparecen en `Opciones > Controles`; se cambian entrando a **Advanced Recon** desde la ruleta.

## JourneyMap

El pack Jobs usa `journeymap-1.20.1-5.10.3-forge`, que implementa JourneyMap API `1.20-1.9-SNAPSHOT`. Lclient compila contra esa generación mediante el JAR 5.10.3 y mantiene las clases de JourneyMap detrás de un bridge opcional para que Lclient también pueda iniciar sin JourneyMap instalado.

## Compatibilidad

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- JourneyMap 1.20.1-5.10.x para JourneyMap+
- JourneyMap es opcional; el resto de Lclient funciona sin él.

## Build

```bash
./gradlew build
```

El workflow de GitHub Actions compila el JAR y publica el payload de despliegue desde `main`.
