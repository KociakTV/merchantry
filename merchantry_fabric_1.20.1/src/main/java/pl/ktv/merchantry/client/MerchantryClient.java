package pl.ktv.merchantry.client;

import net.fabricmc.api.ClientModInitializer;

// Opcjonalna część klienta - zwykli gracze nie muszą instalować Merchantry
public class MerchantryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DeathWaypoints.register();
    }
}
