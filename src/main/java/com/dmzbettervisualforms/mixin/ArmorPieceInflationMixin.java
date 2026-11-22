package com.dmzbettervisualforms.mixin;

import com.dragonminez.client.render.util.ArmorPieceInflation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;

/**
 * Corrige a roupa "gorda" durante a transformacao/destransformacao.
 *
 * <p>O DMZ infla cada peca da roupa pelo tamanho do <b>primeiro</b> cubo do osso. Fora da transicao o osso tem um
 * cubo so; durante a mudanca de modelo ({@code ModelMorph}) o osso ganha varios cubos (os do modelo antigo e os do
 * novo), e o primeiro pode ser um cubo pequeno. A inflacao sai exagerada e a camisa fica maior que o resto da roupa.
 * Aqui, so quando o osso tem mais de um cubo, usamos o maior cubo. O resto do tempo nada muda.</p>
 */
@Mixin(value = ArmorPieceInflation.class, remap = false)
public abstract class ArmorPieceInflationMixin {

    @Inject(method = "inflateFittedPart", at = @At("HEAD"), cancellable = true)
    private static void bvf$fitDuringMorph(PoseStack poseStack, GeoBone bone, ModelPart part, ModelPart.Cube referenceCube,
                                           CallbackInfo ci) {
        if (bone.getCubes().size() < 2) return;

        GeoCube biggest = null;
        double volume = -1.0;
        for (GeoCube cube : bone.getCubes()) {
            double v = cube.size().x() * cube.size().y() * cube.size().z();
            if (v > volume) {
                volume = v;
                biggest = cube;
            }
        }
        if (biggest == null) return;

        float kx = k(biggest.size().x());
        float ky = k(biggest.size().y());
        float kz = k(biggest.size().z());
        ci.cancel();
        if (kx == 1f && ky == 1f && kz == 1f) return;

        Vector3f centre = new Vector3f(
                (referenceCube.minX + referenceCube.maxX) * 0.5f,
                (referenceCube.minY + referenceCube.maxY) * 0.5f,
                (referenceCube.minZ + referenceCube.maxZ) * 0.5f);
        if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) {
            centre.rotate(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
        }
        centre.add(part.x, part.y, part.z).div(16f);

        poseStack.translate(centre.x, centre.y, centre.z);
        poseStack.scale(kx, ky, kz);
        poseStack.translate(-centre.x, -centre.y, -centre.z);
    }

    private static float k(double sizePx) {
        return sizePx <= 0.0 ? 1f : (float) ((sizePx + 2.0 * ArmorPieceInflation.ARMOR_GAP_PX) / sizePx);
    }
}
