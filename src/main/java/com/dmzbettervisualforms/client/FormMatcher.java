package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.FormsConfig;
import com.dragonminez.common.stats.FormTransition;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;

import javax.annotation.Nullable;

/** Decide qual conjunto de roupas ("perfil") vale para uma forma. */
public final class FormMatcher {

    public static final String SSJ4_GT = "ssj4gt";
    public static final String SSJ4_DAIMA = "ssj4d";

    private FormMatcher() {
    }

    /**
     * @return {@code null} = roupa original do DMZ; senao o nome da pasta de texturas dentro de {@code textures/armor/}
     *         ({@code ssj4gt}, {@code ssj4d} ou o nome escolhido no JSON das formas).
     */
    @Nullable
    public static String profileFor(StatsData stats, FormTransition.Look look) {
        Character character = stats.getCharacter();
        if (character == null || look == null) return null;

        // 1) Lista de formas do config (chaves no padrao do lang do DMZ).
        String form = look.form();
        if (form != null && !form.isEmpty()) {
            FormsConfig.Entry listed = FormsConfig.lookup(safe(character.getRaceName()), safe(look.formGroup()), form);
            if (listed != null) {
                if (!listed.enabled()) return null;
                return listed.profile().isEmpty() ? bySsj4Model(look) : listed.profile();
            }
        }

        // 2) Forma que nao esta na lista: o SSJ4 e detectado pelo modelo ("ssj4gt" e "ssj4d", mesmo criterio da pelagem do DMZ).
        return bySsj4Model(look);
    }

    /** {@code ssj4d} / {@code ssj4gt} se a forma usa um desses modelos; senao {@code null}. */
    @Nullable
    private static String bySsj4Model(FormTransition.Look look) {
        String model = look.customModel();
        if (model.contains("ssj4d")) return SSJ4_DAIMA;
        if (model.contains("ssj4gt")) return SSJ4_GT;
        return null;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
