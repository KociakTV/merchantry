package pl.ktv.merchantry.mixin;

import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.event.PlayerEvents;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    // Śmierć gracza (w NeoForge: LivingDeathEvent). Początek die() - totem już nie uratował gracza,
    // a przedmioty jeszcze nie wypadły, więc keepInventory może zapamiętać ekwipunek.
    @Inject(method = "die", at = @At("HEAD"))
    private void merchantry$onDeath(DamageSource source, CallbackInfo ci) {
        PlayerEvents.onDeath((ServerPlayer) (Object) this);
    }

    // Ustawienia klienta (m.in. język gry) - w 1.20.1 ServerPlayer nie zapamiętuje języka
    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void merchantry$onClientOptions(ServerboundClientInformationPacket packet, CallbackInfo ci) {
        Lang.setClientLanguage((ServerPlayer) (Object) this, packet.language());
    }
}
