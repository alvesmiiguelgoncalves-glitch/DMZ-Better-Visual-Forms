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
            if (now - note.tick > 2) {          // parou a transicao (nao e mais desenhado com mistura)
                note.lastSpawned = -1.0f;
                continue;
            }
            if (note.progress <= 0.0f || note.progress >= 1.0f) {
                note.lastSpawned = note.progress;
                continue;
            }
            if (note.lastSpawned < 0.0f) note.lastSpawned = note.progress;
            float delta = note.progress - note.lastSpawned;
            note.lastSpawned = note.progress;
            if (delta <= 0.0f) continue;        // so quando a roupa esta SUMINDO (na volta ela aparece, sem particulas)

            if (!(level.getEntity(entry.getKey()) instanceof Player player)) continue;
            int[] palette = palette(note.texture);
            if (palette.length == 0) continue;

            int count = Math.max(1, Math.min(10, Math.round(delta * 80.0f)));
            spawn(level, player, palette, note.progress, count);
        }
    }

    private static void spawn(ClientLevel level, Player player, int[] palette, float progress, int count) {
        float height = player.getBbHeight();
        float yaw = (float) Math.toRadians(player.yBodyRot);
        float sin = (float) Math.sin(yaw);
        float cos = (float) Math.cos(yaw);
        for (int i = 0; i < count; i++) {
            int rgb = palette[RANDOM.nextInt(palette.length)];
            float yFraction = 0.85f - 0.6f * progress + (RANDOM.nextFloat() - 0.3f) * 0.18f;
            double localX = (RANDOM.nextFloat() - 0.5f) * 0.6f;
            double localZ = (RANDOM.nextFloat() - 0.5f) * 0.3f;
            double x = player.getX() + localX * cos - localZ * sin;
            double z = player.getZ() + localX * sin + localZ * cos;
            double y = player.getY() + height * yFraction;
            double outward = 0.02 + RANDOM.nextFloat() * 0.03;
            Minecraft.getInstance().particleEngine.add(new PixelParticle(level, x, y, z,
                    (localX * cos - localZ * sin) * outward * 3.0, 0.0, (localX * sin + localZ * cos) * outward * 3.0,
                    rgb, RANDOM));
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
