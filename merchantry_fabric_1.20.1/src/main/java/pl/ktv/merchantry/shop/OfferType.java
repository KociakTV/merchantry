package pl.ktv.merchantry.shop;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

// Rodzaj oferty w sklepie
public enum OfferType implements StringRepresentable {
    // Przedmiot dawany graczowi
    ITEM("item"),
    // Komenda odblokowana na zawsze (/craft, /anvil...)
    UNLOCK("unlock"),
    // Dodatkowe miejsce na dom
    HOME_SLOT("home_slot"),
    // Jednorazowe wykonanie komendy przez serwer
    COMMAND("command"),
    // Naprawa przedmiotu w ręce (tylko za walutę)
    REPAIR("repair"),
    // Naprawa całego ekwipunku (tylko za walutę)
    REPAIR_ALL("repair_all"),
    // Ładunek jednorazowego keepInventory (można kupić kilka naraz)
    KEEP_INVENTORY("keep_inventory");

    public static final Codec<OfferType> CODEC = StringRepresentable.fromEnum(OfferType::values);

    private final String name;

    OfferType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
