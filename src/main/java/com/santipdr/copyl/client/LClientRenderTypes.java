package com.santipdr.copyl.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/**
 * Lclient-owned render types.
 *
 * Loot ESP uses a dedicated color-only line pass with NO_DEPTH_TEST. This
 * means terrain depth never hides its geometry and the pass cannot mutate the
 * glow state of ItemEntity or leak global GL state into other mods.
 */
public final class LClientRenderTypes extends RenderType {
    private static final RenderType LOOT_ESP_LINES = create(
            "lclient_loot_esp_lines_xray",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1024,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(2.0D)))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );

    private LClientRenderTypes(
            String name,
            VertexFormat format,
            VertexFormat.Mode mode,
            int bufferSize,
            boolean affectsCrumbling,
            boolean sortOnUpload,
            Runnable setupState,
            Runnable clearState
    ) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        throw new IllegalStateException("Utility class");
    }

    public static RenderType lootEspLines() {
        return LOOT_ESP_LINES;
    }
}
