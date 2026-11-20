package com.dmzbettervisualforms.client;

import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.systems.FormVisualTransition;
import com.dragonminez.common.stats.FormTransition;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.AbstractClientPlayer;

import javax.annotation.Nullable;

/**
 * Progresso da troca de roupa de cada jogador, ligado ao progresso da transformacao do proprio DMZ ({@link FormTransition}):
 * <ul>
 *   <li>transformacao lenta (segurando G): a roupa muda junto com a barra de carga, nao so no fim;</li>
 *   <li>transformacao instantanea e voltar ao normal: o DMZ faz uma transicao curta para o visual anterior, que seguimos.</li>
 * </ul>
 * Nao guarda estado: tudo vem do que o DMZ ja calculou para o quadro atual.
 */
public final class FadeTracker {

    /** Par de perfis e quanto do caminho de {@code from} para {@code to} ja foi feito (0..1). */
    public static final class Frame {
        @Nullable public String from;
        @Nullable public String to;
        public float progress = 1.0f;
    }

    private FadeTracker() {
    }

    public static Frame frame(AbstractClientPlayer player) {
        Frame frame = new Frame();
        StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null || stats.getCharacter() == null) return frame;

        String current = FormMatcher.profileFor(stats, FormTransition.Look.current(stats.getCharacter()));
        frame.from = frame.to = current;
        if (DMZSkinLayer.PREVIEW_MODE) return frame;

        FormTransition.Charge charge = FormVisualTransition.charge(stats);
        if (charge != null) {
            String target = FormMatcher.profileFor(stats, charge.look());
            if (!java.util.Objects.equals(target, current)) {
                frame.to = target;
                frame.progress = clamp(charge.factor());
                return frame;
            }
        }

        FormTransition.Revert revert = FormVisualTransition.revert(stats);
        if (revert != null) {
            String previous = FormMatcher.profileFor(stats, revert.look());
            if (!java.util.Objects.equals(previous, current)) {
                // factor vai de 1 (visual de antes) ate 0 (visual atual)
                frame.to = previous;
                frame.progress = clamp(revert.factor());
            }
        }
        return frame;
    }

    public static void clear() {
        // sem estado proprio
    }

    private static float clamp(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
