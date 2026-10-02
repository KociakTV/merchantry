package pl.ktv.merchantry;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import pl.ktv.merchantry.compat.Compat;
import pl.ktv.merchantry.compat.graves.GravesCompat;
import pl.ktv.merchantry.compat.ultimine.UltimineCompat;
import pl.ktv.merchantry.data.ModAttachments;
import pl.ktv.merchantry.network.DeathSavedPacket;

// Klasa główna moda. Mod działa wyłącznie po stronie serwera - gracze nie muszą go instalować.
@Mod(Merchantry.MOD_ID)
public class Merchantry {
    public static final String MOD_ID = "merchantry";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Merchantry(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        modEventBus.addListener(DeathSavedPacket::register);
        if (Compat.FTB_ULTIMINE.isLoaded()) {
            UltimineCompat.register();
        }
        GravesCompat.register();
    }
}
