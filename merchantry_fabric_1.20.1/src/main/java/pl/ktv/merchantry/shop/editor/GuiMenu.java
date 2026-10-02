package pl.ktv.merchantry.shop.editor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import pl.ktv.merchantry.Lang;

// Wspólna baza okien edytora (skrzynka). Górne sloty są przyciskami; we własnym ekwipunku
// można normalnie podnieść przedmiot na kursor, żeby kliknąć nim w pole (np. "ustaw produkt").
public abstract class GuiMenu extends ChestMenu {
    protected final ServerPlayer player;
    protected final SimpleContainer display;
    private final int size;

    protected GuiMenu(MenuType<?> type, int rows, int containerId, Inventory inventory, ServerPlayer player) {
        this(type, rows, containerId, inventory, player, new SimpleContainer(rows * 9));
    }

    private GuiMenu(MenuType<?> type, int rows, int containerId, Inventory inventory, ServerPlayer player,
                    SimpleContainer display) {
        super(type, containerId, inventory, display, rows);
        this.player = player;
        this.display = display;
        this.size = rows * 9;
    }

    // Kliknięcie w górne okno. shift = shift+klik, right = prawy przycisk, carried = przedmiot na kursorze
    protected abstract void onClick(int slot, boolean shift, boolean right, ItemStack carried);

    protected abstract void render();

    // Okna edytora są tylko dla operatorów; okna rynku nadpisują to na false
    protected boolean requiresOp() {
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player clicker) {
        Lang.setContext(player);
        if (requiresOp() && !player.hasPermissions(2)) {
            player.closeContainer();
            return;
        }
        if (slotId >= 0 && slotId < size) {
            if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
                onClick(slotId, clickType == ClickType.QUICK_MOVE, button == 1, getCarried());
            }
        } else if (slotId >= size && clickType == ClickType.PICKUP) {
            // Własny ekwipunek: zwykłe podnoszenie/odkładanie przedmiotów
            super.clicked(slotId, button, clickType, clicker);
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
}
