package pl.ktv.merchantry.client;

// Opcjonalna część klienta: serwer z Merchantry daje znać, że śmierć uratował ładunek keepInventory.
// Do odrodzenia mapy (Xaero's Minimap, JourneyMap) nie tworzą wtedy waypointu śmierci.
// Bez klas klienta Minecrafta - klasę ładuje też serwer (rejestracja pakietu).
public final class DeathWaypoints {
    private static volatile boolean deathSaved;

    private DeathWaypoints() {
    }

    public static void markDeathSaved() {
        deathSaved = true;
    }

    public static boolean isDeathSaved() {
        return deathSaved;
    }

    // Po odrodzeniu (wywoływane z ClientPacketListenerMixin)
    public static void onRespawn() {
        deathSaved = false;
    }
}
