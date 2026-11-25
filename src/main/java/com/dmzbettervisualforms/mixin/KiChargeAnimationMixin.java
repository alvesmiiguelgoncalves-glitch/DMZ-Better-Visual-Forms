package com.dmzbettervisualforms.mixin;

import com.dmzbettervisualforms.util.FormOptions;
import com.dragonminez.client.animation.BaseAnimations;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * chargeKiAnimation: false (idle) or "overcharge" (base.ki_overcharge). DMZ's own mixin adds {@code predicate(...)} to AbstractClientPlayer; in it,
 * charging ki plays {@code BaseAnimations.kiChargeFor(race)}. For forms with the option off we hand back
 * the idle animation instead, so the player just stands there while the aura charges.
 *
 * <p>Higher priority than DMZ's mixin so ours is applied after it and can see its method.</p>
 */
@Mixin(value = AbstractClientPlayer.class, priority = 1500)
public abstract class KiChargeAnimationMixin {

    @Redirect(method = "predicate", remap = false,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lcom/dragonminez/client/animation/BaseAnimations;kiChargeFor(Ljava/lang/String;)Lsoftware/bernie/geckolib/core/animation/RawAnimation;"))
    private RawAnimation dmzfo$kiCharge(String race) {
        String mode = FormOptions.kiChargeMode((AbstractClientPlayer) (Object) this);
        if ("none".equals(mode)) return BaseAnimations.IDLE;
        if ("overcharge".equals(mode)) return BaseAnimations.KI_OVERCHARGE;
        return BaseAnimations.kiChargeFor(race);
    }
}
