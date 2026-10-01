package pl.ktv.merchantry.market;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.economy.Economy;
import pl.ktv.merchantry.shop.ShopMenu;
import pl.ktv.merchantry.shop.editor.GuiMenu;

import java.util.List;
import java.util.UUID;

// Potwierdzenie zakupu z rynku (9x3); przy własnej ofercie - wycofanie jej
public class MarketBuyMenu extends GuiMenu {
    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_ITEM = 13;
    private static final int SLOT_CANCEL = 15;

    private final UUID listingId;
    private final MarketMenu.State returnState;

    private MarketBuyMenu(int containerId, Inventory inventory, ServerPlayer player, UUID listingId,
                          MarketMenu.State returnState) {
        super(MenuType.GENERIC_9x3, 3, containerId, inventory, player);
        this.listingId = listingId;
        this.returnState = returnState;
        render();
    }

    public static void open(ServerPlayer player, UUID listingId, MarketMenu.State returnState) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new MarketBuyMenu(id, inventory, player, listingId, returnState),
                Lang.msg("market.confirm.title")));
    }

    @Override
    protected boolean requiresOp() {
        return false;
    }

    @Override
    protected void onClick(int slot, boolean shift, boolean right, ItemStack carried) {
        if (slot == SLOT_CONFIRM) {
            MarketService.buy(player, listingId);
            MarketMenu.open(player, returnState);
        } else if (slot == SLOT_CANCEL) {
            MarketMenu.open(player, returnState);
        }
    }

    @Override
    protected void render() {
        display.clearContent();
        ShopMenu.fill(display, 0, 27);
        MarketListing listing = MarketService.data(player.server).find(listingId);
        if (listing == null) {
            display.setItem(SLOT_ITEM, ShopMenu.named(new ItemStack(Items.BARRIER), Lang.msg("market.gone"), List.of()));
        } else {
            display.setItem(SLOT_ITEM, MarketMenu.listingIcon(player, listing, false));
            boolean own = listing.seller().equals(player.getUUID());
            display.setItem(SLOT_CONFIRM, ShopMenu.named(new ItemStack(own ? Items.ORANGE_CONCRETE : Items.LIME_CONCRETE),
                    own ? Lang.msg("market.confirm.withdraw") : Lang.msg("market.confirm.buy", Economy.format(listing.price())),
                    own ? List.of() : List.of(Lang.msg("shop.info.balance", Economy.format(Economy.getBalance(player))))));
        }
        display.setItem(SLOT_CANCEL, ShopMenu.named(new ItemStack(Items.RED_CONCRETE), Lang.msg("confirm.cancel"), List.of()));
    }
}
