package com.dmzbettervisualforms.mixin;

import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * "Voltar a forma base" (clique direito no Descend / a tecla do Better Visual Forms): o DMZ nao pede sincronizacao
 * imediata nesse caminho, entao a mudanca so aparecia no proximo sync. Aqui sincronizamos na hora, como o Descend normal.
 */
@Mixin(value = ExecuteActionC2S.class, remap = false)
public abstract class ExecuteActionC2SMixin {

    @Redirect(method = "/lambda\\$.*/", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/common/util/TransformationsHelper;revertToBaseForm(Lnet/minecraft/server/level/ServerPlayer;Lcom/dragonminez/common/stats/StatsData;Z)Z"),
            require = 0, remap = false)
    private boolean bvf$revertAndSync(ServerPlayer player, StatsData data, boolean respectEvolutionFloor) {
        boolean result = TransformationsHelper.revertToBaseForm(player, data, respectEvolutionFloor);
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        return result;
    }
}
