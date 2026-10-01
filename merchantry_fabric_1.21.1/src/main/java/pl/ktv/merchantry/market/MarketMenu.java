package pl.ktv.merchantry.market;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.shop.ShopMenu;
import pl.ktv.merchantry.shop.editor.GuiMenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Okno rynku (9x6):
//  rząd 0: kategorie + "moje oferty"
//  rzędy 1-4: oferty graczy (najnowsze pierwsze)
//  rząd 5: strony, powrót do sklepu, wystaw przedmiot, saldo
public class MarketMenu extends GuiMenu {
    private static final int SLOT_MINE = 8;
    private static final int FIRST_LISTING = 9;
    private static final int LISTING_SLOTS = 36;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_SHOP = 46;
    private static final int SLOT_SELL = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 53;

    private State state;
    private List<MarketListing> shown = List.of();

    // Wybrana kategoria i strona - zachowywane po powrocie z okna zakupu
    public record State(MarketCategory category, boolean mine, int page) {
        public static final State DEFAULT = new State(MarketCategory.ALL, false, 0);

        State withPage(int newPage) {
            return new State(category, mine, newPage);
        }
    }

    private MarketMenu(int containerId, Inventory inventory, ServerPlayer player, State state) {
        super(MenuType.GENERIC_9x6, 6, containerId, inventory, player);
        this.state = state;
        render();
    }

    public static void open(ServerPlayer player, State state) {
        if (!MarketService.checkAvailable(player)) {
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MarketMenu(id, inventory, player, state),
                Lang.msg("market.title")));
    }

    @Override
    protected boolean requiresOp() {
        return false;
    }

    @Override
    protected void onClick(int slot, boolean shift, boolean right, ItemStack carried) {
        if (!MarketService.isAvailable()) {
            player.closeContainer();
            return;
        }
        MarketCategory[] categories = MarketCategory.values();
        if (slot < categories.length) {
            show(new State(categories[slot], false, 0));
        } else if (slot == SLOT_MINE) {
            show(new State(MarketCategory.ALL, true, 0));
        } else if (slot >= FIRST_LISTING && slot < FIRST_LISTING + LISTING_SLOTS) {
            int index = slot - FIRST_LISTING;
            if (index < shown.size()) {
                MarketListing listing = shown.get(index);
                if (shift && player.hasPermissions(2) && !listing.seller().equals(player.getUUID())) {
                    MarketService.adminRemove(player, listing.id());
                    render();
                } else {
                    MarketBuyMenu.open(player, listing.id(), state);
                }
            }
        } else if (slot == SLOT_PREV && state.page() > 0) {
            show(state.withPage(state.page() - 1));
        } else if (slot == SLOT_NEXT && (state.page() + 1) * LISTING_SLOTS < filtered().size()) {
            show(state.withPage(state.page() + 1));
        } else if (slot == SLOT_SHOP) {
            ShopMenu.open(player);
        } else if (slot == SLOT_SELL) {
            player.closeContainer();
            String command = "/market sell ";
            player.sendSystemMessage(Lang.msg("market.sell_chat").withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command)))));
        }
    }

    private void show(State newState) {
        state = newState;
        render();
    }

    private List<MarketListing> filtered() {
        List<MarketListing> list = new ArrayList<>();
        for (MarketListing listing : MarketService.data(player.server).listings) {
            boolean matches = state.mine()
                    ? listing.seller().equals(player.getUUID())
                    : state.category().matches(listing.stack());
            if (matches) {
                list.add(listing);
            }
        }
        list.sort(Comparator.comparingLong(MarketListing::created).reversed());
        return list;
    }

    @Override
    protected void render() {
        display.clearContent();
        ShopMenu.fill(display, 0, 54);

        MarketCategory[] categories = MarketCategory.values();
        for (int i = 0; i < categories.length; i++) {
            boolean selected = !state.mine() && state.category() == categories[i];
            ItemStack icon = ShopMenu.named(new ItemStack(categories[i].icon()),
                    Lang.msg(selected ? "market.category.selected" : "market.category.name", Lang.msg(categories[i].key())),
                    List.of());
            icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, selected);
            display.setItem(i, icon);
        }
        ItemStack mine = ShopMenu.named(new ItemStack(Items.PLAYER_HEAD),
                Lang.msg(state.mine() ? "market.category.selected" : "market.category.name", Lang.msg("market.mine")),
                List.of(Lang.msg("market.mine.hint", MarketService.countListings(MarketService.data(player.server),
                        player.getUUID()), Config.MAX_MARKET_LISTINGS.get())));
        mine.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, state.mine());
        display.setItem(SLOT_MINE, mine);

        List<MarketListing> all = filtered();
        int pages = Math.max(1, (all.size() + LISTING_SLOTS - 1) / LISTING_SLOTS);
        int page = Math.min(state.page(), pages - 1);
        shown = all.subList(Math.min(all.size(), page * LISTING_SLOTS), Math.min(all.size(), (page + 1) * LISTING_SLOTS));
        for (int i = 0; i < LISTING_SLOTS; i++) {
            display.setItem(FIRST_LISTING + i, i < shown.size() ? listingIcon(player, shown.get(i), true) : ItemStack.EMPTY);
        }

        if (page > 0) {
            display.setItem(SLOT_PREV, ShopMenu.named(new ItemStack(Items.ARROW), Lang.msg("shop.prev"), List.of()));
        }
        if (page < pages - 1) {
            display.setItem(SLOT_NEXT, ShopMenu.named(new ItemStack(Items.ARROW), Lang.msg("shop.next"), List.of()));
        }
        display.setItem(SLOT_SHOP, ShopMenu.named(new ItemStack(Items.CHEST), Lang.msg("market.back_to_shop"), List.of()));
        display.setItem(SLOT_SELL, ShopMenu.named(new ItemStack(Items.WRITABLE_BOOK), Lang.msg("market.sell_button"),
                List.of(Lang.msg("market.sell_button.hint"), Lang.msg("market.fee_info",
                        formatPercent(Config.MARKET_FEE_PERCENT.get())))));
        display.setItem(SLOT_INFO, ShopMenu.named(new ItemStack(Items.GOLD_INGOT),
                Lang.msg("shop.info.balance", Economy.format(Economy.getBalance(player))),
                List.of(Lang.msg("shop.info.page", page + 1, pages), Lang.msg("market.count", all.size()))));
    }

    // Prawdziwy przedmiot z oferty + dopisane linie: cena, sprzedający, co zrobi kliknięcie
    public static ItemStack listingIcon(ServerPlayer player, MarketListing listing, boolean withHint) {
        ItemStack icon = listing.stack().copy();
        List<Component> lines = new ArrayList<>();
        ItemLore existing = icon.get(DataComponents.LORE);
        if (existing != null) {
            lines.addAll(existing.lines());
        }
        lines.add(Component.empty());
        lines.add(noItalic(Lang.msg("market.price", Economy.format(listing.price()))));
        lines.add(noItalic(Lang.msg("market.seller", listing.sellerName())));
        if (withHint) {
            boolean own = listing.seller().equals(player.getUUID());
            lines.add(noItalic(Lang.msg(own ? "market.hint.own" : "market.hint.buy")));
            if (!own && player.hasPermissions(2)) {
                lines.add(noItalic(Lang.msg("market.hint.admin")));
            }
        }
        icon.set(DataComponents.LORE, new ItemLore(lines));
        return icon;
    }

    private static Component noItalic(Component component) {
        return component.copy().withStyle(style -> style.withItalic(false));
    }

    private static String formatPercent(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
