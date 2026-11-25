package com.dmzbettervisualforms;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Configuracao do cliente: config/dmz_better_visual_forms-client.toml.
 * As formas que trocam de roupa ficam em dmz_better_visual_forms-forms.json.
 * As opcoes de Estado de Aura e Raios tambem aparecem no menu Visuais do DragonMine Z.
 */
public final class SsjArmorConfig {

    /** Estado de Aura ao Transformar. */
    public enum AuraStateMode {
        DISABLED, TEMPORARY, ALWAYS;

        public AuraStateMode next(int delta) {
            AuraStateMode[] all = values();
            return all[Math.floorMod(ordinal() + delta, all.length)];
        }
    }

    /** Raios das transformacoes. */
    public enum RayMode {
        OFF, REDUCED, ALWAYS;

        public RayMode next(int delta) {
            RayMode[] all = values();
            return all[Math.floorMod(ordinal() + delta, all.length)];
        }
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue FADE_SLEEVES;
    public static final ForgeConfigSpec.BooleanValue DISSOLVE_PARTICLES;
    public static final ForgeConfigSpec.IntValue DISSOLVE_PARTICLE_COUNT;
    public static final ForgeConfigSpec.EnumValue<AuraStateMode> AURA_STATE;
    public static final ForgeConfigSpec.IntValue AURA_TEMP_SECONDS;
    public static final ForgeConfigSpec.EnumValue<RayMode> RAYS;
    public static final ForgeConfigSpec.IntValue RAYS_TEMP_SECONDS;
    public static final ForgeConfigSpec.BooleanValue OOZARU_TEAR_CLOTHES;
    public static final ForgeConfigSpec.DoubleValue TINT_FADE_SECONDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> OOZARU_KEEP_CLOTHES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");

        ENABLED = builder
                .comment("Liga/desliga a troca de textura das roupas por forma.")
                .define("enabled", true);

        FADE_SLEEVES = builder
                .comment("SSJ4 (GT e Daima): se a pasta de texturas da forma nao tiver a manga, ela some junto com a transformacao em vez de sumir de uma vez no fim.")
                .define("fadeSleeves", true);

        DISSOLVE_PARTICLES = builder
                .comment("Solta particulas de 1 a 3 pixels (cor exata, caem como folhas) da roupa enquanto ela se desfaz na transformacao.")
                .define("dissolveParticles", true);

        DISSOLVE_PARTICLE_COUNT = builder
                .comment("Quantas particulas saem, no maximo, em cada dissolucao completa da roupa.")
                .defineInRange("dissolveParticleCount", 40, 1, 1000);

        AURA_STATE = builder
                .comment("Estado de Aura ao Transformar.",
                        "DISABLED = usa o do proprio jogo; TEMPORARY = aura por alguns segundos depois de transformar;",
                        "ALWAYS = Estado de Aura do jogo ligado enquanto estiver transformado.")
                .defineEnum("auraState", AuraStateMode.DISABLED);

        AURA_TEMP_SECONDS = builder
                .comment("Quanto tempo (segundos) a aura fica no modo TEMPORARY.")
                .defineInRange("auraStateSeconds", 10, 1, 120);

        RAYS = builder
                .comment("Raios das transformacoes.",
                        "OFF = nenhum raio de transformacao; REDUCED = so ao transformar (alguns segundos), ao usar a aura de ki ou com o Estado de Aura ligado;",
                        "ALWAYS = igual ao DMZ original.")
                .defineEnum("rays", RayMode.ALWAYS);

        RAYS_TEMP_SECONDS = builder
                .comment("Quanto tempo (segundos) os raios aparecem depois de transformar no modo REDUCED.")
                .defineInRange("raysSeconds", 5, 1, 120);

        TINT_FADE_SECONDS = builder
                .comment("Tempo (segundos) que o tint da pele, cabelo, sobrancelha e cauda leva para deslizar de uma forma para outra.",
                        "0 = desligado (o tint troca de uma vez, como no DMZ).")
                .defineInRange("tintFadeSeconds", 1.0, 0.0, 5.0);

        OOZARU_TEAR_CLOTHES = builder
                .comment("Ao virar Oozaru a roupa inteira se rasga (dissolve + particulas), menos as armaduras saiyajins.")
                .define("oozaruTearClothes", true);

        OOZARU_KEEP_CLOTHES = builder
                .comment("Roupas que NAO se rasgam no Oozaru (armaduras saiyajins). Basta o nome aparecer no nome da roupa,",
                        "ex.: \"bardock\" vale para bardock_armor e bardockdbs_armor.")
                .defineListAllowEmpty("oozaruKeepClothes", List.of("bardock", "broly", "gine", "king_vegeta", "raditz", "turles",
                        "vegeta_saiyan", "vegetaz_armor", "vegetanamek", "vegetasuper"), o -> o instanceof String);

        builder.pop();
        SPEC = builder.build();
    }

    private SsjArmorConfig() {
    }

    /** Salva no disco depois de mudar um valor pelo menu. */
    public static void save() {
        try {
            SPEC.save();
        } catch (RuntimeException ignored) {
            // config ainda nao carregada: o valor fica so em memoria
        }
    }
}
