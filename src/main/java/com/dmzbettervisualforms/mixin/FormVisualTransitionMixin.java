package com.dmzbettervisualforms.mixin;

import com.dmzbettervisualforms.client.PlayerVisuals;
import com.dragonminez.client.systems.FormVisualTransition;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.FormTransition;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

/**
 * Cor dos olhos do SSJ4 GT escolhida pelo proprio jogador (tela de Customizacao). So vale para o jogador local
 * e so enquanto ele esta nessa forma: o JSON e os outros jogadores nao mudam.
 */
@Mixin(value = FormVisualTransition.class, remap = false)
public abstract class FormVisualTransitionMixin {

    @Inject(method = "color", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bvf$playerEyes(StatsData stats, float[] base, Function<FormConfig.FormData, float[]> rgb, CallbackInfoReturnable<float[]> cir) {
        try {
            if (!PlayerVisuals.isLocal(stats)) return;
            if (PlayerVisuals.custom(PlayerVisuals.KEY_EYE1) == null && PlayerVisuals.custom(PlayerVisuals.KEY_EYE2) == null) return;
            float[] probed = rgb.apply(PlayerVisuals.probe());
            String key = probed == PlayerVisuals.PROBE_EYE1 ? PlayerVisuals.KEY_EYE1 : probed == PlayerVisuals.PROBE_EYE2 ? PlayerVisuals.KEY_EYE2 : null;
            float[] custom = key == null ? null : PlayerVisuals.customRgb(key);
            if (custom == null) return;

            Function<FormConfig.FormData, float[]> wrapped = data -> PlayerVisuals.isSsj4Gt(data) ? custom : rgb.apply(data);
            float[] out = FormTransition.Look.current(stats.getCharacter()).overlay(base, wrapped);
            FormTransition.Charge charge = FormVisualTransition.charge(stats);
            if (charge != null) out = FormTransition.lerp(out, charge.look().overlay(base, wrapped), charge.factor());
            FormTransition.Revert revert = FormVisualTransition.revert(stats);
            if (revert != null) out = FormTransition.lerp(out, revert.look().overlay(base, wrapped), revert.factor());
            cir.setReturnValue(out);
        } catch (RuntimeException ignored) {
            // em caso de erro, o DMZ segue com o comportamento normal
        }
    }
}
