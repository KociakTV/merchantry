package pl.ktv.merchantry.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import pl.ktv.merchantry.network.DeathSavedPacket;

// Opcjonalna część klienta: serwer z Merchantry daje znać, że śmierć uratował ładunek keepInventory.
// Do odrodzenia mapy (Xaero's Minimap, JourneyMap) nie tworzą wtedy waypointu śmierci.
public final class DeathWaypoints {
    // Ustawiane w wątku sieci, zanim klient przetworzy pakiet śmierci - stąd volatile
    private static volatile boolean deathSaved;

    private DeathWaypoints() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DeathSavedPacket.ID,
                (client, handler, buf, responseSender) -> deathSaved = true);
    }

    public static boolean isDeathSaved() {
        return deathSaved;
    }

    // Po odrodzeniu (wywoływane z ClientPacketListenerMixin)
    public static void onRespawn() {
        deathSaved = false;
    }
}
