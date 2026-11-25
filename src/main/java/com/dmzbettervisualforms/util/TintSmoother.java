package com.dmzbettervisualforms.util;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dmzbettervisualforms.SsjArmorConfig;
import com.dragonminez.common.config.FormConfig;
import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Faz o tint (pele, cabelo, sobrancelha, cauda) acompanhar a transformacao: em vez de trocar de uma vez quando a forma muda,
 * a cor e a intensidade deslizam ate o valor novo (ou ate 0, ao perder o tint).
 *
 * <p>O tint do DMZ e {@code cor*(1-i) + tint*i}, linear na cor e na intensidade; por isso deslizamos {@code tint*i} e {@code i}
 * juntos, o que equivale a misturar o resultado antigo com o novo.</p>
 *
 * <p>So um objeto reaproveitado por jogador e canal (nada de copiar o FormData a cada quadro).</p>
 */
public final class TintSmoother {

    public static final TintSmoother BODY = new TintSmoother();
    public static final TintSmoother HAIR = new TintSmoother();

    private static final Gson GSON = new Gson();

    private static final class State {
        final float[] premultiplied = new float[3];
        float intensity;
        long lastNanos;
        @Nullable FormConfig.FormData scratch;
        @Nullable FormConfig.FormData scratchSource;
    }

    private final Map<Object, State> states = new WeakHashMap<>();

    private TintSmoother() {
    }

    @Nullable
    public FormConfig.FormData smooth(@Nullable Object owner, @Nullable FormConfig.FormData target) {
        double seconds = SsjArmorConfig.TINT_FADE_SECONDS.get();
        if (owner == null || seconds <= 0.0) return target;

        float[] targetColor = target != null && target.hasTint() ? target.getRgbTintColor() : null;
        float targetIntensity = targetColor == null ? 0.0f : (float) Math.min(1.0, target.getTintIntensity());

        long now = System.nanoTime();
        State state = states.get(owner);
        if (state == null) {
            state = new State();
            for (int c = 0; c < 3; c++) state.premultiplied[c] = targetColor == null ? 0.0f : targetColor[c] * targetIntensity;
            state.intensity = targetIntensity;
            state.lastNanos = now;
            states.put(owner, state);
            return target;
        }

        float dt = Math.min(0.25f, (now - state.lastNanos) / 1.0e9f);
        state.lastNanos = now;
        // constante de tempo = segundos / 3: chega a ~95% do caminho em "segundos"
        float k = dt <= 0.0f ? 0.0f : 1.0f - (float) Math.exp(-dt * 3.0 / seconds);
        float error = Math.abs(targetIntensity - state.intensity);
        for (int c = 0; c < 3; c++) {
            float want = targetColor == null ? 0.0f : targetColor[c] * targetIntensity;
            state.premultiplied[c] += (want - state.premultiplied[c]) * k;
            error = Math.max(error, Math.abs(want - state.premultiplied[c]));
        }
        state.intensity += (targetIntensity - state.intensity) * k;

        if (error < 0.004f) {
            // chegou: devolve o proprio alvo (mantem a identidade e o comportamento normal)
            state.intensity = targetIntensity;
            for (int c = 0; c < 3; c++) state.premultiplied[c] = targetColor == null ? 0.0f : targetColor[c] * targetIntensity;
            return target;
        }
        if (state.intensity < 0.003f) return null;

        FormConfig.FormData source = target != null ? target : state.scratchSource;
        if (source == null) return target;
        if (state.scratch == null || state.scratchSource != source) {
            try {
                state.scratch = GSON.fromJson(GSON.toJsonTree(source), FormConfig.FormData.class);
                state.scratchSource = source;
            } catch (RuntimeException e) {
                DmzBetterVisualForms.LOGGER.warn("[dmz_better_visual_forms] could not copy form data for the tint transition.", e);
                return target;
            }
        }
        FormConfig.FormData out = state.scratch;
        float[] rgb = new float[3];
        for (int c = 0; c < 3; c++) rgb[c] = Math.max(0.0f, Math.min(1.0f, state.premultiplied[c] / state.intensity));
        out.setTintColor(String.format("#%02X%02X%02X", Math.round(rgb[0] * 255), Math.round(rgb[1] * 255), Math.round(rgb[2] * 255)));
        out.setTintIntensity((double) state.intensity);
        out.setRgbTintColor(rgb);
        return out;
    }
}
