package com.dmzbettervisualforms.util;

import com.dragonminez.common.config.FormConfig;

import javax.annotation.Nullable;

/** Implemented on {@code FormConfig.FormData} by {@code FormDataMixin}. All getters are null-safe. */
public interface FormOptionsAccess {

    /** Hair/eyebrow/tail tint color of this form. Empty = the form's own tintColor. */
    String dmzfo$hairTintColor();

    /** Hair/eyebrow/tail tint intensity of this form. Negative = the form's own tintIntensity. */
    double dmzfo$hairTintIntensity();

    boolean dmzfo$forceTiedTail();

    boolean dmzfo$forceLooseTail();

    /** Default true (vanilla behavior). */
    boolean dmzfo$stackTintAffectsHairAndTail();

    /** Negative = use the stack form's own intensity. */
    double dmzfo$stackTintHairAndTailIntensity();

    /** Skin tint color while a stack form is active (set on the BASE form). Empty = the stack form's own tintColor. */
    String dmzfo$stackTintColor();

    /** Skin tint intensity while a stack form is active (set on the BASE form). Negative = the stack form's own. */
    double dmzfo$stackTintIntensity();

    /** Outline primary color while a stack form is active (set on the BASE form). Empty or "#" = the stack form's own. */
    String dmzfo$stackOutlinePrimaryColor();

    /** Multiplies the STACK form's own outline thickness while it is active (set on the BASE form). 1.0 = unchanged. */
    double dmzfo$stackOutlineThicknessMultiplicator();

    /** "pulse" or "gradient" for the stack form's outline (set on the BASE form). Empty = the stack form's own. */
    String dmzfo$stackOutlineColorMode();

    /** RGB 0..1, or null when empty/invalid (= game default). */
    @Nullable
    float[] dmzfo$scleraRgb();

    boolean dmzfo$noEyebrows();

    boolean dmzfo$eyeliner();

    @Nullable
    float[] dmzfo$eyelinerRgb();

    /** Default true. */
    boolean dmzfo$chargeKiAnimation();

    /** {@code true}/{@code false} = force angular eyes on/off (DMZ Blink); {@code null} = automatic (decided by the hair type). */
    @Nullable
    Boolean dmzfo$angularEyes();

    /**
     * A cached copy of this form with another tint: {@code color} null/empty keeps the form's own tintColor,
     * {@code intensity} negative keeps the form's own tintIntensity. Null if the copy failed.
     */
    @Nullable
    FormConfig.FormData dmzfo$tintVariant(@Nullable String color, double intensity);
}
