package com.dmzbettervisualforms.mixin;

import com.dragonminez.client.render.layer.DMZPlayerArmorLayer;
import com.dragonminez.client.render.util.ArmorPieceInflation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

/**
 * Roupa "gorda"/deslocada durante a transformacao e a destransformacao.
 *
 * <p>O GeckoLib ({@code ItemArmorGeoLayer.prepModelPartForRender}) dimensiona a caixa da roupa pelo tamanho <b>declarado</b>
 * do primeiro cubo do osso e a pendura no pivo do osso. Durante a mudanca de modelo do DMZ ({@code ModelMorph}) os vertices
 * dos cubos mudam de tamanho e de altura, mas o tamanho declarado continua o do modelo final, e o corpo buffed tem dois cubos
 * (peitoral e abdomen) contra um so na forma base. Resultado: a camisa fica no peitoral ou na barriga e o corpo aparece por cima.</p>
 *
 * <p>Aqui, so quando o tamanho declarado do primeiro cubo nao bate com os vertices dele (ou seja, so durante a mudanca),
 * a caixa da roupa e encaixada nos limites reais de todos os cubos do osso. O resto do tempo, nada muda.</p>
 */
@Mixin(value = DMZPlayerArmorLayer.class, remap = false)
public abstract class PlayerArmorFitMixin {

    @Shadow
    protected abstract ModelPart.Cube getReferenceCubeForModel(GeoBone bone, ModelPart part);

    @Inject(method = "prepModelPartForRender", at = @At("HEAD"), cancellable = true)
    private void bvf$fitToRealBounds(PoseStack poseStack, GeoBone bone, ModelPart part, CallbackInfo ci) {
        if (bone.getCubes().isEmpty()) return;

        GeoCube first = bone.getCubes().get(0);
        float[] firstBounds = bounds(java.util.List.of(first));
        if (firstBounds == null) return;
        boolean stale = Math.abs((firstBounds[3] - firstBounds[0]) - first.size().x()) > 0.05
                || Math.abs((firstBounds[4] - firstBounds[1]) - first.size().y()) > 0.05
                || Math.abs((firstBounds[5] - firstBounds[2]) - first.size().z()) > 0.05;
        if (!stale) return;

        float[] b = bounds(bone.getCubes());
        ModelPart.Cube ref = getReferenceCubeForModel(bone, part);
        if (b == null || ref == null) return;

        float width = b[3] - b[0];
        float height = b[4] - b[1];
        float depth = b[5] - b[2];
        float refWidth = Math.abs(ref.maxX - ref.minX);
        float refHeight = Math.abs(ref.maxY - ref.minY);
        float refDepth = Math.abs(ref.maxZ - ref.minZ);
        if (width < 0.01f || height < 0.01f || depth < 0.01f || refWidth < 0.01f || refHeight < 0.01f || refDepth < 0.01f) return;

        float sx = width / refWidth;
        float sy = height / refHeight;
        float sz = depth / refDepth;
        float centreX = (b[0] + b[3]) * 0.5f;
        float centreZ = (b[2] + b[5]) * 0.5f;
        float refCentreX = (ref.minX + ref.maxX) * 0.5f;
        float refCentreZ = (ref.minZ + ref.maxZ) * 0.5f;
        float refTop = Math.min(ref.minY, ref.maxY);

        // mesma convencao do GeckoLib (x e y espelhados), mas pendurando a caixa nos limites reais, nao no pivo
        part.setPos(-centreX / sx - refCentreX, -b[4] / sy - refTop, centreZ / sz - refCentreZ);
        part.xRot = -bone.getRotX();
        part.yRot = -bone.getRotY();
        part.zRot = bone.getRotZ();
        poseStack.scale(sx, sy, sz);

        // folga da roupa sobre o corpo, igual a do DMZ (ver ArmorPieceInflation.inflateFittedPart), pelo tamanho real
        float kx = inflation(width);
        float ky = inflation(height);
        float kz = inflation(depth);
        if (kx != 1f || ky != 1f || kz != 1f) {
            Vector3f centre = new Vector3f(refCentreX, (ref.minY + ref.maxY) * 0.5f, refCentreZ);
            if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) {
                centre.rotate(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
            }
            centre.add(part.x, part.y, part.z).div(16f);
            poseStack.translate(centre.x, centre.y, centre.z);
            poseStack.scale(kx, ky, kz);
            poseStack.translate(-centre.x, -centre.y, -centre.z);
        }
        ci.cancel();
    }

    private static float inflation(float sizePx) {
        return sizePx <= 0.0f ? 1f : (sizePx + 2.0f * ArmorPieceInflation.ARMOR_GAP_PX) / sizePx;
    }

    /** {minX, minY, minZ, maxX, maxY, maxZ} em pixels, pelos vertices atuais dos cubos; {@code null} se nao ha vertices. */
    private static float[] bounds(java.util.List<GeoCube> cubes) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        boolean any = false;
        for (GeoCube cube : cubes) {
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) continue;
                for (GeoVertex vertex : quad.vertices()) {
                    Vector3f p = vertex.position();
                    minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
                    minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
                    minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
                    any = true;
                }
            }
        }
        return any ? new float[]{minX * 16f, minY * 16f, minZ * 16f, maxX * 16f, maxY * 16f, maxZ * 16f} : null;
    }
}
