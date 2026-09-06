# Lclient

Lclient es un cliente modular **100% client-side** para Minecraft Forge 1.20.1.

## Diseño

Todo se controla desde una única ruleta. CopyL es un módulo más del mismo cliente, no un menú/mod separado.

La tecla de apertura de la ruleta **no se registra en Opciones > Controles**. Se cambia desde `Mods > Lclient > Config`. Las teclas internas de CopyL y Recon se editan desde la propia ruleta.

## Lclient 2.3.0

La 2.3 elimina módulos que habían terminado generando ruido o duplicando funciones del pack. Ya no existen Sound Radar, Combat Log, centro de notificaciones ni Entity Alerts.

Los cinco módulos actuales son:

- **CopyL / Mensajes rápidos:** 10 mensajes o comandos con teclas propias, configurados dentro de la ruleta.
- **Loot ESP:** dejó de depender del glow de Minecraft. Lclient renderiza cajas propias sin depth-test alrededor de `ItemEntity`, por lo que son visibles a través de bloques. Permite ajustar alcance y stack mínimo.
- **Smart Offhand:** mueve automáticamente una comida adecuada a la offhand cuando baja el hambre y restaura el objeto anterior sólo si el slot de respaldo sigue siendo seguro. No pisa cambios manuales y no toca la offhand si hay un objeto siendo arrastrado por el menú.
- **Advanced Recon:** zoom táctico render-only, sin modificar el FOV guardado del juego. Mantén la tecla de zoom y usa la rueda del mouse: arriba aumenta el zoom y abajo lo reduce. El raycast de Recon alcanza hasta la distancia configurada y compara bloques/entidades, de modo que el waypoint se crea realmente donde miras y no en el reach vanilla. El Target Panel sólo aparece mientras Recon está haciendo zoom.
- **JourneyMap+:** integración opcional con JourneyMap 1.20.1-5.10.x / API 1.9. Recon puede crear un waypoint temporal en el objetivo del raycast y limpiarlo desde la ruleta.

## Recon

Teclas por defecto:

- Zoom: `C` (mantener pulsado).
- Waypoint: `V` (sólo mientras el zoom está activo).

Mientras el zoom está activo:

1. La rueda del mouse cambia la magnificación sin mover la hotbar.
2. El HUD muestra el zoom actual, FOV, alcance, coordenadas y distancia del objetivo.
3. Si apuntas a una entidad, aparece el Target Panel con nombre, tipo, posición, distancia y vida conocida.
4. El waypoint usa un raycast client-side largo, no `minecraft.hitResult`/reach vanilla.

El zoom se aplica mediante el evento de render de FOV de Forge, por lo que el valor de Video Settings no se sobrescribe ni necesita restauración posterior.

## JourneyMap

El pack Jobs usa `journeymap-1.20.1-5.10.3-forge`, que implementa JourneyMap API `1.20-1.9-SNAPSHOT`. Lclient compila contra esa generación mediante el JAR 5.10.3 y mantiene las clases de JourneyMap detrás de un bridge opcional para que el resto del cliente pueda iniciar aunque JourneyMap no esté instalado.

## Compatibilidad

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- JourneyMap 1.20.1-5.10.x para JourneyMap+
- JourneyMap es opcional

## Build

```bash
./gradlew build
```

El workflow de GitHub Actions compila el JAR y publica el payload de despliegue desde `main`.
