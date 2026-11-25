package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/**
 * Muda SO as partes visuais das formas (cores, cabelo, olhos, aura, raios, tint, modelo...) sem mexer nos JSON do DMZ.
 *
 * <p>Fontes (a do config vence a do mod, campo a campo):
 * <ol>
 *   <li>{@code assets/dmz_better_visual_forms/visuals.json}, dentro do mod;</li>
 *   <li>{@code config/dmz_better_visual_forms-visuals.json}, relido quando muda.</li>
 * </ol>
 * Formato: {@code { "<chave da forma>": { "hairColor": "#FFD700", ... } }}, com as mesmas chaves do {@code forms.json} do addon:
 * {@code race.dragonminez.<raca>.form.<grupo>.<forma>} ou {@code stack.<grupo>.<forma>}. Pode colar o objeto inteiro da forma:
 * status, requisitos, custos, maestria etc. sao ignorados (so entra o visual).</p>
 */
@Mod.EventBusSubscriber(modid = DmzBetterVisualForms.MOD_ID, value = Dist.CLIENT)
public final class FormVisuals {

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("dmz_better_visual_forms-visuals.json");
    private static final String RESOURCE = "/assets/" + DmzBetterVisualForms.MOD_ID + "/visuals.json";
    private static final String TEMPLATE = "{\n}\n";

    /** Nao e visual: nunca e copiado, mesmo que esteja no arquivo. */
    private static final Pattern BLOCKED = Pattern.compile("(?i)^(name|formCombo|unlockOnSkillLevel|.*Multiplier|.*Drain|energyDrain|staminaDrain|healthDrain"
            + "|attackSpeed|maxMastery|masteryPer.*|passiveMastery.*|formRequisite.*|levelRequisite|sagaRequisite|unlockOnMastery|stackOnMastery"
            + "|instantTransformOnMastery|allowFreeTransformOnMastery|formStackable|incompatibleWith|shareMastery.*|triggerItemCosts|durationItemCosts"
            + "|mobEffects|otherworld.*|configVersion)$");

    private static final Gson GSON = new Gson();
    private static final Map<String, Field> FIELDS = new HashMap<>();
    private static final Map<FormConfig.FormData, Long> APPLIED = new WeakHashMap<>();

    private static Map<String, JsonObject> overrides = new HashMap<>();
    private static long stamp = 0;
    private static long fileStamp = Long.MIN_VALUE;
    private static boolean loadedOnce;

    private FormVisuals() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.tickCount % 5 != 0) return;
        try {
            reloadIfChanged();
            if (overrides.isEmpty()) return;
            for (Map.Entry<String, Map<String, FormConfig>> race : ConfigManager.getAllForms().entrySet()) {
                for (Map.Entry<String, FormConfig> group : race.getValue().entrySet()) applyGroup(group.getValue(), race.getKey(), group.getKey());
            }
            for (Map.Entry<String, FormConfig> group : ConfigManager.getAllStackForms().entrySet()) applyGroup(group.getValue(), null, group.getKey());
        } catch (RuntimeException e) {
            if (!loadedOnce) DmzBetterVisualForms.LOGGER.warn("[dmz_better_visual_forms] visuals.json: {}", e.toString());
        }
    }

    /** @param race raca da forma, ou null para stack forms. */
    private static void applyGroup(FormConfig config, String race, String group) {
        for (Map.Entry<String, FormConfig.FormData> entry : config.getForms().entrySet()) {
            FormConfig.FormData data = entry.getValue();
            if (data == null || APPLIED.getOrDefault(data, -1L) == stamp) continue;
            APPLIED.put(data, stamp);
            String base = race == null ? "stack." + group : "race.dragonminez." + race + ".form." + group;
            JsonObject merged = new JsonObject();
            boolean any = false;
            // primeiro o grupo todo (".*"), depois a forma especifica (vence)
            for (String key : new String[]{base + ".*", base + "." + entry.getKey()}) {
                JsonObject found = overrides.get(key.toLowerCase(Locale.ROOT));
                if (found == null) continue;
                any = true;
                for (Map.Entry<String, JsonElement> e : found.entrySet()) merged.add(e.getKey(), e.getValue());
            }
            if (any) apply(data, merged);
        }
    }

    private static void apply(FormConfig.FormData target, JsonObject values) {
        if (FIELDS.isEmpty()) {
            for (Field field : FormConfig.FormData.class.getDeclaredFields()) {
                int m = field.getModifiers();
                if (Modifier.isStatic(m) || Modifier.isTransient(m) || Modifier.isFinal(m) || BLOCKED.matcher(field.getName()).matches()) continue;
                field.setAccessible(true);
                FIELDS.put(field.getName().toLowerCase(Locale.ROOT), field);
            }
        }
        for (Map.Entry<String, JsonElement> e : values.entrySet()) {
            Field field = FIELDS.get(e.getKey().toLowerCase(Locale.ROOT));
            if (field == null) continue;
            try {
                field.set(target, GSON.fromJson(e.getValue(), field.getGenericType()));
            } catch (ReflectiveOperationException | RuntimeException ex) {
                DmzBetterVisualForms.LOGGER.warn("[dmz_better_visual_forms] visuals.json: valor invalido em '{}': {}", e.getKey(), ex.toString());
            }
        }
        // zera os caches de cor ja convertidos (rgbHairColor, rgbTintColor...) para o DMZ ler os valores novos
        for (Field field : FormConfig.FormData.class.getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers()) && (field.getName().startsWith("rgb") || field.getName().startsWith("dmzfo$"))) {
                try {
                    field.setAccessible(true);
                    field.set(target, null);
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
    }

    private static void reloadIfChanged() {
        long modified;
        try {
            if (!Files.exists(FILE)) Files.writeString(FILE, TEMPLATE, StandardCharsets.UTF_8);
            modified = Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException e) {
            modified = 0L;
        }
        if (loadedOnce && modified == fileStamp) return;
        fileStamp = modified;
        loadedOnce = true;

        Map<String, JsonObject> next = new HashMap<>();
        try (InputStream in = FormVisuals.class.getResourceAsStream(RESOURCE)) {
            if (in != null) merge(next, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)), "visuals.json (mod)");
        } catch (IOException | RuntimeException e) {
            DmzBetterVisualForms.LOGGER.warn("[dmz_better_visual_forms] visuals.json do mod: {}", e.toString());
        }
        try {
            merge(next, JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)), FILE.getFileName().toString());
        } catch (IOException | RuntimeException e) {
            DmzBetterVisualForms.LOGGER.warn("[dmz_better_visual_forms] {}: {}", FILE.getFileName(), e.toString());
        }
        overrides = next;
        stamp++;
        DmzBetterVisualForms.LOGGER.info("[dmz_better_visual_forms] ajustes visuais: {} forma(s).", next.size());
    }

    private static void merge(Map<String, JsonObject> into, JsonElement root, String source) {
        if (root == null || !root.isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject target = into.computeIfAbsent(entry.getKey().toLowerCase(Locale.ROOT), k -> new JsonObject());
            for (Map.Entry<String, JsonElement> field : entry.getValue().getAsJsonObject().entrySet()) target.add(field.getKey(), field.getValue());
        }
    }
}
