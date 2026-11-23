package com.dmzbettervisualforms.mixin;

import com.dmzbettervisualforms.util.FormOptionsAccess;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.character.Character;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Integration with the "DMZ Blink" (DMZ Face Animations) mod, only when it is installed.
 *
 * <p>Blink gives angular eyes when the active form uses an SSJ hair ({@code ssj, ssj2, ssj3, ssj4}). A form can now say
 * {@code "angularEyes": true} (e.g. Kaioken, MUI, which use the base hair) or {@code "angularEyes": false} (e.g. a mastered SSJ)
 * in its JSON. {@code "auto"} (the default) keeps Blink's own rule. With a stack form, the stack form's setting wins over the
 * base form's.</p>
 */
@Pseudo
@Mixin(targets = "com.gustavo.dmzblink.BlinkFrames", remap = false)
public abstract class BlinkAngularEyesMixin {

    @Inject(method = "isSuperSaiyan", at = @At("HEAD"), cancellable = true, require = 0)
    private static void bvf$angularEyes(Character character, CallbackInfoReturnable<Boolean> cir) {
        if (character == null) return;
        try {
            Boolean forced = null;
            if (character.hasActiveStackForm()) forced = option(character.getActiveStackFormData());
            if (forced == null && character.hasActiveForm()) forced = option(character.getActiveFormData());
            if (forced != null) cir.setReturnValue(forced);
        } catch (RuntimeException ignored) {
            // on any doubt, Blink keeps its own rule
        }
    }

    private static Boolean option(FormConfig.FormData data) {
        return data == null ? null : ((FormOptionsAccess) (Object) data).dmzfo$angularEyes();
    }
}
