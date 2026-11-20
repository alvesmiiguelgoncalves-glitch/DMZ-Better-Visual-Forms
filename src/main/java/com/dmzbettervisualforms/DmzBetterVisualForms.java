package com.dmzbettervisualforms;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * DMZ: Better Visual Forms. Addon do DragonMine Z que junta o antigo "SSJ4 Armors" (roupa por forma, com transicao)
 * e o antigo "Form Options" (opcoes novas nos JSON das formas), mais atalhos e opcoes visuais de aura e raios.
 * Quase tudo e feito com mixins (ver pacote {@code mixin}).
 */
@Mod(DmzBetterVisualForms.MOD_ID)
public class DmzBetterVisualForms {

    public static final String MOD_ID = "dmz_better_visual_forms";
    /** Namespace das texturas de roupa por forma. Continua o do antigo SSJ4 Armors para os resource packs ja feitos seguirem funcionando. */
    public static final String TEXTURE_NAMESPACE = "dmz_ssj4_armors";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DmzBetterVisualForms() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SsjArmorConfig.SPEC, MOD_ID + "-client.toml");
    }
}
