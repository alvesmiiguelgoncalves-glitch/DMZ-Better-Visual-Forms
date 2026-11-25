package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dmzbettervisualforms.util.FormOptions;
import com.dragonminez.client.render.effects.AuraModeState;
import com.dragonminez.client.util.SaiyanTailRules;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralUserConfig;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Atalhos: F6 Minha Aura 3D, F7 Contornos de Transformacao, F8 soltar/amarrar a cauda (F9 fica livre). Mudaveis em Controles. */
public final class BvfKeys {

    private static final String CATEGORY = "key.categories." + DmzBetterVisualForms.MOD_ID;

    public static final KeyMapping TOGGLE_AURA_3D = key("toggle_aura_3d", GLFW.GLFW_KEY_F6);
    public static final KeyMapping TOGGLE_OUTLINES = key("toggle_outlines", GLFW.GLFW_KEY_F7);
    public static final KeyMapping TOGGLE_TAIL = key("toggle_tail", GLFW.GLFW_KEY_F8);
    /** Sem tecla por padrao: o jogador escolhe em Controles. */
    public static final KeyMapping REVERT_BASE = key("revert_base", InputConstants.UNKNOWN.getValue());

    private BvfKeys() {
    }

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key." + DmzBetterVisualForms.MOD_ID + "." + name, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    @Mod.EventBusSubscriber(modid = DmzBetterVisualForms.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        private Register() {
        }

        @SubscribeEvent
        public static void onRegister(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_AURA_3D);
            event.register(TOGGLE_OUTLINES);
            event.register(TOGGLE_TAIL);
            event.register(REVERT_BASE);
        }
    }

    @Mod.EventBusSubscriber(modid = DmzBetterVisualForms.MOD_ID, value = Dist.CLIENT)
    public static final class Handler {
        private Handler() {
        }

        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;

            while (TOGGLE_AURA_3D.consumeClick()) {
                GeneralUserConfig config = ConfigManager.getUserConfig();
                boolean next = !config.getAura3DPersonal();
                config.setAura3DPersonal(next);
                AuraModeState.pushLocalPreference();
                ConfigManager.saveGeneralUserConfig();
                message(mc, "message.dmz_better_visual_forms.aura3d", next);
            }
            while (TOGGLE_OUTLINES.consumeClick()) {
                GeneralUserConfig config = ConfigManager.getUserConfig();
                boolean next = !config.getTransformationOutlines();
                config.setTransformationOutlines(next);
                ConfigManager.saveGeneralUserConfig();
                message(mc, "message.dmz_better_visual_forms.outlines", next);
            }
            while (TOGGLE_TAIL.consumeClick()) toggleTail(mc);
            while (REVERT_BASE.consumeClick()) revertToBase(mc);
        }

        /** Volta direto a forma base e tira o stack (mesma acao do clique direito no Descend do DMZ). */
        private static void revertToBase(Minecraft mc) {
            StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
            if (stats == null || stats.getCharacter() == null) return;
            if (!stats.getCharacter().hasActiveForm() && !stats.getCharacter().hasActiveStackForm()) return;
            NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.FORCE_DESCEND, true));
        }

        private static void toggleTail(Minecraft mc) {
            StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
            if (stats == null || stats.getCharacter() == null) return;
            String race = stats.getCharacter().getRaceName();
            String form = stats.getCharacter().getActiveForm();
            boolean tailRace = "saiyan".equals(race) || stats.getCharacter().isHasSaiyanTail() || (form != null && form.contains("oozaru"));
            if (!tailRace) return;
            // forma que forca a cauda (forceLooseTail / forceTiedTail): o atalho nao mexe
            if (FormOptions.tailOverride(mc.player) != null) {
                mc.player.displayClientMessage(Component.translatable("message.dmz_better_visual_forms.tail_forced"), true);
                return;
            }
            boolean wasOut = stats.getStatus().isTailVisible();
            NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.TOGGLE_TAIL));
            message(mc, "message.dmz_better_visual_forms.tail", !wasOut);
        }

        private static void message(Minecraft mc, String key, boolean on) {
            mc.player.displayClientMessage(Component.translatable(key, Component.translatable(
                    on ? "message.dmz_better_visual_forms.on" : "message.dmz_better_visual_forms.off")), true);
        }
    }
}
