package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dmzbettervisualforms.SsjArmorConfig;
import net.minecraft.client.resources.language.I18n;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Coloca "Estado de Aura ao Transformar" e "Raios das Transformacoes" no menu Visuais do DragonMine Z.
 * As classes internas do menu (Category, ConfigType, ConfigOption) sao privadas, entao tudo aqui e por reflexao;
 * se o DMZ mudar esses nomes, as opcoes simplesmente nao aparecem (o resto do mod segue valendo, e o toml continua valendo).
 */
public final class ConfigMenuHook {

    public static final String KEY_AURA = "config.bvfAuraState";
    public static final String KEY_RAYS = "config.bvfRays";

    private static boolean failedOnce;

    private ConfigMenuHook() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addOptions(Object screen) {
        try {
            Class<?> screenClass = screen.getClass();
            ClassLoader loader = screenClass.getClassLoader();
            Class<?> categoryClass = Class.forName(screenClass.getName() + "$Category", false, loader);
            Class<?> typeClass = Class.forName(screenClass.getName() + "$ConfigType", false, loader);
            Object visuals = Enum.valueOf((Class) categoryClass, "VISUALS");
            Object typeInt = Enum.valueOf((Class) typeClass, "INT");
            Method number = screenClass.getDeclaredMethod("number", categoryClass, String.class, typeClass,
                    float.class, float.class, float.class, Consumer.class);
            number.setAccessible(true);

            Consumer<Float> auraSetter = v -> {
                SsjArmorConfig.AURA_STATE.set(SsjArmorConfig.AuraStateMode.values()[clamp(v, 2)]);
                SsjArmorConfig.save();
            };
            Consumer<Float> raysSetter = v -> {
                SsjArmorConfig.RAYS.set(SsjArmorConfig.RayMode.values()[clamp(v, 2)]);
                SsjArmorConfig.save();
            };
            number.invoke(screen, visuals, KEY_AURA, typeInt, (float) SsjArmorConfig.AURA_STATE.get().ordinal(), 0.0f, 2.0f, auraSetter);
            number.invoke(screen, visuals, KEY_RAYS, typeInt, (float) SsjArmorConfig.RAYS.get().ordinal(), 0.0f, 2.0f, raysSetter);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!failedOnce) {
                failedOnce = true;
                DmzBetterVisualForms.LOGGER.warn("[dmz_better_visual_forms] nao foi possivel adicionar as opcoes ao menu Visuais do DMZ (use o arquivo dmz_better_visual_forms-client.toml).", e);
            }
        }
    }

    /** Texto do valor de uma das nossas opcoes, ou {@code null} se a opcao nao e nossa. */
    public static String valueText(Object option) {
        try {
            Field keyField = option.getClass().getDeclaredField("key");
            keyField.setAccessible(true);
            String key = (String) keyField.get(option);
            if (!KEY_AURA.equals(key) && !KEY_RAYS.equals(key)) return null;
            Field valueField = option.getClass().getDeclaredField("value");
            valueField.setAccessible(true);
            int value = clamp(valueField.getFloat(option), 2);
            String[] names = KEY_AURA.equals(key) ? new String[]{"disabled", "temporary", "always"} : new String[]{"off", "reduced", "always"};
            return I18n.get("gui.dragonminez.config.bvf." + (KEY_AURA.equals(key) ? "aura." : "rays.") + names[value]);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static int clamp(float v, int max) {
        return Math.max(0, Math.min(max, Math.round(v)));
    }
}
