package com.dmzbettervisualforms.util;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dragonminez.client.render.layer.AuraTintTracker;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.systems.FormVisualTransition;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.FormTransition;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Shared logic for the mixins. Not a mixin itself, so it can be called from anywhere. */
public final class FormOptions {

    /** Guards {@code SaiyanTailRulesMixin} against calling itself. */
    public static boolean tailReentry;

    /** Set per player render by {@code SkinLayerTintMixin}; read by the Saiyan tail redirect. */
    @Nullable public static FormConfig.FormData bodyTint;
    @Nullable public static FormConfig.FormData hairTailTint;
    public static boolean tintContextValid;

    private FormOptions() {
    }

    @Nullable
    public static FormOptionsAccess of(@Nullable FormConfig.FormData data) {
        return data instanceof FormOptionsAccess access ? access : null;
    }

    /** Active stack form first (it is the more recent choice), then the base form. */
    public static List<FormOptionsAccess> active(@Nullable Character character) {
        List<FormOptionsAccess> out = new ArrayList<>(2);
        if (character == null) return out;
        if (character.hasActiveStackForm()) {
            FormOptionsAccess stack = of(character.getActiveStackFormData());
            if (stack != null) out.add(stack);
        }
        if (character.hasActiveForm()) {
            FormOptionsAccess form = of(character.getActiveFormData());
            if (form != null) out.add(form);
        }
        return out;
    }

    @Nullable
    public static Character characterOf(@Nullable Entity entity) {
        if (entity == null) return null;
        StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, entity).orElse(null);
        return stats == null ? null : stats.getCharacter();
    }

    // ------------------------------------------------------------------ tail

    /** @return TRUE = force loose tail, FALSE = force tied tail, null = leave DMZ alone. */
    @Nullable
    public static Boolean tailOverride(@Nullable Entity entity) {
        List<FormOptionsAccess> forms = active(characterOf(entity));
        List<FormOptionsAccess> moving = formsDuringTransition(entity);
        if (moving != null) forms = moving;
        for (FormOptionsAccess form : forms) if (form.dmzfo$forceLooseTail()) return Boolean.TRUE;
        for (FormOptionsAccess form : forms) if (form.dmzfo$forceTiedTail()) return Boolean.FALSE;
        return null;
    }

    /**
     * Enquanto o jogador transforma (lenta ou instantanea), a cauda passa a seguir a forma de destino quando a transicao
     * passa da metade (e, ao voltar, a forma anterior ate a metade), para ela soltar/amarrar junto com a roupa e o corpo.
     *
     * @return as formas que valem agora, ou {@code null} se nao ha transicao em andamento
     */
    @Nullable
    private static List<FormOptionsAccess> formsDuringTransition(@Nullable Entity entity) {
        try {
            if (!(entity instanceof net.minecraft.world.entity.player.Player)) return null;
            StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, entity).orElse(null);
            if (stats == null) return null;
            com.dragonminez.common.stats.FormTransition.Charge charge = com.dragonminez.client.systems.FormVisualTransition.charge(stats);
            if (charge != null) {
                return charge.factor() >= 0.5f ? lookForms(charge.look()) : null;
            }
            com.dragonminez.common.stats.FormTransition.Revert revert = com.dragonminez.client.systems.FormVisualTransition.revert(stats);
            if (revert != null && revert.factor() > 0.5f) return lookForms(revert.look());
        } catch (RuntimeException | LinkageError e) {
            // sem transicao legivel: vale a forma ativa
        }
        return null;
    }

    private static List<FormOptionsAccess> lookForms(com.dragonminez.common.stats.FormTransition.Look look) {
        List<FormOptionsAccess> out = new ArrayList<>(2);
        FormOptionsAccess stack = of(look.stackData());
        if (stack != null) out.add(stack);
        FormOptionsAccess form = of(look.formData());
        if (form != null) out.add(form);
        return out;
    }

    // ------------------------------------------------------------------ eyes

    /** Eye options for the current forms, or null when nothing is set (DMZ draws its own eyes). */
    @Nullable
    public static EyeOptions eyes(@Nullable Character character) {
        float[] sclera = null;
        float[] liner = null;
        boolean noBrows = false;
        for (FormOptionsAccess form : active(character)) {
            if (sclera == null) sclera = form.dmzfo$scleraRgb();
            if (liner == null && form.dmzfo$eyeliner()) liner = form.dmzfo$eyelinerRgb();
            noBrows |= form.dmzfo$noEyebrows();
        }
        if (sclera == null && liner == null && !noBrows) return null;
        return new EyeOptions(sclera, liner, noBrows);
    }

    public record EyeOptions(@Nullable float[] sclera, @Nullable float[] eyeliner, boolean noEyebrows) {
    }

    // ------------------------------------------------------------------ Kaio-ken tint on hair / tail

    /**
     * The tint form DMZ should use for hair and tail. DMZ's own choice is a stack form (Kaio-ken)
     * when one is active, otherwise the base form. When a stack form is active, the BASE form can say:
     * {@code stackTintAffectsHairAndTail: false} (no tint on hair/tail) or
     * {@code stackTintHairAndTailIntensity: 0.1} (weaker tint on hair/tail). The color is always the stack form's own.
     */
    @Nullable
    public static FormConfig.FormData hairTailTint(StatsData stats, @Nullable FormConfig.FormData original) {
        Character character = stats == null ? null : stats.getCharacter();
        return character == null ? original : hairTailTint(FormTransition.Look.current(character), original);
    }

    @Nullable
    public static FormConfig.FormData hairTailTint(FormTransition.Look look, @Nullable FormConfig.FormData original) {
        FormConfig.FormData result = stackHairTailTint(look, original);
        if (result == null && original != null) return null; // a regra do stack ja tirou o tint
        return ownHairTint(look, original, result);
    }

    /** O mesmo que {@code DMZSkinLayer.resolveTintForm}, mas para um visual (Look) qualquer, nao so o atual. */
    @Nullable
    public static FormConfig.FormData tintFormOf(FormTransition.Look look) {
        if (look.hasStack() && look.stackData() != null && look.stackData().hasTint()) return look.stackData();
        if (look.hasForm() && look.formData() != null && look.formData().hasTint()) return look.formData();
        return null;
    }

    /**
     * Tint do corpo e do cabelo/cauda, ja misturados com o progresso da transformacao do DMZ: na carga (lenta ou instantanea)
     * vai do tint atual para o do visual alvo, e ao voltar, do tint do visual anterior para o atual. Sem transformacao
     * em andamento (ou no menu de pre-visualizacao) devolve os objetos normais, sem copiar nada.
     */
    public static TintBlend.Result tints(StatsData stats) {
        Character character = stats.getCharacter();
        FormTransition.Look now = FormTransition.Look.current(character);
        FormConfig.FormData original = DMZSkinLayer.resolveTintForm(stats);
        FormConfig.FormData body = bodyTintForm(stats, original);
        FormConfig.FormData hair = hairTailTint(stats, original);
        if (DMZSkinLayer.PREVIEW_MODE) return new TintBlend.Result(body, hair);

        FormTransition.Charge charge = FormVisualTransition.charge(stats);
        FormTransition.Revert revert = FormVisualTransition.revert(stats);
        if (charge == null && revert == null) return new TintBlend.Result(body, hair);

        List<TintBlend.Step> steps = new ArrayList<>(2);
        if (charge != null) steps.add(new TintBlend.Step(charge.look(), charge.factor()));
        if (revert != null) steps.add(new TintBlend.Step(revert.look(), revert.factor()));
        return TintBlend.blend(stats, body, hair, steps);
    }

    /**
     * {@code hairTintColor} / {@code hairTintIntensity} na PROPRIA forma (a de stack, se houver uma ativa, senao a base):
     * mudam so o tint do cabelo, sobrancelha e cauda. Vazio / -1 = o tint da propria forma.
     */
    @Nullable
    private static FormConfig.FormData ownHairTint(FormTransition.Look look, @Nullable FormConfig.FormData original, @Nullable FormConfig.FormData current) {
        try {
            FormConfig.FormData source = look.hasStack() ? look.stackData() : look.hasForm() ? look.formData() : null;
            FormOptionsAccess own = of(source);
            if (own == null) return current;
            String color = own.dmzfo$hairTintColor();
            double intensity = own.dmzfo$hairTintIntensity();
            if (color.isEmpty() && intensity < 0.0) return current;
            if (intensity == 0.0) return null;
            FormConfig.FormData base = current != null ? current : source;
            FormOptionsAccess access = of(base);
            if (access == null) return current;
            FormConfig.FormData variant = access.dmzfo$tintVariant(color.isEmpty() ? null : color, intensity >= 0.0 ? Math.min(1.0, intensity) : -1.0);
            return variant != null ? variant : current;
        } catch (RuntimeException e) {
            return current;
        }
    }

    @Nullable
    private static FormConfig.FormData stackHairTailTint(FormTransition.Look look, @Nullable FormConfig.FormData original) {
        try {
            if (original == null) return original;
            if (!look.hasStack() || look.stackData() != original) return original;
            if (!look.hasForm()) return original;

            FormOptionsAccess base = of(look.formData());
            if (base == null) return original;
            if (!base.dmzfo$stackTintAffectsHairAndTail()) return null;

            double intensity = base.dmzfo$stackTintHairAndTailIntensity();
            if (intensity < 0.0) return original;
            if (intensity == 0.0) return null;

            FormOptionsAccess stack = of(original);
            if (stack == null) return original;
            FormConfig.FormData copy = stack.dmzfo$tintVariant(null, Math.min(1.0, intensity));
            return copy != null ? copy : original;
        } catch (RuntimeException e) {
            return original;
        }
    }

    /**
     * The tint form DMZ should use for the SKIN (and everything else that is not hair/tail/eyebrows) while a stack
     * form is active. The BASE form can say {@code stackTintColor} and/or {@code stackTintIntensity}; with both
     * unset ({@code ""} and {@code -1}) DMZ keeps the stack form's own tint.
     */
    @Nullable
    public static FormConfig.FormData bodyTintForm(@Nullable StatsData stats, @Nullable FormConfig.FormData original) {
        Character character = stats == null ? null : stats.getCharacter();
        return character == null ? original : bodyTintForm(FormTransition.Look.current(character), original);
    }

    @Nullable
    public static FormConfig.FormData bodyTintForm(FormTransition.Look look, @Nullable FormConfig.FormData original) {
        try {
            if (!look.hasStack() || !look.hasForm()) return original;

            FormOptionsAccess stack = of(look.stackData());
            FormOptionsAccess base = of(look.formData());
            if (stack == null || base == null) return original;

            String color = base.dmzfo$stackTintColor();
            double intensity = base.dmzfo$stackTintIntensity();
            boolean hasColor = !color.isEmpty();
            boolean hasIntensity = intensity >= 0.0;
            if (!hasColor && !hasIntensity) return original;
            if (hasIntensity && intensity == 0.0) return null;

            FormConfig.FormData variant = stack.dmzfo$tintVariant(hasColor ? color : null, hasIntensity ? Math.min(1.0, intensity) : -1.0);
            return variant != null && variant.hasTint() ? variant : original;
        } catch (RuntimeException e) {
            return original;
        }
    }

    private static boolean loggedHook;

    /** Used by the redirects in the hair and race-parts renderers (call replaces {@code resolveTintForm}). */
    @Nullable
    public static FormConfig.FormData resolveHairTailTint(StatsData stats) {
        FormConfig.FormData original = DMZSkinLayer.resolveTintForm(stats);
        FormConfig.FormData result = tints(stats).hair();
        if (result != original && !loggedHook) {
            loggedHook = true;
            DmzBetterVisualForms.LOGGER.info("[dmz_better_visual_forms] hair/tail tint override is active (intensity {}).",
                    result == null ? "off" : String.valueOf(result.getTintIntensity()));
        }
        return result;
    }

    /**
     * Same math as DMZ's {@code applyColorTint}, but with the hair/tail tint. Returns the input array
     * untouched when the form removes the tint; a tinted copy otherwise; null when nothing special applies
     * (the caller then runs DMZ's own code).
     */
    @Nullable
    public static float[] tintWithHairTail(float[] rgb) {
        if (!tintContextValid || hairTailTint == bodyTint || rgb == null || rgb.length < 3) return null;
        FormConfig.FormData wanted = hairTailTint;
        if (wanted == null) return rgb;
        float[] color = wanted.getRgbTintColor();
        float intensity = (float) wanted.getTintIntensity();
        if (color == null || intensity <= 0.0f) return rgb;
        float scaled = Math.min(1.0f, intensity) * AuraTintTracker.darkTintScale(rgb);
        float[] out = rgb.clone();
        for (int i = 0; i < 3; i++) {
            out[i] = Math.max(0.0f, Math.min(1.0f, rgb[i] * (1.0f - scaled) + color[i] * scaled));
        }
        return out;
    }

    // ------------------------------------------------------------------ per-strand hair colors

    /** Tint form chosen by the last hair resolve (client render thread); null = no strand tint. */
    private static FormConfig.FormData strandTint;

    public static void setStrandTint(@Nullable FormConfig.FormData form) {
        strandTint = form;
    }

    /** Tints a packed 0xRRGGBB color taken from a strand that has its own color. */
    public static int tintStrandColor(int rgb) {
        FormConfig.FormData form = strandTint;
        if (form == null || rgb < 0) return rgb;
        float[] color = form.getRgbTintColor();
        float intensity = (float) form.getTintIntensity();
        if (color == null || intensity <= 0.0f) return rgb;
        float[] c = {((rgb >> 16) & 0xFF) / 255.0f, ((rgb >> 8) & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f};
        float scaled = Math.min(1.0f, intensity) * AuraTintTracker.darkTintScale(c);
        int out = 0;
        for (int i = 0; i < 3; i++) {
            float v = Math.max(0.0f, Math.min(1.0f, c[i] * (1.0f - scaled) + color[i] * scaled));
            out = (out << 8) | Math.round(v * 255.0f);
        }
        return out;
    }

    // ------------------------------------------------------------------ ki charge animation

    /** "none" if any active form turns the pose off; else "overcharge" if one asks for it; else "default". */
    public static String kiChargeMode(@Nullable Entity entity) {
        String result = "default";
        for (FormOptionsAccess form : active(characterOf(entity))) {
            String mode = form.dmzfo$chargeKiMode();
            if ("none".equals(mode)) return "none";
            if ("overcharge".equals(mode)) result = mode;
        }
        return result;
    }

    public static boolean suppressKiChargeAnimation(@Nullable Entity entity) {
        for (FormOptionsAccess form : active(characterOf(entity))) {
            if (!form.dmzfo$chargeKiAnimation()) return true;
        }
        return false;
    }
}
