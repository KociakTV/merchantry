package pl.ktv.merchantry.mixin.compat;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.keepinventory.KeepInventory;

// FTB Chunks po odrodzeniu wysyła klientowi waypoint miejsca śmierci (to jedyne, co robi playerCloned).
// Gdy śmierć uratował ładunek keepInventory, pomijamy go - nie ma czego szukać.
// Nakładany tylko, gdy FTB Chunks jest zainstalowany (MerchantryMixinPlugin).
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.FTBChunks", remap = false)
public abstract class FtbChunksMixin {
    @Inject(method = "playerCloned", at = @At("HEAD"), cancellable = true, require = 0)
    private void merchantry$skipDeathWaypoint(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wonGame,
                                              CallbackInfo ci) {
        if (!wonGame && Config.HIDE_DEATH_WAYPOINTS.get() && KeepInventory.isDeathSaved(oldPlayer)) {
            ci.cancel();
        }
    }
}
