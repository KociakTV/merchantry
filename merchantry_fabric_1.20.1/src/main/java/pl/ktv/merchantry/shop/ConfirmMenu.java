package pl.ktv.merchantry.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.ktv.merchantry.Lang;

import java.util.List;

// Okno potwierdzenia zakupu (skrzynka 9x3). Dla przedmiotów pozwala wybrać ilość.
//  [ ][-10][-1][ ][oferta][ ][+1][+10][ ]
//  [ ][ ][potwierdź][ ][ ][ ][anuluj][ ][ ]
public class ConfirmMenu extends ChestMenu {
    private static final int SLOT_MINUS_10 = 10;
    private static final int SLOT_MINUS_1 = 11;
    private static final int SLOT_OFFER = 13;
    private static final int SLOT_PLUS_1 = 15;
    private static final int SLOT_PLUS_10 = 16;
    private static final int SLOT_CONFIRM = 20;
    private static final int SLOT_CANCEL = 24;

    private final ServerPlayer player;
    private final SimpleContainer display;
    private final ShopOffer offer;
    // Strona sklepu, do której wracamy; -1 = zamknij okno (otwarte komendą)
    private final int returnPage;
    private int quantity = 1;

    private ConfirmMenu(int containerId, Inventory inventory, ServerPlayer player, SimpleContainer display,
                        ShopOffer offer, int returnPage) {
        super(MenuType.GENERIC_9x3, containerId, inventory, display, 3);
        this.player = player;
        this.display = display;
        this.offer = offer;
        this.returnPage = returnPage;
        render();
    }

    public static void open(ServerPlayer player, ShopOffer offer, int returnPage) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ConfirmMenu(id, inventory, player, new SimpleContainer(27), offer, returnPage),
                Lang.msg("confirm.title")));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clicker) {
        Lang.setContext(player);
        if (slotId >= 0 && slotId < 27 && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
            handleClick(slotId);
        }
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

    private void handleClick(int slot) {
        boolean hasQuantity = ShopService.maxQuantity(offer) > 1;
        switch (slot) {
            case SLOT_MINUS_10 -> changeQuantity(hasQuantity, -10);
            case SLOT_MINUS_1 -> changeQuantity(hasQuantity, -1);
            case SLOT_PLUS_1 -> changeQuantity(hasQuantity, 1);
            case SLOT_PLUS_10 -> changeQuantity(hasQuantity, 10);
            case SLOT_CONFIRM -> {
                if (ShopService.purchase(player, offer, quantity)) {
                    back();
                } else {
                    render();
                }
            }
            case SLOT_CANCEL -> back();
            default -> {
            }
        }
    }

    private void changeQuantity(boolean hasQuantity, int delta) {
        if (!hasQuantity) {
            return;
        }
        quantity = Math.max(1, Math.min(quantity + delta, ShopService.maxQuantity(offer)));
        render();
    }

    private void back() {
        if (returnPage >= 0) {
            ShopMenu.open(player, returnPage);
        } else {
            player.closeContainer();
        }
    }

    private void render() {
        display.clearContent();
        ShopMenu.fill(display, 0, 27);
        display.setItem(SLOT_OFFER, ShopMenu.offerIcon(player, offer, quantity));

        if (ShopService.maxQuantity(offer) > 1) {
            display.setItem(SLOT_MINUS_10, button(Items.RED_STAINED_GLASS_PANE, "§c-10"));
            display.setItem(SLOT_MINUS_1, button(Items.RED_STAINED_GLASS_PANE, "§c-1"));
            display.setItem(SLOT_PLUS_1, button(Items.LIME_STAINED_GLASS_PANE, "§a+1"));
            display.setItem(SLOT_PLUS_10, button(Items.LIME_STAINED_GLASS_PANE, "§a+10"));
        }

        display.setItem(SLOT_CONFIRM, ShopMenu.named(new ItemStack(Items.LIME_CONCRETE), Lang.msg("confirm.accept"),
                List.of(Lang.msg("shop.price", Payment.describe(player, offer, quantity)))));
        display.setItem(SLOT_CANCEL, ShopMenu.named(new ItemStack(Items.RED_CONCRETE), Lang.msg("confirm.cancel"),
                List.of()));
    }

    private static ItemStack button(net.minecraft.world.item.Item item, String label) {
        return ShopMenu.named(new ItemStack(item), Component.literal(label), List.of());
    }
}
