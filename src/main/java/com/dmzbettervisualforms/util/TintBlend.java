package com.dmzbettervisualforms.util;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.FormTransition;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Mistura o tint (pele, cabelo, sobrancelha, cauda) com o progresso da transformacao do DMZ, do mesmo jeito que o DMZ ja faz com as
 * cores do corpo: {@code lerp(atual, alvo, fator)}.
 *
 * <p>O tint do DMZ e {@code cor*(1-i) + tint*i}, linear na cor e na intensidade; por isso misturamos {@code tint*i} e {@code i} juntos,
 * o que equivale a misturar o resultado de cada um. Usa um unico objeto reaproveitado por jogador e canal.</p>
 */
public final class TintBlend {

    public record Result(@Nullable FormConfig.FormData body, @Nullable FormConfig.FormData hair) {
    }

    /** Um passo da mistura: o visual (Look) com o qual misturar e quanto (0..1). */
    public record Step(FormTransition.Look look, float factor) {
    }

    private static final class Value {
        final float[] pc = new float[3];
        float i;
        @Nullable FormConfig.FormData source;

        static Value of(@Nullable FormConfig.FormData data) {
            Value v = new Value();
            if (data != null && data.hasTint()) {
                float[] rgb = data.getRgbTintColor();
                v.i = (float) Math.min(1.0, data.getTintIntensity());
                for (int c = 0; c < 3; c++) v.pc[c] = rgb[c] * v.i;
                v.source = data;
            }
            return v;
        }

        void mix(Value other, float f) {
            for (int c = 0; c < 3; c++) pc[c] += (other.pc[c] - pc[c]) * f;
            i += (other.i - i) * f;
            if (other.source != null && f >= 0.5f) source = other.source;
            else if (source == null) source = other.source;
        }
    }

    private static final Gson GSON = new Gson();
    private static final Map<Object, FormConfig.FormData[]> SCRATCH = new WeakHashMap<>();

    private TintBlend() {
    }

    public static Result blend(StatsData stats, @Nullable FormConfig.FormData body, @Nullable FormConfig.FormData hair, List<Step> steps) {
        Value bodyValue = Value.of(body);
        Value hairValue = Value.of(hair);
        for (Step step : steps) {
            FormTransition.Look look = step.look();
            FormConfig.FormData original = FormOptions.tintFormOf(look);
            float f = Math.max(0.0f, Math.min(1.0f, step.factor()));
            bodyValue.mix(Value.of(FormOptions.bodyTintForm(look, original)), f);
            hairValue.mix(Value.of(FormOptions.hairTailTint(look, original)), f);
        }
        FormConfig.FormData[] scratch = SCRATCH.computeIfAbsent(stats, key -> new FormConfig.FormData[2]);
        return new Result(toData(bodyValue, scratch, 0), toData(hairValue, scratch, 1));
    }

    @Nullable
    private static FormConfig.FormData toData(Value v, FormConfig.FormData[] scratch, int slot) {
        if (v.i < 0.003f || v.source == null) return null;
        FormConfig.FormData out = scratch[slot];
        if (out == null || scratch.length < 2 || sourceChanged(out, v.source)) {
            try {
                out = GSON.fromJson(GSON.toJsonTree(v.source), FormConfig.FormData.class);
                scratch[slot] = out;
            } catch (RuntimeException e) {
                DmzBetterVisualForms.LOGGER.warn("[dmz_better_visual_forms] could not copy form data for the tint transition.", e);
                return v.source;
            }
        }
        float[] rgb = new float[3];
        for (int c = 0; c < 3; c++) rgb[c] = Math.max(0.0f, Math.min(1.0f, v.pc[c] / v.i));
        out.setTintColor(String.format("#%02X%02X%02X", Math.round(rgb[0] * 255), Math.round(rgb[1] * 255), Math.round(rgb[2] * 255)));
        out.setTintIntensity((double) v.i);
        out.setRgbTintColor(rgb);
        return out;
    }

    private static boolean sourceChanged(FormConfig.FormData copy, FormConfig.FormData source) {
        return !copy.getName().equals(source.getName());
    }
}
