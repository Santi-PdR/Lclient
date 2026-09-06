# Lclient

Lclient es un cliente modular client-side para Minecraft Forge 1.20.1.

## Diseño

Todo se controla desde una única ruleta. CopyL ya no es un menú/mod aislado: es uno de los módulos del anillo junto a las demás ventajas.

La tecla de apertura de la ruleta **no se registra en Opciones > Controles**. Se cambia desde `Mods > Lclient > Config`. Las teclas internas de CopyL y Recon también se guardan en la configuración propia de Lclient y se editan desde la ruleta.

## Módulos

- **CopyL / Mensajes rápidos:** 10 mensajes o comandos con teclas propias, configurados dentro de la ruleta.
- **Sound Radar:** muestra dirección, tipo aproximado y distancia de sonidos relevantes recibidos por el cliente.
- **Loot ESP:** resalta con glow los `ItemEntity` cargados dentro del radio configurado.
- **Combat + Notificaciones:** panel del objetivo bajo la mira, historial de golpes con entidad y coordenadas, y centro de avisos.
- **Smart Offhand:** cuando baja el hambre intercambia temporalmente una comida del inventario con la offhand y, al recuperarse, devuelve el objeto anterior.
- **Entity Alerts:** avisa cuando aparece una entidad cercana; ignora items, rayos y al jugador local.
- **Advanced Recon:** muestra coordenadas del objetivo y permite marcar lo apuntado con una tecla propia.
- **JourneyMap+:** crea waypoints temporales para el último atacante y para marcas de Recon cuando JourneyMap está disponible.

## Compatibilidad

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- JourneyMap es opcional; Lclient funciona sin él.

## Build

```bash
./gradlew build
```

El workflow de GitHub Actions compila el JAR y publica el payload de despliegue desde `main`.
