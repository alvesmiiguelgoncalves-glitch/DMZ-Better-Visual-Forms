package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dmzbettervisualforms.SsjArmorConfig;
import com.dmzbettervisualforms.util.StatusAccess;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Acompanha, no cliente, quando cada jogador acabou de se transformar e liga o Estado de Aura visual
 * (ver {@code StatusMixin}). Tambem responde se os raios da forma devem aparecer.
 */
@Mod.EventBusSubscriber(modid = DmzBetterVisualForms.MOD_ID, value = Dist.CLIENT)
public final class VisualEvents {

    private static final class PlayerState {
        String key;
        String formGroup = "";
        String form = "";
        String stackGroup = "";
        String stack = "";
        long transformTick = Long.MIN_VALUE;
        long seen;
    }

    private static final Map<Integer, PlayerState> STATES = new HashMap<>();

    private VisualEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            STATES.clear();
            return;
        }
        long now = mc.level.getGameTime();
        for (Player player : mc.level.players()) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> update(player, stats, now));
        }
        Iterator<PlayerState> it = STATES.values().iterator();
        while (it.hasNext()) {
            if (now - it.next().seen > 200) it.remove();
        }
    }

    private static void update(Player player, StatsData stats, long now) {
        Character character = stats.getCharacter();
        if (character == null) return;

        boolean transformed = character.hasActiveForm() || character.hasActiveStackForm();
        String fg = nz(character.getActiveFormGroup());
        String f = nz(character.getActiveForm());
        String sg = nz(character.getActiveStackFormGroup());
        String st = nz(character.getActiveStackForm());
        String key = fg + "|" + f + "|" + sg + "|" + st;
        String race = nz(character.getRaceName());

        PlayerState state = STATES.get(player.getId());
        if (state == null) {
            state = new PlayerState();
            state.key = key; // quem ja entra transformado nao dispara
            STATES.put(player.getId(), state);
        } else if (!key.equals(state.key)) {
            boolean descent = isDescent(race, state, fg, f, sg, st);
            state.key = key;
            if (descent || !transformed) {
                state.transformTick = Long.MIN_VALUE; // destransformar nao liga a aura/raios temporarios
            } else {
                state.transformTick = now;
            }
        }
        state.formGroup = fg;
        state.form = f;
        state.stackGroup = sg;
        state.stack = st;
        state.seen = now;

        boolean aura = switch (SsjArmorConfig.AURA_STATE.get()) {
            case ALWAYS -> transformed;
            case TEMPORARY -> transformed && state.transformTick != Long.MIN_VALUE
                    && now - state.transformTick <= SsjArmorConfig.AURA_TEMP_SECONDS.get() * 20L;
            default -> false;
        };
        StatusAccess access = StatusAccess.of(stats.getStatus());
        if (access != null && access.bvf$auraOverride() != aura) access.bvf$setAuraOverride(aura);
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    /** Foi uma volta (forma ou stack perdida, ou para uma forma de posicao menor no mesmo grupo)? */
    private static boolean isDescent(String race, PlayerState old, String fg, String f, String sg, String st) {
        if (!old.form.isEmpty() && f.isEmpty()) return true;
        if (!old.stack.isEmpty() && st.isEmpty()) return true;
        if (!f.isEmpty() && fg.equals(old.formGroup) && !f.equals(old.form)
                && rank(race, fg, f, false) < rank(race, old.formGroup, old.form, false)) return true;
        return !st.isEmpty() && sg.equals(old.stackGroup) && !st.equals(old.stack)
                && rank(race, sg, st, true) < rank(race, old.stackGroup, old.stack, true);
    }

    private static int rank(String race, String group, String form, boolean stack) {
        try {
            FormConfig config = stack ? ConfigManager.getStackFormGroup(group) : ConfigManager.getFormGroup(race, group);
            if (config == null) return -1;
            int index = 0;
            for (FormConfig.FormData data : config.getForms().values()) {
                if (data.getName().equalsIgnoreCase(form)) return index;
                index++;
            }
        } catch (RuntimeException ignored) {
            // sem ordem conhecida
        }
        return -1;
    }

    /** Ticks desde a ultima transformacao deste jogador, ou {@link Long#MAX_VALUE}. */
    public static long ticksSinceTransform(Player player) {
        PlayerState state = player == null ? null : STATES.get(player.getId());
        Minecraft mc = Minecraft.getInstance();
        if (state == null || state.transformTick == Long.MIN_VALUE || mc.level == null) return Long.MAX_VALUE;
        return mc.level.getGameTime() - state.transformTick;
    }

    /** Os raios da forma devem aparecer para este jogador agora? */
    public static boolean raysAllowed(StatsData stats) {
        SsjArmorConfig.RayMode mode = SsjArmorConfig.RAYS.get();
        if (mode == SsjArmorConfig.RayMode.ALWAYS) return true;
        if (mode == SsjArmorConfig.RayMode.OFF) return false;
        if (stats == null) return true;

        var status = stats.getStatus();
        if (status.isChargingKi() || status.isAuraActive() || status.isForcedAura() || status.isPermanentAura()) return true;
        StatusAccess access = StatusAccess.of(status);
        if (access != null && access.bvf$auraOverride()) return true;
        return ticksSinceTransform(stats.getPlayer()) <= SsjArmorConfig.RAYS_TEMP_SECONDS.get() * 20L;
    }
}
