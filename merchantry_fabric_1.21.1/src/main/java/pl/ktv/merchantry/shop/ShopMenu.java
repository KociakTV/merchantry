package pl.ktv.merchantry.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.command.HomeCommands;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.market.MarketMenu;
import pl.ktv.merchantry.market.MarketService;
import pl.ktv.merchantry.shop.sell.SellManager;
import pl.ktv.merchantry.shop.sell.SellOffer;

import java.util.ArrayList;
import java.util.List;

// Okno sklepu jako zwykła skrzynka 9x6 - działa na kliencie bez moda.
// Górne 5 rzędów to oferty, dolny rząd to nawigacja. Kliknięcie oferty otwiera okno potwierdzenia.
public class ShopMenu extends ChestMenu {
    private static final int OFFER_SLOTS = 45;
    // Dolny rząd: [ ][kup][sprzedaj][rynek][saldo][ ][ ][◀][▶] (kantor Numismatics jest tylko w wersji NeoForge)
    private static final int SLOT_INFO = 49;
    private static final int SLOT_PREV = 52;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_TAB_BUY = 46;
    private static final int SLOT_TAB_SELL = 47;
    private static final int SLOT_TAB_MARKET = 48;

    // Zakładki sklepu: kupowanie ofert serwera i sprzedawanie surowców serwerowi (rynek to osobne okno)
    public enum Tab {
        BUY, SELL
    }

    private final ServerPlayer player;
    private final SimpleContainer display;
    private int page;
    private Tab tab;

    private ShopMenu(int containerId, Inventory inventory, ServerPlayer player, SimpleContainer display, int page, Tab tab) {
        super(MenuType.GENERIC_9x6, containerId, inventory, display, 6);
        this.player = player;
        this.display = display;
        this.page = page;
        this.tab = tab;
        render();
    }

    public static void open(ServerPlayer player) {
        open(player, 0);
    }

    public static void open(ServerPlayer player, int page) {
        open(player, page, Tab.BUY);
    }

    public static void open(ServerPlayer player, int page, Tab tab) {
        Tab actual = tab == Tab.SELL && !Economy.isEnabled() ? Tab.BUY : tab;
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ShopMenu(id, inventory, player, new SimpleContainer(54), page, actual),
                Lang.msg("shop.title")));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clicker) {
        Lang.setContext(player);
        // Blokujemy wszystkie ruchy przedmiotów; obsługujemy tylko kliknięcia w górne okno
        if (slotId >= 0 && slotId < 54 && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
            handleClick(slotId, button == 1, clickType == ClickType.QUICK_MOVE);
        }
        // Przywraca klientowi prawdziwy stan okna (klient mógł "przewidzieć" podniesienie przedmiotu)
        if (player.containerMenu == this) {
            sendAllDataToRemote();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player clicker, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player clicker) {
        return true;
    }

    private void handleClick(int slot, boolean right, boolean shift) {
        if (slot < OFFER_SLOTS) {
            int index = page * OFFER_SLOTS + slot;
            if (tab == Tab.BUY && index < ShopManager.offers().size()) {
                ConfirmMenu.open(player, ShopManager.offers().get(index), page);
            } else if (tab == Tab.SELL && index < SellManager.offers().size()) {
                // LPM: 1 sztuka, PPM: 64, Shift+klik: wszystko
                SellManager.sell(player, SellManager.offers().get(index), shift ? -1 : right ? 64 : 1);
                render();
            }
        } else if (slot == SLOT_TAB_BUY && tab != Tab.BUY) {
            tab = Tab.BUY;
            page = 0;
            render();
        } else if (slot == SLOT_TAB_SELL && tab != Tab.SELL && Economy.isEnabled()) {
            tab = Tab.SELL;
            page = 0;
            render();
        } else if (slot == SLOT_TAB_MARKET && MarketService.isAvailable()) {
            MarketMenu.open(player, MarketMenu.State.DEFAULT);
        } else if (slot == SLOT_PREV && page > 0) {
            page--;
            render();
        } else if (slot == SLOT_NEXT && page < pageCount() - 1) {
            page++;
            render();
        }
    }

    private int pageCount() {
        int size = tab == Tab.SELL ? SellManager.offers().size() : ShopManager.offers().size();
        return Math.max(1, (size + OFFER_SLOTS - 1) / OFFER_SLOTS);
    }

    private void render() {
        display.clearContent();
        page = Math.max(0, Math.min(page, pageCount() - 1));

        for (int slot = 0; slot < OFFER_SLOTS; slot++) {
            int index = page * OFFER_SLOTS + slot;
            if (tab == Tab.BUY && index < ShopManager.offers().size()) {
                display.setItem(slot, offerIcon(player, ShopManager.offers().get(index), 1));
            } else if (tab == Tab.SELL && index < SellManager.offers().size()) {
                display.setItem(slot, sellIcon(SellManager.offers().get(index)));
            }
        }

        fill(display, OFFER_SLOTS, 54);
        display.setItem(SLOT_TAB_BUY, tabIcon(Items.CHEST, "shop.tab.buy", "shop.tab.buy.hint", tab == Tab.BUY));
        if (Economy.isEnabled()) {
            display.setItem(SLOT_TAB_SELL, tabIcon(Items.HOPPER, "shop.tab.sell", "shop.tab.sell.hint", tab == Tab.SELL));
        }
        if (MarketService.isAvailable()) {
            display.setItem(SLOT_TAB_MARKET, tabIcon(Items.BELL, "shop.tab.market", "shop.tab.market.hint", false));
        }
        if (page > 0) {
            display.setItem(SLOT_PREV, named(new ItemStack(Items.ARROW), Lang.msg("shop.prev"), List.of()));
        }
        if (page < pageCount() - 1) {
            display.setItem(SLOT_NEXT, named(new ItemStack(Items.ARROW), Lang.msg("shop.next"), List.of()));
        }
        Component info = Economy.isEnabled()
                ? Lang.msg("shop.info.balance", Economy.format(Economy.getBalance(player)))
                : Lang.msg("shop.info.items");
        display.setItem(SLOT_INFO, named(new ItemStack(Items.GOLD_INGOT), info,
                List.of(Lang.msg("shop.info.page", page + 1, pageCount()))));
    }

    private ItemStack tabIcon(net.minecraft.world.item.Item item, String nameKey, String hintKey, boolean active) {
        ItemStack icon = named(new ItemStack(item), Lang.msg(active ? "shop.tab.active" : "shop.tab.inactive", Lang.msg(nameKey)),
                List.of(Lang.msg(hintKey)));
        icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, active);
        return icon;
    }

    // Ikona oferty skupu: cena za sztukę, ile gracz ma i ile za to dostanie
    private ItemStack sellIcon(SellOffer offer) {
        int have = SellManager.count(player, offer.item());
        return named(new ItemStack(offer.item()), offer.item().getDescription().copy().withStyle(ChatFormatting.GOLD), List.of(
                Lang.msg("sell.price", Economy.format(offer.price())),
                Lang.msg("sell.have", have, Economy.format(have * offer.price())),
                Component.empty(),
                Lang.msg("sell.hint.one"),
                Lang.msg("sell.hint.stack"),
                Lang.msg("sell.hint.all")));
    }

    // Ikona oferty z opisem, ceną dla podanej ilości i stanem (wspólna dla sklepu i potwierdzenia)
    public static ItemStack offerIcon(ServerPlayer player, ShopOffer offer, int quantity) {
        List<Component> lore = new ArrayList<>();
        for (String line : offer.descriptionLines()) {
            lore.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        switch (offer.type) {
            case HOME_SLOT -> lore.add(Lang.msg("shop.homes_count", HomeCommands.limit(player), Config.MAX_HOMES.get()));
            case REPAIR -> {
                ItemStack hand = player.getMainHandItem();
                lore.add(Lang.msg("repair.target", hand.isEmpty() ? Component.literal("-") : hand.getHoverName()));
            }
            case REPAIR_ALL -> lore.add(Lang.msg("repair.count",
                    pl.ktv.merchantry.repair.Repair.targets(player, true).size()));
            case KEEP_INVENTORY -> lore.add(Lang.msg("keepinv.charges",
                    pl.ktv.merchantry.data.ModAttachments.get(player).keepInventoryCharges));
            default -> {
            }
        }
        lore.add(Component.empty());
        if (quantity > 1) {
            lore.add(Lang.msg("shop.quantity", quantity));
        }
        lore.add(Lang.msg("shop.price", Payment.describe(player, offer, quantity)));
        lore.add(Lang.msg(switch (ShopService.status(player, offer, quantity)) {
            case AVAILABLE -> "shop.status.buy";
            case CANT_AFFORD -> "shop.status.cant_afford";
            case OWNED -> "shop.status.owned";
            case MAX_REACHED -> "shop.status.max";
            case NOTHING_TO_REPAIR -> "shop.status.nothing_to_repair";
            case UNAVAILABLE -> "shop.status.unavailable";
        }));
        ItemStack icon = offer.iconStack();
        icon.setCount(Math.min(99, icon.getCount() * quantity));
        return named(icon, offer.displayName().copy().withStyle(ChatFormatting.GOLD), lore);
    }

    public static ItemStack named(ItemStack stack, Component name, List<Component> lore) {
        stack.set(DataComponents.CUSTOM_NAME, name.copy().withStyle(style -> style.withItalic(false)));
        List<Component> lines = new ArrayList<>();
        for (Component line : lore) {
            lines.add(line.copy().withStyle(style -> style.withItalic(false)));
        }
        stack.set(DataComponents.LORE, new ItemLore(lines));
        stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return stack;
    }

    // Wypełnia sloty [from, to) szarymi szybami bez opisu
    public static void fill(SimpleContainer container, int from, int to) {
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
        for (int slot = from; slot < to; slot++) {
            container.setItem(slot, filler.copy());
        }
    }
}
