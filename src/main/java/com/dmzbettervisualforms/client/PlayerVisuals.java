package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dmzbettervisualforms.util.FormOptionsAccess;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Cores que SO o jogador local escolhe na tela de Customizacao (tint do cabelo e cor dos olhos do SSJ4 GT).
 * Ficam em {@code config/dmz_better_visual_forms-player.json}; nao mexem nos JSON das formas nem aparecem para outros jogadores.
 * Enquanto o jogador nao escolhe nada, vale a cor do JSON da forma.
 */
public final class PlayerVisuals {

    public static final String KEY_HAIR_TINT = "ssj4gt.hairTint";
    public static final String KEY_EYE1 = "ssj4gt.eye1";
    public static final String KEY_EYE2 = "ssj4gt.eye2";

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("dmz_better_visual_forms-player.json");
    private static final Map<String, String> VALUES = new HashMap<>();
    private static final Map<String, float[]> RGB = new HashMap<>();
    private static boolean loaded;

    /** Formas "sentinela": servem para descobrir se uma funcao do DMZ pede o olho 1 ou o olho 2. */
    public static final float[] PROBE_EYE1 = new float[]{0.0f, 0.0f, 0.0f};
    public static final float[] PROBE_EYE2 = new float[]{0.0f, 0.0f, 0.0f};
    private static FormConfig.FormData probe;

    private PlayerVisuals() {
    }

    public static synchronized FormConfig.FormData probe() {
        if (probe == null) {
            probe = new FormConfig.FormData();
            probe.setRgbEye1Color(PROBE_EYE1);
            probe.setRgbEye2Color(PROBE_EYE2);
        }
        return probe;
    }

    public static boolean isSsj4Gt(@Nullable FormConfig.FormData data) {
        return data != null && "ssj4gt".equalsIgnoreCase(data.getCustomModel());
    }

    public static boolean isLocal(@Nullable StatsData stats) {
        return stats != null && stats.getPlayer() != null && stats.getPlayer() == Minecraft.getInstance().player;
    }

    /** A forma SSJ4 GT da raca (a primeira cujo modelo e "ssj4gt"), ou null. */
    @Nullable
    public static FormConfig.FormData ssj4GtForm(String race) {
        if (race == null) return null;
        for (FormConfig group : ConfigManager.getAllFormsForRace(race).values()) {
            for (FormConfig.FormData data : group.getForms().values()) {
                if (isSsj4Gt(data)) return data;
            }
        }
        return null;
    }

    /** Valor escolhido pelo jogador (hex) ou null se ele ainda nao mexeu. */
    @Nullable
    public static synchronized String custom(String key) {
        load();
        String value = VALUES.get(key);
        return value == null || value.isEmpty() ? null : value;
    }

    @Nullable
    public static synchronized float[] customRgb(String key) {
        load();
        String value = custom(key);
        if (value == null) return null;
        return RGB.computeIfAbsent(key + "=" + value, k -> {
            try {
                String hex = value.startsWith("#") ? value.substring(1) : value;
                int rgb = Integer.parseInt(hex, 16);
                return new float[]{((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f};
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }

    public static synchronized void set(String key, String hex) {
        load();
        VALUES.put(key, hex);
        save();
    }

    /** O que mostrar na caixa de cor: a escolha do jogador, senao a cor do JSON da forma. */
    public static String shown(String key, String race, String fallbackEye1) {
        String custom = custom(key);
        if (custom != null) return custom;
        FormConfig.FormData form = ssj4GtForm(race);
        String value = "";
        if (form != null) {
            switch (key) {
                case KEY_HAIR_TINT -> {
                    FormOptionsAccess access = form instanceof FormOptionsAccess a ? a : null;
                    String hair = access != null ? access.dmzfo$hairTintColor() : "";
                    value = hair != null && !hair.isEmpty() ? hair : form.getTintColor();
                }
                case KEY_EYE1 -> value = form.getEye1Color();
                case KEY_EYE2 -> value = form.getEye2Color();
                default -> {
                }
            }
        }
        return value == null || value.isEmpty() ? (fallbackEye1 != null ? fallbackEye1 : "#FFFFFF") : value;
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.exists(FILE)) return;
            JsonElement root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) return;
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().entrySet()) {
                if (e.getValue().isJsonPrimitive()) VALUES.put(e.getKey(), e.getValue().getAsString().toUpperCase(Locale.ROOT));
            }
        } catch (IOException | RuntimeException e) {
            DmzBetterVisualForms.LOGGER.warn("[dmz_better_visual_forms] {}: {}", FILE.getFileName(), e.toString());
        }
    }

    private static void save() {
        try {
            JsonObject out = new JsonObject();
            for (Map.Entry<String, String> e : VALUES.entrySet()) out.addProperty(e.getKey(), e.getValue());
            Files.writeString(FILE, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(out), StandardCharsets.UTF_8);
        } catch (IOException e) {
            DmzBetterVisualForms.LOGGER.warn("[dmz_better_visual_forms] could not save {}: {}", FILE.getFileName(), e.toString());
        }
    }
}
