package pl.ktv.merchantry.compat.ultimine;

import dev.ftb.mods.ftbultimine.FTBUltimine;
import net.minecraft.server.level.ServerPlayer;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.unlock.Unlock;

// FTB Ultimine: prawo do używania ultimine kupowane w sklepie (odblokowanie "ultimine").
// Oficjalne API FTB Ultimine - serwer po prostu nie wykona ultimine graczowi bez odblokowania.
public final class UltimineCompat {
    private UltimineCompat() {
    }

    public static void register() {
        FTBUltimine.setPermissionOverride(player -> !Config.ULTIMINE_REQUIRES_UNLOCK.get()
                || !(player instanceof ServerPlayer serverPlayer)
                || Unlock.ULTIMINE.has(serverPlayer));
    }
}
