package pl.ktv.merchantry.compat.graves;

import com.b1n_ry.yigd.events.AllowGraveGenerationEvent;
import eu.pb4.graves.event.PlayerGraveCreationEvent;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.compat.Compat;
import pl.ktv.merchantry.keepinventory.KeepInventory;

// Mody grobów: gdy śmierć uratował ładunek keepInventory, grób nie powstaje (ekwipunek i tak jest pusty,
// a pusty grób tylko myli gracza). Gravestones i Forgotten Graves biorą przedmioty dopiero po nas,
// więc dostają pusty ekwipunek i grobu nie stawiają.
public final class GravesCompat {
    private GravesCompat() {
    }

    public static void register() {
        if (Compat.UNIVERSAL_GRAVES.isLoaded()) {
            UniversalGraves.register();
        }
        if (Compat.YIGD.isLoaded()) {
            Yigd.register();
        }
    }

    private static boolean blockGrave(net.minecraft.server.level.ServerPlayer player) {
        return Config.BLOCK_GRAVES.get() && KeepInventory.isDeathSaved(player);
    }

    // Osobne klasy, żeby klasy jednego moda nie były ładowane, gdy zainstalowany jest tylko drugi
    private static final class UniversalGraves {
        static void register() {
            PlayerGraveCreationEvent.EVENT.register(player -> blockGrave(player)
                    ? PlayerGraveCreationEvent.CreationResult.BLOCK_SILENT
                    : PlayerGraveCreationEvent.CreationResult.ALLOW);
        }
    }

    private static final class Yigd {
        static void register() {
            AllowGraveGenerationEvent.EVENT.register((context, grave) -> !blockGrave(context.player()));
        }
    }
}
