package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dmzbettervisualforms.SsjArmorConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Particulas de pixels da roupa enquanto ela se desfaz. O {@link ArmorBlender} avisa, a cada quadro, qual textura esta
 * se desfazendo e em que ponto da dissolucao ela esta ({@link #note}); a cada tick, se a dissolucao avancou, saem particulas
 * com as cores dessa textura, numa faixa que desce do peito para os pes junto com a roupa.
 */
@Mod.EventBusSubscriber(modid = DmzBetterVisualForms.MOD_ID, value = Dist.CLIENT)
public final class DissolveParticles {

    private static final class Note {
        ResourceLocation texture;
        float progress;
        float lastSpawned = -1.0f;
        long tick;
        boolean rising;
    }

    private static final Map<Integer, Note> NOTES = new HashMap<>();
    private static final Map<ResourceLocation, int[]> PALETTES = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();


    private DissolveParticles() {
    }

    /** Chamado pelo render (thread do cliente): {@code progress} 0 = roupa inteira, 1 = toda desfeita. */
    static void note(AbstractClientPlayer player, ResourceLocation texture, float progress) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Note note = NOTES.computeIfAbsent(player.getId(), id -> new Note());
        note.texture = texture;
        note.progress = progress;
        note.tick = mc.level.getGameTime();
    }

    static void clear() {
        NOTES.clear();
        PALETTES.clear();
    }

    /** Total aproximado de particulas por dissolucao completa (a quantidade por tick segue o avanco da dissolucao). */
    private static final float PARTICLES_PER_DISSOLVE = 220.0f;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        if (!SsjArmorConfig.ENABLED.get() || !SsjArmorConfig.DISSOLVE_PARTICLES.get()) return;

        long now = level.getGameTime();
        for (Map.Entry<Integer, Note> entry : NOTES.entrySet()) {
            Note note = entry.getValue();
            if (!(level.getEntity(entry.getKey()) instanceof Player player)) continue;

            if (now - note.tick > 2) {
                // A transicao acabou (a transformacao instantanea pode terminar de um quadro para o outro): solta o que faltou
                if (note.lastSpawned >= 0.0f && note.rising && note.lastSpawned > 0.5f && note.lastSpawned < 1.0f) {
                    emit(level, player, note, 1.0f - note.lastSpawned, 1.0f);
                }
                note.lastSpawned = -1.0f;
                note.rising = false;
                continue;
            }
            if (note.lastSpawned < 0.0f) {
                // a dissolucao comeca em 0; se o primeiro quadro ja vem perto de 1 e a roupa que esta VOLTANDO, nao ha o que soltar
                note.lastSpawned = note.progress < 0.5f ? 0.0f : note.progress;
            }
            float delta = note.progress - note.lastSpawned;
            note.lastSpawned = note.progress;
            if (delta <= 0.0f) {                // so quando a roupa esta SUMINDO (na volta ela aparece, sem particulas)
                note.rising = false;
                continue;
            }
            note.rising = true;
            emit(level, player, note, delta, note.progress);
        }
    }

    private static void emit(ClientLevel level, Player player, Note note, float delta, float progress) {
        int[] palette = palette(note.texture);
        if (palette.length == 0) return;
        int count = Math.max(1, Math.min(40, Math.round(delta * PARTICLES_PER_DISSOLVE)));
        spawn(level, player, palette, progress, count);
    }

    private static void spawn(ClientLevel level, Player player, int[] palette, float progress, int count) {
        float height = player.getBbHeight();
        float yaw = (float) Math.toRadians(player.yBodyRot);
        float sin = (float) Math.sin(yaw);
        float cos = (float) Math.cos(yaw);
        for (int i = 0; i < count; i++) {
            int rgb = palette[RANDOM.nextInt(palette.length)];
            float yFraction = 0.85f - 0.6f * progress + (RANDOM.nextFloat() - 0.3f) * 0.25f;
            double side = RANDOM.nextBoolean() ? 1.0 : -1.0;
            double localX = side * (0.1 + RANDOM.nextFloat() * 0.3);
            double localZ = (RANDOM.nextFloat() - 0.5f) * 0.3f;
            // sai para os lados (eixo X do corpo), com um pouco de variacao para frente/tras e para cima; depois cai
            double speed = 0.04 + RANDOM.nextFloat() * 0.14;
            double velX = side * speed * (0.6 + 0.4 * RANDOM.nextFloat());
            double velZ = (RANDOM.nextFloat() - 0.5f) * speed * 0.8;
            double velY = RANDOM.nextFloat() * 0.08;
            double x = player.getX() + localX * cos - localZ * sin;
            double z = player.getZ() + localX * sin + localZ * cos;
            double y = player.getY() + height * yFraction;
            Minecraft.getInstance().particleEngine.add(new PixelParticle(level, x, y, z,
                    velX * cos - velZ * sin, velY, velX * sin + velZ * cos, rgb, RANDOM));
        }
    }

    /** Cores (RGB) da textura que esta se desfazendo, tiradas da frente do tronco e dos bracos. */
    private static int[] palette(ResourceLocation texture) {
        int[] cached = PALETTES.get(texture);
        if (cached != null) return cached;
        List<Integer> colors = new ArrayList<>();
        try (NativeImage image = ArmorBlender.read(texture)) {
            int w = image.getWidth();
            int h = image.getHeight();
            // frente do tronco (20,20)-(27,31) e braco (44,20)-(47,31) num layout de 64x64
            int[][] rects = {{20, 20, 8, 12}, {44, 20, 4, 12}};
            for (int[] r : rects) {
                for (int y = r[1]; y < r[1] + r[3]; y++) {
                    for (int x = r[0]; x < r[0] + r[2]; x++) {
                        int px = image.getPixelRGBA(Math.min(w - 1, x * w / 64), Math.min(h - 1, y * h / 64));
                        if (((px >>> 24) & 0xFF) < 128) continue;
                        int red = px & 0xFF;
                        int green = (px >>> 8) & 0xFF;
                        int blue = (px >>> 16) & 0xFF;
                        colors.add((red << 16) | (green << 8) | blue);
                    }
                }
            }
        } catch (Exception e) {
            // textura ilegivel: sem particulas para ela
        }
        int[] palette = new int[colors.size()];
        for (int i = 0; i < palette.length; i++) palette[i] = colors.get(i);
        PALETTES.put(texture, palette);
        return palette;
    }
}
