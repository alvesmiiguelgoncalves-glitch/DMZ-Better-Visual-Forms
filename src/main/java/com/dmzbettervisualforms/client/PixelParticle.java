package com.dmzbettervisualforms.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Pedacinho de roupa: de 1 a 3 pixels de textura juntos (um pixel, um "I" de 2, um "I" ou "L" de 3), na cor exata do pixel.
 * Cai devagar, balancando para os lados e girando, como as folhas/petalas do Minecraft, e pousa no chao.
 * Usa a textura lisa de um bloco branco, pintada com a cor.
 */
final class PixelParticle extends TextureSheetParticle {

    /** Formas, em celulas de 1 pixel (x para a direita, y para cima, ja centradas na hora de desenhar). */
    private static final int[][][] SHAPES = {
            {{0, 0}},                         // 1 pixel
            {{0, 0}},
            {{0, 0}},
            {{0, 0}, {0, 1}},                 // I de 2
            {{0, 0}, {0, 1}},
            {{0, 0}, {0, 1}, {1, 0}},         // L de 3
            {{0, 0}, {0, 1}, {1, 0}},
            {{0, 0}, {0, 1}, {0, 2}},         // I de 3
    };

    private final float[][] cells;
    private final float phase;
    private final float spin;

    /** @param size lado de 1 pixel em blocos (1/16 de bloco, na escala do jogador). */
    PixelParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, int rgb, float size, RandomSource random) {
        super(level, x, y, z);
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
                .getParticleIcon(Blocks.WHITE_CONCRETE.defaultBlockState());
        this.setSprite(sprite);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasPhysics = true;
        this.quadSize = size * 0.5f;           // metade do lado de uma celula
        this.lifetime = 60 + random.nextInt(50);
        this.rCol = ((rgb >> 16) & 0xFF) / 255.0f;
        this.gCol = ((rgb >> 8) & 0xFF) / 255.0f;
        this.bCol = (rgb & 0xFF) / 255.0f;

        int[][] shape = SHAPES[random.nextInt(SHAPES.length)];
        float meanX = 0, meanY = 0;
        for (int[] c : shape) {
            meanX += c[0];
            meanY += c[1];
        }
        meanX /= shape.length;
        meanY /= shape.length;
        this.cells = new float[shape.length][2];
        for (int i = 0; i < shape.length; i++) {
            this.cells[i][0] = shape[i][0] - meanX;
            this.cells[i][1] = shape[i][1] - meanY;
        }
        this.phase = random.nextFloat() * Mth.TWO_PI;
        this.spin = (random.nextBoolean() ? 1.0f : -1.0f) * (0.04f + random.nextFloat() * 0.08f);
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        if (this.onGround) {
            // pousou: fica parada no chao ate sumir
            this.xd = 0.0;
            this.zd = 0.0;
            this.yd = 0.0;
            return;
        }
        // queda de folha: afundando devagar, balancando para os lados e girando
        this.yd = Math.max(this.yd - 0.012, -0.045);
        this.xd += Math.cos(this.age * 0.28 + this.phase) * 0.005;
        this.zd += Math.sin(this.age * 0.23 + this.phase) * 0.005;
        this.roll += this.spin;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.92;
        this.zd *= 0.92;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 view = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - view.x());
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - view.y());
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - view.z());
        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));

        float size = this.getQuadSize(partialTicks);
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        for (float[] cell : this.cells) {
            float cx = cell[0] * 2.0f;
            float cy = cell[1] * 2.0f;
            Vector3f[] corners = {
                    new Vector3f(cx - 1.0f, cy - 1.0f, 0.0f),
                    new Vector3f(cx - 1.0f, cy + 1.0f, 0.0f),
                    new Vector3f(cx + 1.0f, cy + 1.0f, 0.0f),
                    new Vector3f(cx + 1.0f, cy - 1.0f, 0.0f)};
            for (Vector3f corner : corners) {
                corner.rotate(rotation).mul(size).add(px, py, pz);
            }
            buffer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
            buffer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
            buffer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
            buffer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        float life = (this.age + partialTick) / this.lifetime;
        return life > 0.85f ? this.quadSize * Math.max(0.0f, (1.0f - life) / 0.15f) : this.quadSize;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}
