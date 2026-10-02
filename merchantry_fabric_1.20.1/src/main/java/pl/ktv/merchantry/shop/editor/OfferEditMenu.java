package pl.ktv.merchantry.shop.editor;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.shop.ItemPrice;
import pl.ktv.merchantry.shop.OfferType;
import pl.ktv.merchantry.shop.ShopManager;
import pl.ktv.merchantry.shop.ShopMenu;
import pl.ktv.merchantry.shop.ShopOffer;
import pl.ktv.merchantry.unlock.Unlock;

import java.util.ArrayList;
import java.util.List;

// Edycja jednej oferty (9x6). Każda zmiana zapisuje się od razu do config/merchantry_offers.json.
//  rząd 0: podgląd w sklepie
//  rząd 1: produkt | ikona | nazwa | opis
//  rząd 2: cena w $       [-1000][-100][-10][-1][cena][+1][+10][+100][+1000]
//  rząd 3: cena w przedm. [-64][-10][-1][ ][przedmiot][ ][+1][+10][+64]
//  rząd 4: przesuń w lewo | odblokowanie/komenda | przesuń w prawo
//  rząd 5: powrót | usuń | zamknij
public class OfferEditMenu extends GuiMenu {
    private static final int SLOT_PREVIEW = 4;
    private static final int SLOT_PRODUCT = 10;
    private static final int SLOT_ICON = 12;
    private static final int SLOT_NAME = 14;
    private static final int SLOT_DESC = 16;
    private static final int[] MONEY_SLOTS = {18, 19, 20, 21, 23, 24, 25, 26};
    private static final long[] MONEY_STEPS = {-1000, -100, -10, -1, 1, 10, 100, 1000};
    private static final int SLOT_MONEY = 22;
    private static final int[] ITEM_SLOTS = {27, 28, 29, 33, 34, 35};
    private static final int[] ITEM_STEPS = {-64, -10, -1, 1, 10, 64};
    private static final int SLOT_ITEM_PRICE = 31;
    private static final int SLOT_MOVE_LEFT = 38;
    private static final int SLOT_SPECIAL = 40;
    private static final int SLOT_MOVE_RIGHT = 42;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_DELETE = 49;
    private static final int SLOT_CLOSE = 53;

    private final ShopOffer offer;

    private OfferEditMenu(int containerId, Inventory inventory, ServerPlayer player, ShopOffer offer) {
        super(MenuType.GENERIC_9x6, 6, containerId, inventory, player);
        this.offer = offer;
        render();
    }

    // Znaczniki pól, na które można upuścić przedmiot z JEI/EMI (odczytuje je opcjonalna część klienta)
    public static final String DROP_TAG = "merchantry_drop";
    public static final String FIELD_PRODUCT = "product";
    public static final String FIELD_ICON = "icon";
    public static final String FIELD_PRICE_ITEM = "price_item";

    public ShopOffer offer() {
        return offer;
    }

    // Odświeża okno, jeśli gracz edytuje tę ofertę (np. po komendzie wysłanej przez JEI/EMI)
    public static void refreshFor(ServerPlayer player, ShopOffer offer) {
        if (player.containerMenu instanceof OfferEditMenu menu && menu.offer == offer) {
            menu.render();
            menu.sendAllDataToRemote();
        }
    }

    private ItemStack dropTarget(ItemStack stack, String field) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("field", field);
        tag.putString("offer", offer.id);
        pl.ktv.merchantry.util.Stacks.putCustomData(stack, DROP_TAG, tag);
        return stack;
    }

    public static void open(ServerPlayer player, ShopOffer offer) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new OfferEditMenu(id, inventory, player, offer),
                Lang.msg("editor.edit.title", offer.id)));
    }

    // Naprawy mają cenę liczoną automatycznie - bez edycji cen
    private boolean hasManualPrice() {
        return offer.type != OfferType.REPAIR && offer.type != OfferType.REPAIR_ALL;
    }

    @Override
    protected void onClick(int slot, boolean shift, boolean right, ItemStack carried) {
        // Oferta mogła zostać usunięta komendą w międzyczasie
        if (!ShopManager.offers().contains(offer)) {
            EditorListMenu.open(player, 0);
            return;
        }
        boolean changed = switch (slot) {
            case SLOT_PRODUCT -> setProduct(carried);
            case SLOT_ICON -> setIcon(shift, carried);
            case SLOT_NAME -> {
                if (shift) {
                    offer.name = "";
                    yield true;
                }
                suggest("editor.chat.name", "/shopconfig name " + offer.id + " ");
                yield false;
            }
            case SLOT_DESC -> {
                if (shift) {
                    offer.description.clear();
                    yield true;
                }
                suggest("editor.chat.desc", "/shopconfig desc " + offer.id + " ");
                yield false;
            }
            case SLOT_MONEY -> {
                if (!hasManualPrice()) {
                    yield false;
                }
                if (shift) {
                    offer.moneyPrice = null;
                    yield true;
                }
                suggest("editor.chat.money", "/shopconfig price " + offer.id + " money ");
                yield false;
            }
            case SLOT_ITEM_PRICE -> setItemPrice(shift, carried);
            case SLOT_MOVE_LEFT -> move(-1);
            case SLOT_MOVE_RIGHT -> move(1);
            case SLOT_SPECIAL -> special();
            case SLOT_DELETE -> {
                if (shift) {
                    ShopManager.offers().remove(offer);
                    ShopManager.save();
                    player.sendSystemMessage(Lang.msg("config.removed", offer.id));
                    EditorListMenu.open(player, 0);
                }
                yield false;
            }
            case SLOT_BACK -> {
                EditorListMenu.open(player, EditorListMenu.pageOf(offer));
                yield false;
            }
            case SLOT_CLOSE -> {
                player.closeContainer();
                yield false;
            }
            default -> step(slot);
        };
        if (changed) {
            ShopManager.save();
            render();
        }
    }

    private boolean setProduct(ItemStack carried) {
        if (offer.type != OfferType.ITEM || carried.isEmpty()) {
            return false;
        }
        offer.item = carried.copy();
        return true;
    }

    private boolean setIcon(boolean shift, ItemStack carried) {
        if (shift) {
            offer.icon = ItemStack.EMPTY;
            return true;
        }
        if (carried.isEmpty()) {
            return false;
        }
        offer.icon = carried.copyWithCount(1);
        return true;
    }

    private boolean setItemPrice(boolean shift, ItemStack carried) {
        if (!hasManualPrice()) {
            return false;
        }
        if (shift) {
            offer.itemPrice = null;
            return true;
        }
        if (carried.isEmpty()) {
            return false;
        }
        int count = offer.itemPrice != null ? offer.itemPrice.count() : carried.getCount();
        offer.itemPrice = new ItemPrice(carried.getItem(), count);
        return true;
    }

    // Przyciski +/- dla obu cen
    private boolean step(int slot) {
        if (!hasManualPrice()) {
            return false;
        }
        for (int i = 0; i < MONEY_SLOTS.length; i++) {
            if (slot == MONEY_SLOTS[i]) {
                long current = offer.moneyPrice != null ? offer.moneyPrice : 0;
                offer.moneyPrice = Math.max(0, current + MONEY_STEPS[i]);
                return true;
            }
        }
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (slot == ITEM_SLOTS[i]) {
                if (offer.itemPrice == null) {
                    player.sendSystemMessage(Lang.msg("editor.item_price.first"));
                    return false;
                }
                int count = Math.max(1, Math.min(1_000_000, offer.itemPrice.count() + ITEM_STEPS[i]));
                offer.itemPrice = new ItemPrice(offer.itemPrice.item(), count);
                return true;
            }
        }
        return false;
    }

    private boolean move(int delta) {
        List<ShopOffer> offers = ShopManager.offers();
        int index = offers.indexOf(offer);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= offers.size()) {
            return false;
        }
        offers.remove(index);
        offers.add(target, offer);
        return true;
    }

    // Odblokowanie: przełącza na następne; komenda: wpisanie na czacie
    private boolean special() {
        if (offer.type == OfferType.UNLOCK) {
            Unlock current = Unlock.byId(offer.unlock);
            List<Unlock> unlocks = Unlock.available();
            Unlock next = unlocks.get(current == null ? 0 : (unlocks.indexOf(current) + 1) % unlocks.size());
            // Domyślną nazwę i opis zmieniamy razem z odblokowaniem; własne zostawiamy
            if (current != null && offer.name.equals("@default.unlock." + current.id())) {
                offer.name = "@default.unlock." + next.id();
            }
            if (current != null && offer.description.equals(List.of(current.defaultDescription()))) {
                offer.description.clear();
                offer.description.add(next.defaultDescription());
            }
            offer.unlock = next.id();
            return true;
        }
        if (offer.type == OfferType.COMMAND) {
            suggest("editor.chat.command", "/shopconfig command " + offer.id + " ");
        }
        return false;
    }

    // Zamyka okno i wysyła klikalną wiadomość, która wpisuje komendę do czatu
    private void suggest(String key, String command) {
        player.closeContainer();
        player.sendSystemMessage(Lang.msg(key).withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command)))));
    }

    @Override
    protected void render() {
        display.clearContent();
        ShopMenu.fill(display, 0, 54);
        boolean currency = Economy.isEnabled();

        List<Component> preview = new ArrayList<>(List.of(Lang.msg("editor.preview")));
        display.setItem(SLOT_PREVIEW, withExtraLore(ShopMenu.offerIcon(player, offer, 1), preview));

        // Produkt
        if (offer.type == OfferType.ITEM) {
            ItemStack product = offer.item.isEmpty() ? new ItemStack(Items.BARRIER) : offer.item.copy();
            display.setItem(SLOT_PRODUCT, dropTarget(ShopMenu.named(product,
                    offer.item.isEmpty() ? Lang.msg("editor.product.none") : Lang.msg("editor.product", offer.item.getCount(), offer.item.getHoverName()),
                    List.of(Lang.msg("editor.product.hint"), Lang.msg("editor.jei_hint"))), FIELD_PRODUCT));
        } else {
            display.setItem(SLOT_PRODUCT, ShopMenu.named(offer.iconStack(), Lang.msg("editor.type." + offer.type.getSerializedName()),
                    List.of(Lang.msg("editor.product.not_item"))));
        }

        display.setItem(SLOT_ICON, dropTarget(ShopMenu.named(offer.iconStack(), Lang.msg("editor.icon"), List.of(
                Lang.msg(offer.icon.isEmpty() ? "editor.icon.default" : "editor.icon.custom"),
                Lang.msg("editor.icon.hint"), Lang.msg("editor.icon.reset"), Lang.msg("editor.jei_hint"))), FIELD_ICON));

        display.setItem(SLOT_NAME, ShopMenu.named(new ItemStack(Items.NAME_TAG), Lang.msg("editor.name"), List.of(
                Lang.msg("editor.current", offer.displayName()),
                Lang.msg("editor.chat_click"), Lang.msg("editor.name.reset"))));

        List<Component> descLore = new ArrayList<>();
        if (offer.description.isEmpty()) {
            descLore.add(Lang.msg("editor.desc.empty"));
        }
        for (String line : offer.descriptionLines()) {
            descLore.add(Component.literal("§7" + line));
        }
        descLore.add(Component.empty());
        descLore.add(Lang.msg("editor.desc.hint"));
        descLore.add(Lang.msg("editor.desc.reset"));
        display.setItem(SLOT_DESC, ShopMenu.named(new ItemStack(Items.WRITABLE_BOOK), Lang.msg("editor.desc"), descLore));

        if (hasManualPrice()) {
            renderMoney(currency);
            renderItemPrice(currency);
        } else {
            display.setItem(SLOT_MONEY, ShopMenu.named(new ItemStack(Items.GOLD_INGOT), Lang.msg("editor.auto_price"),
                    List.of(Lang.msg("editor.auto_price.hint"))));
        }

        display.setItem(SLOT_MOVE_LEFT, ShopMenu.named(new ItemStack(Items.SPECTRAL_ARROW), Lang.msg("editor.move_left"),
                List.of(Lang.msg("editor.position", ShopManager.offers().indexOf(offer) + 1, ShopManager.offers().size()))));
        display.setItem(SLOT_MOVE_RIGHT, ShopMenu.named(new ItemStack(Items.SPECTRAL_ARROW), Lang.msg("editor.move_right"),
                List.of(Lang.msg("editor.position", ShopManager.offers().indexOf(offer) + 1, ShopManager.offers().size()))));

        if (offer.type == OfferType.UNLOCK) {
            Unlock unlock = Unlock.byId(offer.unlock);
            display.setItem(SLOT_SPECIAL, ShopMenu.named(new ItemStack(unlock != null ? unlock.icon() : Items.BARRIER),
                    Lang.msg("editor.unlock", unlock != null ? "/" + unlock.id() : "?"), List.of(Lang.msg("editor.unlock.hint"))));
        } else if (offer.type == OfferType.COMMAND) {
            display.setItem(SLOT_SPECIAL, ShopMenu.named(new ItemStack(Items.COMMAND_BLOCK), Lang.msg("editor.command"), List.of(
                    Lang.msg("editor.current", offer.command == null ? "-" : "/" + offer.command),
                    Lang.msg("editor.chat_click"))));
        }

        display.setItem(SLOT_BACK, ShopMenu.named(new ItemStack(Items.ARROW), Lang.msg("editor.back"), List.of()));
        display.setItem(SLOT_DELETE, ShopMenu.named(new ItemStack(Items.LAVA_BUCKET), Lang.msg("editor.delete"),
                List.of(Lang.msg("editor.delete.hint"))));
        display.setItem(SLOT_CLOSE, ShopMenu.named(new ItemStack(Items.BARRIER), Lang.msg("editor.close"), List.of()));
    }

    private void renderMoney(boolean currency) {
        List<Component> lore = new ArrayList<>();
        lore.add(Lang.msg(currency ? "editor.price.active" : "editor.price.inactive_money"));
        if (offer.type == OfferType.HOME_SLOT) {
            lore.add(Lang.msg("editor.price.home_base"));
        }
        lore.add(Component.empty());
        lore.add(Lang.msg("editor.money.hint"));
        lore.add(Lang.msg("editor.price.reset"));
        display.setItem(SLOT_MONEY, ShopMenu.named(new ItemStack(offer.moneyPrice != null ? Items.GOLD_INGOT : Items.BARRIER),
                Lang.msg("editor.money", moneyText(offer)), lore));
        for (int i = 0; i < MONEY_SLOTS.length; i++) {
            display.setItem(MONEY_SLOTS[i], stepButton(MONEY_STEPS[i], Economy.format(Math.abs(MONEY_STEPS[i]))));
        }
    }

    private void renderItemPrice(boolean currency) {
        List<Component> lore = new ArrayList<>();
        lore.add(Lang.msg(currency ? "editor.price.inactive_items" : "editor.price.active"));
        if (offer.type == OfferType.HOME_SLOT) {
            lore.add(Lang.msg("editor.price.home_base"));
        }
        lore.add(Component.empty());
        lore.add(Lang.msg("editor.item_price.hint"));
        lore.add(Lang.msg("editor.price.reset"));
        lore.add(Lang.msg("editor.jei_hint"));
        ItemStack icon = offer.itemPrice != null
                ? new ItemStack(offer.itemPrice.item(), Math.min(99, offer.itemPrice.count()))
                : new ItemStack(Items.BARRIER);
        display.setItem(SLOT_ITEM_PRICE, dropTarget(ShopMenu.named(icon, Lang.msg("editor.items", itemPriceText(offer)), lore),
                FIELD_PRICE_ITEM));
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            display.setItem(ITEM_SLOTS[i], stepButton(ITEM_STEPS[i], String.valueOf(Math.abs(ITEM_STEPS[i]))));
        }
    }

    private static ItemStack stepButton(long step, String label) {
        Item item = step < 0 ? Items.RED_STAINED_GLASS_PANE : Items.LIME_STAINED_GLASS_PANE;
        return ShopMenu.named(new ItemStack(item), Component.literal((step < 0 ? "§c-" : "§a+") + label), List.of());
    }

    private static ItemStack withExtraLore(ItemStack stack, List<Component> extra) {
        List<Component> lines = new ArrayList<>(extra);
        lines.add(Component.empty());
        lines.addAll(pl.ktv.merchantry.util.Stacks.getLore(stack));
        pl.ktv.merchantry.util.Stacks.setLore(stack, lines);
        return stack;
    }

    public static Component moneyText(ShopOffer offer) {
        if (offer.type == OfferType.REPAIR || offer.type == OfferType.REPAIR_ALL) {
            return Lang.msg("editor.auto");
        }
        return offer.moneyPrice != null ? Component.literal(Economy.format(offer.moneyPrice)) : Lang.msg("editor.none");
    }

    public static Component itemPriceText(ShopOffer offer) {
        if (offer.type == OfferType.REPAIR || offer.type == OfferType.REPAIR_ALL) {
            return Lang.msg("editor.money_only");
        }
        return offer.itemPrice != null
                ? Component.literal(offer.itemPrice.count() + "x ").append(offer.itemPrice.item().getDescription())
                : Lang.msg("editor.none");
    }
}
