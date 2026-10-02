package pl.ktv.merchantry.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.client.DeathWaypoints;

// Informacja dla klienta z Merchantry, że śmierć uratował ładunek keepInventory - wtedy mapy
// (Xaero's Minimap, JourneyMap) nie tworzą waypointu śmierci. Klienci bez moda tego pakietu nie dostają.
public record DeathSavedPacket() implements CustomPacketPayload {
    public static final Type<DeathSavedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Merchantry.MOD_ID, "death_saved"));
    public static final StreamCodec<ByteBuf, DeathSavedPacket> CODEC = StreamCodec.unit(new DeathSavedPacket());

    // Kanał opcjonalny - gracze bez Merchantry nadal mogą wejść na serwer
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, CODEC, (payload, context) -> DeathWaypoints.markDeathSaved());
    }

    public static void send(ServerPlayer player) {
        if (Config.HIDE_DEATH_WAYPOINTS.get() && player.connection.hasChannel(TYPE)) {
            PacketDistributor.sendToPlayer(player, new DeathSavedPacket());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
