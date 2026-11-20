package com.dmzbettervisualforms.client;

import com.dmzbettervisualforms.DmzBetterVisualForms;
import com.dmzbettervisualforms.SsjArmorConfig;
import com.dmzbettervisualforms.util.StatusAccess;
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
        String key = character.getActiveFormGroup() + "|" + character.getActiveForm() + "|"
                + character.getActiveStackFormGroup() + "|" + character.getActiveStackForm();

        PlayerState state = STATES.get(player.getId());
        if (state == null) {
            state = new PlayerState();
            state.key = key; // quem ja entra transformado nao dispara
            STATES.put(player.getId(), state);
        } else if (!key.equals(state.key)) {
            state.key = key;
            if (transformed) state.transformTick = now;
        }
        state.seen = now;

        boolean aura = switch (SsjArmorConfig.AURA_STATE.get()) {
            case ALWAYS -> transformed;
            case TEMPORARY -> transformed && now - state.transformTick <= SsjArmorConfig.AURA_TEMP_SECONDS.get() * 20L;
            default -> false;
        };
        StatusAccess access = StatusAccess.of(stats.getStatus());
        if (access != null && access.bvf$auraOverride() != aura) access.bvf$setAuraOverride(aura);
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
