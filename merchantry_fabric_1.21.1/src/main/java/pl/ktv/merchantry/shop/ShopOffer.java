package pl.ktv.merchantry.shop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.unlock.Unlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Pojedyncza oferta w sklepie. Ma dwie niezależne ceny: w $ (gdy waluta włączona)
// i w przedmiotach (gdy wyłączona) - admin ustawia jedną lub obie.
public class ShopOffer {
    public static final Codec<ShopOffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(o -> o.id),
            OfferType.CODEC.fieldOf("type").forGetter(o -> o.type),
            Codec.STRING.optionalFieldOf("name", "").forGetter(o -> o.name),
            ItemStack.CODEC.optionalFieldOf("icon").forGetter(o -> optional(o.icon)),
            Codec.STRING.listOf().optionalFieldOf("description", List.of()).forGetter(o -> o.description),
            Codec.LONG.optionalFieldOf("price_money").forGetter(o -> Optional.ofNullable(o.moneyPrice)),
            ItemPrice.CODEC.optionalFieldOf("price_item").forGetter(o -> Optional.ofNullable(o.itemPrice)),
            ItemStack.CODEC.optionalFieldOf("item").forGetter(o -> optional(o.item)),
            Codec.STRING.optionalFieldOf("unlock").forGetter(o -> Optional.ofNullable(o.unlock)),
            Codec.STRING.optionalFieldOf("command").forGetter(o -> Optional.ofNullable(o.command))
    ).apply(instance, ShopOffer::new));

    public String id;
    public OfferType type;
    public String name;
    public ItemStack icon;
    public List<String> description;
    public Long moneyPrice;
    public ItemPrice itemPrice;
    public ItemStack item;
    public String unlock;
    public String command;

    public ShopOffer(String id, OfferType type) {
        this(id, type, "", Optional.empty(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    private ShopOffer(String id, OfferType type, String name, Optional<ItemStack> icon, List<String> description,
                      Optional<Long> moneyPrice, Optional<ItemPrice> itemPrice, Optional<ItemStack> item,
                      Optional<String> unlock, Optional<String> command) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.icon = icon.orElse(ItemStack.EMPTY);
        this.description = new ArrayList<>(description);
        this.moneyPrice = moneyPrice.orElse(null);
        this.itemPrice = itemPrice.orElse(null);
        this.item = item.orElse(ItemStack.EMPTY);
        this.unlock = unlock.orElse(null);
        this.command = command.orElse(null);
    }

    // Nazwa może być kluczem tłumaczenia ("@default.home_slot") - wtedy jest w języku gracza
    public Component displayName() {
        if (!name.isBlank()) {
            return Component.literal(Lang.resolve(name).replace('&', '§'));
        }
        if (type == OfferType.ITEM && !item.isEmpty()) {
            return item.getHoverName();
        }
        return Component.literal(id);
    }

    // Linie opisu po przetłumaczeniu kluczy i zamianie kodów kolorów &
    public List<String> descriptionLines() {
        List<String> lines = new ArrayList<>();
        for (String line : description) {
            lines.add(Lang.resolve(line).replace('&', '§'));
        }
        return lines;
    }

    // Ikona w oknie sklepu: własna albo domyślna dla rodzaju oferty
    public ItemStack iconStack() {
        if (!icon.isEmpty()) {
            return icon.copy();
        }
        return switch (type) {
            case ITEM -> item.isEmpty() ? new ItemStack(Items.BARRIER) : item.copy();
            case UNLOCK -> {
                Unlock u = Unlock.byId(unlock);
                yield new ItemStack(u != null ? u.icon() : Items.BARRIER);
            }
            case HOME_SLOT -> new ItemStack(Items.RED_BED);
            case COMMAND -> new ItemStack(Items.PAPER);
            case REPAIR -> new ItemStack(Items.CHIPPED_ANVIL);
            case REPAIR_ALL -> new ItemStack(Items.DAMAGED_ANVIL);
            case KEEP_INVENTORY -> new ItemStack(Items.TOTEM_OF_UNDYING);
        };
    }

    private static Optional<ItemStack> optional(ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
    }
}
