package pl.ktv.merchantry.compat;

import net.fabricmc.loader.api.FabricLoader;

// Mody, z którymi Merchantry współpracuje. Wszystkie są opcjonalne - klasy zgodności
// (pakiety compat.*) wolno ładować tylko wtedy, gdy dany mod jest zainstalowany.
public enum Compat {
    CREATE("create"),
    CREATE_ADDITIONS("createaddition"),
    CREATE_NEW_AGE("create_new_age"),
    CREATE_MORE_ADDITIONS("create_more_additions"),
    NUMISMATICS("numismatics"),
    AE2("ae2"),
    FTB_ULTIMINE("ftbultimine"),
    FTB_CHUNKS("ftbchunks"),
    TRINKETS("trinkets"),
    UNIVERSAL_GRAVES("universal-graves"),
    YIGD("yigd"),
    XAEROS_MINIMAP("xaerominimap"),
    JOURNEYMAP("journeymap");

    private final String modId;
    private Boolean loaded;

    Compat(String modId) {
        this.modId = modId;
    }

    public String modId() {
        return modId;
    }

    public boolean isLoaded() {
        if (loaded == null) {
            loaded = FabricLoader.getInstance().isModLoaded(modId);
        }
        return loaded;
    }

    // Zgodność z modem po jego ID (np. dla domyślnych ofert)
    public static boolean isLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
