package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dmzbettervisualforms.SsjArmorConfig;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.ActionMode;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Volta direto a forma base (e tira o stack), pulando as formas do meio. Usa a mesma acao do clique direito no Descend do DMZ.
 * <ul>
 *   <li>Tecla "Return to Base Form" em Controles (sem tecla por padrao, escolha a sua, ex.: Alt+H).</li>
 *   <li>Toque duplo na tecla de destransformar (Descend) do DMZ, ligado/desligado na config ({@code revertDoubleTap}).</li>
 * </ul>
 */
public final class BaseFormKey {

    public static final KeyMapping REVERT_BASE = new KeyMapping(
            "key.bvf.revert_base",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.dragonminez");

    private BaseFormKey() {
    }

    @Mod.EventBusSubscriber(modid = DmzBetterVisualForms.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onRegister(RegisterKeyMappingsEvent event) {
            event.register(REVERT_BASE);
        }
    }

    @Mod.EventBusSubscriber(modid = DmzBetterVisualForms.MOD_ID, value = Dist.CLIENT)
    public static final class Tick {
        private static boolean wasDown;
        private static long lastTap;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            // tecla propria
            while (REVERT_BASE.consumeClick()) {
                if (mc.screen == null) revert(mc);
            }

            // toque duplo no Descend do DMZ
            boolean down = mc.screen == null && KeyBinds.isChordDown(KeyBinds.DESCEND);
            if (down && !wasDown && SsjArmorConfig.REVERT_DOUBLE_TAP.get()) {
                long now = System.currentTimeMillis();
                if (now - lastTap <= SsjArmorConfig.REVERT_DOUBLE_TAP_MS.get()) {
                    revert(mc);
                    lastTap = 0L;
                } else {
                    lastTap = now;
                }
            }
            wasDown = down;
        }

        private static void revert(Minecraft mc) {
            StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(stats -> {
                Character character = stats.getCharacter();
                if (character == null || !(character.hasActiveForm() || character.hasActiveStackForm())) return;
                ActionMode mode = stats.getStatus().getSelectedAction();
                // o toque duplo so vale nos modos em que o Descend destransforma (Forma/Stack), como no DMZ
                if (mode != ActionMode.FORM && mode != ActionMode.STACK) return;
                NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.FORCE_DESCEND, true));
            });
        }
    }
}
