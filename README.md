# Lclient

Lclient es un cliente modular **100% client-side** para Minecraft Forge 1.20.1.

## Diseño

Todo se controla desde una única ruleta. CopyL es un módulo más del mismo cliente, no un menú/mod separado.

La tecla de apertura de la ruleta **no se registra en Opciones > Controles**. Se cambia desde `Mods > Lclient > Config`. Las teclas internas de CopyL y Recon se guardan en la configuración propia de Lclient y se editan desde la ruleta.

## Lclient 2.1.0

- **CopyL / Mensajes rápidos:** 10 mensajes o comandos con teclas propias, configurados dentro de la ruleta.
- **Sound Radar:** muestra dirección, diferencia vertical y distancia de sonidos relevantes. Agrupa sonidos repetidos cercanos para evitar que pasos/disparos llenen el HUD.
- **Loot ESP:** resalta `ItemEntity` dentro del radio configurado y recuerda/restaura el estado de glow previo de cada item.
- **Combat + Notificaciones:** panel del objetivo, daño recibido, entidad atacante real, tipo registrado, coordenadas de ambos y dimensión. Las notificaciones repetidas se agrupan.
- **Smart Offhand:** mueve automáticamente una comida adecuada a la offhand cuando baja el hambre y restaura el objeto anterior sólo si puede hacerlo con seguridad. Si el jugador modifica la offhand o el slot de respaldo, cancela la restauración antes de mover un objeto incorrecto.
- **Entity Alerts:** ya no interpreta como spawn todo `EntityJoinLevelEvent`. Un chunk debe permanecer cargado/estable antes de que una entidad nueva pueda generar aviso, evitando el spam de mobs preexistentes al entrar en zonas nuevas o al cargar el Overworld. Sigue ignorando items, rayos y al jugador local.
- **Advanced Recon:** muestra coordenadas del objetivo y permite marcar lo apuntado con una tecla propia.
- **JourneyMap+:** integración opcional con la generación correcta de la API de JourneyMap 1.20.1-5.10.x. Crea waypoints temporales para el último atacante y Recon, permite desactivar ambos por separado y limpiar los waypoints tácticos desde la ruleta.

## JourneyMap

El pack Jobs usa `journeymap-1.20.1-5.10.3-forge`, que implementa JourneyMap API `1.20-1.9-SNAPSHOT`. Lclient 2.1 compila contra esa generación mediante el JAR 5.10.3 y mantiene las clases de JourneyMap detrás de un bridge opcional para que Lclient también pueda iniciar sin JourneyMap instalado.

La integración anterior de Lclient 2.0 apuntaba a API 2.0.0, que no corresponde a JourneyMap 5.10.3.

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
