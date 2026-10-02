package pl.ktv.merchantry.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Merchantry;

// Informacja dla klienta z Merchantry, że śmierć uratował ładunek keepInventory - wtedy mapy
// (Xaero's Minimap, JourneyMap) nie tworzą waypointu śmierci. Klienci bez moda tego pakietu nie dostają.
public record DeathSavedPacket() implements CustomPacketPayload {
    public static final Type<DeathSavedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Merchantry.MOD_ID, "death_saved"));
    public static final StreamCodec<ByteBuf, DeathSavedPacket> CODEC = StreamCodec.unit(new DeathSavedPacket());

    // Rejestracja typu pakietu (po obu stronach, przy starcie moda)
    public static void register() {
        PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
    }

    public static void send(ServerPlayer player) {
        if (Config.HIDE_DEATH_WAYPOINTS.get() && ServerPlayNetworking.canSend(player, TYPE)) {
            ServerPlayNetworking.send(player, new DeathSavedPacket());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
