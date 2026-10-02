package pl.ktv.merchantry.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.ktv.merchantry.client.DeathWaypoints;

// Xaero's Minimap / World Map: bez deathpointa, gdy śmierć uratował ładunek keepInventory.
// Nakładany tylko, gdy Xaero's Minimap jest zainstalowany (MerchantryMixinPlugin).
@Pseudo
@Mixin(targets = "xaero.hud.minimap.waypoint.DeathpointHandler", remap = false)
public abstract class XaeroDeathpointMixin {
    @Inject(method = "createDeathpoint", at = @At("HEAD"), cancellable = true, require = 0)
    private void merchantry$skipDeathpoint(CallbackInfo ci) {
        if (DeathWaypoints.isDeathSaved()) {
            ci.cancel();
        }
    }
}
