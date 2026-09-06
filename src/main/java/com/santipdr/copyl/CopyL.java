package com.santipdr.copyl;

import com.santipdr.copyl.client.CopyLClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(CopyL.MOD_ID)
public final class CopyL {
    public static final String MOD_ID = "copyl";

    public CopyL() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CopyLClientSetup.registerConfigScreen();
        }
    }
}
