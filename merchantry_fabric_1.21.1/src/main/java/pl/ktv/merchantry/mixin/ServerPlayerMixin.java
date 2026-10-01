package pl.ktv.merchantry.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.ktv.merchantry.event.PlayerEvents;

// Śmierć gracza (w NeoForge: LivingDeathEvent). Początek die() - totem już nie uratował gracza,
// a przedmioty jeszcze nie wypadły, więc keepInventory może zapamiętać ekwipunek.
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void merchantry$onDeath(DamageSource source, CallbackInfo ci) {
        PlayerEvents.onDeath((ServerPlayer) (Object) this);
    }
}
