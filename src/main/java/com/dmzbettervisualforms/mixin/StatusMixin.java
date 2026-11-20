package com.dmzbettervisualforms.mixin;

import com.dmzbettervisualforms.util.StatusAccess;
import com.dragonminez.common.stats.character.Status;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Estado de Aura ao Transformar: o cliente liga uma bandeira visual neste {@code Status} e, so na thread de render,
 * {@code isPermanentAura()} responde {@code true}. O servidor nunca ve isso (outro objeto, outra thread), entao nada de
 * gameplay muda: so o desenho da aura, dos raios, do cabelo e do som.
 */
@Mixin(value = Status.class, remap = false)
public abstract class StatusMixin implements StatusAccess {

    @Unique private boolean bvf$auraOverride;

    @Override
    public boolean bvf$auraOverride() {
        return bvf$auraOverride;
    }

    @Override
    public void bvf$setAuraOverride(boolean value) {
        bvf$auraOverride = value;
    }

    @Inject(method = "isPermanentAura", at = @At("HEAD"), cancellable = true, remap = false)
    private void bvf$visualAura(CallbackInfoReturnable<Boolean> cir) {
        if (!bvf$auraOverride || !RenderSystem.isOnRenderThread()) return;
        net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
        // nos menus do DMZ o botao do Estado de Aura mostra o estado real, nao o visual
        if (screen != null && screen.getClass().getName().startsWith("com.dragonminez.client.gui")) return;
        cir.setReturnValue(Boolean.TRUE);
    }
}
