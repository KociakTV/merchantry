package pl.ktv.merchantry.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.ktv.merchantry.client.DeathWaypoints;

// JourneyMap: bez waypointu śmierci, gdy śmierć uratował ładunek keepInventory.
// Nakładany tylko, gdy JourneyMap jest zainstalowany (MerchantryMixinPlugin).
@Pseudo
@Mixin(targets = "journeymap.client.event.handlers.DeathPointHandler", remap = false)
public abstract class JourneyMapDeathpointMixin {
    @Inject(method = "createDeathpoint", at = @At("HEAD"), cancellable = true, require = 0)
    private void merchantry$skipDeathpoint(CallbackInfo ci) {
        if (DeathWaypoints.isDeathSaved()) {
            ci.cancel();
        }
    }
}
