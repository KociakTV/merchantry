package pl.ktv.merchantry.shop.editor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.shop.OfferType;
import pl.ktv.merchantry.shop.ShopManager;
import pl.ktv.merchantry.shop.ShopMenu;
import pl.ktv.merchantry.shop.ShopOffer;

import java.util.List;

// Wybór rodzaju nowej oferty (9x3). Dla przedmiotu: kliknij, trzymając produkt na kursorze.
public class NewOfferMenu extends GuiMenu {
    private static final OfferType[] TYPES = {OfferType.ITEM, OfferType.UNLOCK, OfferType.HOME_SLOT,
            OfferType.COMMAND, OfferType.REPAIR, OfferType.REPAIR_ALL, OfferType.KEEP_INVENTORY};
    private static final int FIRST_SLOT = 10;
    private static final int SLOT_BACK = 22;

    private NewOfferMenu(int containerId, Inventory inventory, ServerPlayer player) {
        super(MenuType.GENERIC_9x3, 3, containerId, inventory, player);
        render();
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new NewOfferMenu(id, inventory, player),
                Lang.msg("editor.new.title")));
    }

    @Override
    protected void onClick(int slot, boolean shift, boolean right, ItemStack carried) {
        int index = slot - FIRST_SLOT;
        if (index >= 0 && index < TYPES.length) {
            ShopOffer offer = ShopManager.create(TYPES[index], carried);
            ShopManager.offers().add(offer);
            ShopManager.save();
            OfferEditMenu.open(player, offer);
        } else if (slot == SLOT_BACK) {
            EditorListMenu.open(player, 0);
        }
    }

    @Override
    protected void render() {
        display.clearContent();
        ShopMenu.fill(display, 0, 27);
        for (int i = 0; i < TYPES.length; i++) {
            ShopOffer sample = new ShopOffer("sample", TYPES[i]);
            String key = "editor.type." + TYPES[i].getSerializedName();
            List<net.minecraft.network.chat.Component> lore = TYPES[i] == OfferType.ITEM
                    ? List.of(Lang.msg("editor.new.item_hint"))
                    : List.of(Lang.msg("editor.new.click"));
            ItemStack icon = TYPES[i] == OfferType.ITEM ? new ItemStack(Items.CHEST) : sample.iconStack();
            if (TYPES[i] == OfferType.UNLOCK) {
                icon = new ItemStack(Items.CRAFTING_TABLE);
            } else if (TYPES[i] == OfferType.COMMAND) {
                icon = new ItemStack(Items.COMMAND_BLOCK);
            }
            display.setItem(FIRST_SLOT + i, ShopMenu.named(icon, Lang.msg(key), lore));
        }
        display.setItem(SLOT_BACK, ShopMenu.named(new ItemStack(Items.ARROW), Lang.msg("editor.back"), List.of()));
    }
}
