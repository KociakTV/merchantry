package pl.ktv.merchantry.shop.editor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.shop.ShopManager;
import pl.ktv.merchantry.shop.ShopMenu;
import pl.ktv.merchantry.shop.ShopOffer;

import java.util.ArrayList;
import java.util.List;

// Lista ofert w edytorze (9x6). Kliknięcie oferty otwiera jej edycję.
public class EditorListMenu extends GuiMenu {
    private static final int OFFER_SLOTS = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_MODE = 47;
    private static final int SLOT_NEW = 49;
    private static final int SLOT_SHOP = 51;
    private static final int SLOT_NEXT = 53;

    private int page;

    private EditorListMenu(int containerId, Inventory inventory, ServerPlayer player, int page) {
        super(MenuType.GENERIC_9x6, 6, containerId, inventory, player);
        this.page = page;
        render();
    }

    public static void open(ServerPlayer player, int page) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EditorListMenu(id, inventory, player, page),
                Lang.msg("editor.list.title")));
    }

    public static int pageOf(ShopOffer offer) {
        return Math.max(0, ShopManager.offers().indexOf(offer)) / OFFER_SLOTS;
    }

    @Override
    protected void onClick(int slot, boolean shift, boolean right, ItemStack carried) {
        List<ShopOffer> offers = ShopManager.offers();
        if (slot < OFFER_SLOTS) {
            int index = page * OFFER_SLOTS + slot;
            if (index < offers.size()) {
                OfferEditMenu.open(player, offers.get(index));
            }
        } else if (slot == SLOT_PREV && page > 0) {
            page--;
            render();
        } else if (slot == SLOT_NEXT && page < pageCount() - 1) {
            page++;
            render();
        } else if (slot == SLOT_NEW) {
            NewOfferMenu.open(player);
        } else if (slot == SLOT_SHOP) {
            ShopMenu.open(player);
        }
    }

    private int pageCount() {
        return Math.max(1, (ShopManager.offers().size() + OFFER_SLOTS - 1) / OFFER_SLOTS);
    }

    @Override
    protected void render() {
        display.clearContent();
        List<ShopOffer> offers = ShopManager.offers();
        page = Math.max(0, Math.min(page, pageCount() - 1));
        for (int slot = 0; slot < OFFER_SLOTS; slot++) {
            int index = page * OFFER_SLOTS + slot;
            if (index >= offers.size()) {
                break;
            }
            display.setItem(slot, icon(offers.get(index)));
        }

        ShopMenu.fill(display, OFFER_SLOTS, 54);
        if (page > 0) {
            display.setItem(SLOT_PREV, ShopMenu.named(new ItemStack(Items.ARROW), Lang.msg("shop.prev"), List.of()));
        }
        if (page < pageCount() - 1) {
            display.setItem(SLOT_NEXT, ShopMenu.named(new ItemStack(Items.ARROW), Lang.msg("shop.next"), List.of()));
        }
        display.setItem(SLOT_MODE, ShopMenu.named(new ItemStack(Items.BOOK), Lang.msg("editor.mode.title"), List.of(
                Lang.msg(Economy.isEnabled() ? "editor.mode.money" : "editor.mode.items"),
                Lang.msg("shop.info.page", page + 1, pageCount()))));
        display.setItem(SLOT_NEW, ShopMenu.named(new ItemStack(Items.NETHER_STAR), Lang.msg("editor.new"),
                List.of(Lang.msg("editor.new.hint"))));
        display.setItem(SLOT_SHOP, ShopMenu.named(new ItemStack(Items.CHEST), Lang.msg("editor.open_shop"), List.of()));
    }

    private static ItemStack icon(ShopOffer offer) {
        List<Component> lore = new ArrayList<>();
        lore.add(Lang.msg("editor.info.id", offer.id));
        lore.add(Lang.msg("editor.info.type", Lang.msg("editor.type." + offer.type.getSerializedName())));
        lore.add(Lang.msg("editor.info.money", OfferEditMenu.moneyText(offer)));
        lore.add(Lang.msg("editor.info.items", OfferEditMenu.itemPriceText(offer)));
        lore.add(Component.empty());
        lore.add(Lang.msg("editor.click_edit"));
        return ShopMenu.named(offer.iconStack(), offer.displayName(), lore);
    }
}
