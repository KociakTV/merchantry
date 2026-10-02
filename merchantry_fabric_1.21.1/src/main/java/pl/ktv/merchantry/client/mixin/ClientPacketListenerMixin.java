package pl.ktv.merchantry.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.ktv.merchantry.client.DeathWaypoints;

// Odrodzenie gracza - koniec okresu, w którym mapy nie tworzą waypointu śmierci
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void merchantry$onRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        DeathWaypoints.onRespawn();
    }
}
