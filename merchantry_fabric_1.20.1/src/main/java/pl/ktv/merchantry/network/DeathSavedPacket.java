package pl.ktv.merchantry.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Merchantry;

// Informacja dla klienta z Merchantry, że śmierć uratował ładunek keepInventory - wtedy mapy
// (Xaero's Minimap, JourneyMap) nie tworzą waypointu śmierci. Klienci bez moda tego pakietu nie dostają.
public final class DeathSavedPacket {
    public static final ResourceLocation ID = new ResourceLocation(Merchantry.MOD_ID, "death_saved");

    private DeathSavedPacket() {
    }

    public static void send(ServerPlayer player) {
        if (Config.HIDE_DEATH_WAYPOINTS.get() && ServerPlayNetworking.canSend(player, ID)) {
            ServerPlayNetworking.send(player, ID, PacketByteBufs.empty());
        }
    }
}
