package com.dmzbettervisualforms.mixin;

import com.dmzbettervisualforms.client.ConfigMenuHook;
import com.dragonminez.client.gui.character.ConfigMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opcoes novas no menu de configuracoes (aba Visuais) do DragonMine Z. */
@Mixin(value = ConfigMenuScreen.class, remap = false)
public abstract class ConfigMenuScreenMixin {

    @Inject(method = "initializeConfigOptions", at = @At("TAIL"), remap = false)
    private void bvf$addOptions(CallbackInfo ci) {
        ConfigMenuHook.addOptions(this);
    }

    @Inject(method = "valueText", at = @At("HEAD"), cancellable = true, remap = false)
    private void bvf$valueText(@Coerce Object option, CallbackInfoReturnable<String> cir) {
        String text = ConfigMenuHook.valueText(option);
        if (text != null) cir.setReturnValue(text);
    }
}
