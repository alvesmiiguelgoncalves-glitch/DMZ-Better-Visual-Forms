package com.dmzbettervisualforms.mixin;

import com.dmzbettervisualforms.client.VisualEvents;
import com.dragonminez.client.render.effects.AuraFxState;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Raios das transformacoes: Desativado / Reduzido / Sempre Ativo. Mexe so nos raios que vem da forma (os do surto e da reserva ficam). */
@Mixin(value = AuraFxState.class, remap = false)
public abstract class AuraFxStateMixin {

    @Inject(method = "formLightning", at = @At("RETURN"), cancellable = true, remap = false)
    private static void bvf$rays(StatsData stats, CallbackInfoReturnable<FormConfig.FormData> cir) {
        if (cir.getReturnValue() == null) return;
        if (!VisualEvents.raysAllowed(stats)) cir.setReturnValue(null);
    }
}
