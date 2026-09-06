package com.santipdr.copyl.client.integration;

import com.santipdr.copyl.CopyL;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.DisplayType;
import journeymap.client.api.display.Waypoint;
import journeymap.client.api.event.ClientEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * JourneyMap 5.10.x / API 1.9 plugin used only for Lclient Recon waypoints.
 *
 * We never retain a JourneyMap Waypoint object across mapping sessions. Only
 * immutable waypoint data is cached so DISPLAY_UPDATE can rebuild the current
 * transient marker when JourneyMap refreshes its display registry.
 */
@journeymap.client.api.ClientPlugin
public final class LClientJourneyMapPlugin implements IClientPlugin {
    private static final String RECON_ID = "lclient_recon";
    private static final long PROBE_INTERVAL_MS = 1000L;

    private static IClientAPI api;
    private static boolean mappingActive;
    private static boolean acceptsWaypoints;
    private static long nextProbeAt;
    private static String lastError = "";
    private static String lastLabel = "";
    private static BlockPos lastPosition;
    private static ResourceKey<Level> lastDimension;

    @Override
    public void initialize(IClientAPI clientAPI) {
        api = clientAPI;
        mappingActive = false;
        acceptsWaypoints = false;
        nextProbeAt = 0L;
        lastError = "";
        clearWaypointData();

        try {
            api.subscribe(
                    getModId(),
                    EnumSet.of(
                            ClientEvent.Type.DISPLAY_UPDATE,
                            ClientEvent.Type.MAPPING_STARTED,
                            ClientEvent.Type.MAPPING_STOPPED
                    )
            );
            probeState(true);
        } catch (Throwable throwable) {
            recordError("No se pudo inicializar la integración", throwable);
        }
    }

    @Override
    public String getModId() {
        return CopyL.MOD_ID;
    }

    @Override
    public void onEvent(ClientEvent event) {
        if (event == null) return;
        try {
            if (event.type == ClientEvent.Type.MAPPING_STARTED) {
                mappingActive = true;
                if (refreshAcceptance()) lastError = "";
                nextProbeAt = System.currentTimeMillis() + PROBE_INTERVAL_MS;
            } else if (event.type == ClientEvent.Type.MAPPING_STOPPED) {
                // JourneyMap owns and clears its session display registry here.
                mappingActive = false;
                acceptsWaypoints = false;
                clearWaypointData();
                nextProbeAt = 0L;
            } else if (event.type == ClientEvent.Type.DISPLAY_UPDATE) {
                // Non-persistent displayables are expected to be re-published
                // when JourneyMap asks plugins to refresh a dimension.
                if (mappingActive
                        && acceptsWaypoints
                        && lastPosition != null
                        && lastDimension != null
                        && lastDimension.equals(event.dimension)) {
                    showReconWaypoint(lastPosition, lastLabel, lastDimension, false);
                }
            }
        } catch (Throwable throwable) {
            recordError("Error procesando el estado de JourneyMap", throwable);
        }
    }

    public static boolean isReady() {
        if (api == null) return false;
        probeState(false);
        return mappingActive && acceptsWaypoints;
    }

    /** Shows/replaces the single Recon waypoint using a stable display id. */
    public static boolean markRecon(BlockPos pos, String label, ResourceKey<Level> dimension) {
        if (pos == null || dimension == null || !isReady()) return false;
        return showReconWaypoint(pos.immutable(), sanitizeLabel(label), dimension, true);
    }

    private static boolean showReconWaypoint(
            BlockPos pos,
            String safeLabel,
            ResourceKey<Level> dimension,
            boolean remember
    ) {
        try {
            IClientAPI current = api;
            if (current == null) return false;

            Waypoint waypoint = new Waypoint(
                    CopyL.MOD_ID,
                    RECON_ID,
                    "Recon · " + safeLabel,
                    dimension,
                    pos
            ).setColor(0x5AAFFF).setPersistent(false).setEditable(false);

            current.show(waypoint);
            if (remember) {
                lastPosition = pos.immutable();
                lastDimension = dimension;
                lastLabel = safeLabel;
            }
            lastError = "";
            return true;
        } catch (Throwable throwable) {
            recordError("JourneyMap rechazó el waypoint", throwable);
            return false;
        }
    }

    public static boolean clearTacticalWaypoints() {
        if (api == null) {
            clearWaypointData();
            return false;
        }
        try {
            api.removeAll(CopyL.MOD_ID, DisplayType.Waypoint);
            clearWaypointData();
            lastError = "";
            return true;
        } catch (Throwable throwable) {
            clearWaypointData();
            recordError("No se pudo limpiar el waypoint", throwable);
            return false;
        }
    }

    public static String getStatusText() {
        if (api == null) return "JourneyMap API aún no inicializada";
        probeState(false);
        if (!lastError.isBlank()) return lastError;
        if (!mappingActive) return "JourneyMap no está mapeando este mundo";
        if (!acceptsWaypoints) return "JourneyMap no acepta waypoints de Lclient";
        return lastLabel.isBlank() ? "JourneyMap conectado" : "Waypoint activo · " + lastLabel;
    }

    private static void probeState(boolean force) {
        IClientAPI current = api;
        if (current == null) return;

        long now = System.currentTimeMillis();
        if (!force && now < nextProbeAt) return;
        nextProbeAt = now + PROBE_INTERVAL_MS;

        try {
            // API 1.9 documents getDataPath() as non-null only while mapping.
            mappingActive = current.getDataPath(CopyL.MOD_ID) != null;
            if (mappingActive) {
                if (refreshAcceptance()) lastError = "";
            } else {
                acceptsWaypoints = false;
                lastError = "";
            }
        } catch (Throwable throwable) {
            mappingActive = false;
            acceptsWaypoints = false;
            recordError("JourneyMap no respondió correctamente", throwable);
        }
    }

    /**
     * @return true when the API call itself succeeded, regardless of whether
     * the player currently accepts this display type.
     */
    private static boolean refreshAcceptance() {
        try {
            acceptsWaypoints = api != null && api.playerAccepts(CopyL.MOD_ID, DisplayType.Waypoint);
            return true;
        } catch (Throwable throwable) {
            acceptsWaypoints = false;
            recordError("No se pudo consultar permiso de waypoints", throwable);
            return false;
        }
    }

    private static void clearWaypointData() {
        lastLabel = "";
        lastPosition = null;
        lastDimension = null;
    }

    private static String sanitizeLabel(String label) {
        String safeLabel = label == null || label.isBlank() ? "Recon" : label.trim();
        safeLabel = safeLabel.replace('\n', ' ').replace('\r', ' ');
        if (safeLabel.length() > 42) safeLabel = safeLabel.substring(0, 42);
        return safeLabel;
    }

    private static void recordError(String context, Throwable throwable) {
        String detail = throwable == null || throwable.getMessage() == null
                ? ""
                : throwable.getMessage().trim();
        lastError = detail.isBlank() ? context : context + ": " + detail;
        if (lastError.length() > 120) lastError = lastError.substring(0, 120);
    }
}
