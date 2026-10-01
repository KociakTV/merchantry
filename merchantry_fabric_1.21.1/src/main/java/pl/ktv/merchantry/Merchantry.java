package pl.ktv.merchantry;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import pl.ktv.merchantry.data.ModAttachments;
import pl.ktv.merchantry.event.PlayerEvents;
import pl.ktv.merchantry.event.ServerEvents;

// Klasa główna moda. Mod działa wyłącznie po stronie serwera - gracze nie muszą go instalować.
public class Merchantry implements ModInitializer {
    public static final String MOD_ID = "merchantry";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        Config.load();
        ModAttachments.init();
        ServerEvents.register();
        PlayerEvents.register();
    }
}
