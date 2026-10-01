package pl.ktv.merchantry.unlock;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.data.ModAttachments;

import java.util.function.Supplier;

// Komendy kupowane na zawsze, które otwierają okno bloku bez stawiania go
public enum Unlock {
    CRAFT("craft", () -> Items.CRAFTING_TABLE, "container.crafting"),
    ANVIL("anvil", () -> Items.ANVIL, "container.repair"),
    ENDERCHEST("enderchest", () -> Items.ENDER_CHEST, "container.enderchest"),
    STONECUTTER("stonecutter", () -> Items.STONECUTTER, "container.stonecutter"),
    SMITHING("smithing", () -> Items.SMITHING_TABLE, "container.upgrade"),
    GRINDSTONE("grindstone", () -> Items.GRINDSTONE, "container.grindstone_title"),
    LOOM("loom", () -> Items.LOOM, "container.loom"),
    CARTOGRAPHY("cartography", () -> Items.CARTOGRAPHY_TABLE, "container.cartography_table");

    private final String id;
    private final Supplier<Item> icon;
    private final String titleKey;

    Unlock(String id, Supplier<Item> icon, String titleKey) {
        this.id = id;
        this.icon = icon;
        this.titleKey = titleKey;
    }

    public String id() {
        return id;
    }

    public Item icon() {
        return icon.get();
    }

    public static Unlock byId(String id) {
        for (Unlock unlock : values()) {
            if (unlock.id.equals(id)) {
                return unlock;
            }
        }
        return null;
    }

    public boolean has(ServerPlayer player) {
        return ModAttachments.get(player).unlocks.contains(id)
                || (Config.OPS_BYPASS_UNLOCKS.get() && player.hasPermissions(2));
    }

    public void open(ServerPlayer player) {
        // Pozycja gracza jako "pozycja bloku" - potrzebna np. do zwracania przedmiotów po zamknięciu okna.
        // stillValid() zawsze true, bo w tym miejscu nie ma prawdziwego bloku.
        ContainerLevelAccess access = ContainerLevelAccess.create(player.level(), player.blockPosition());
        MenuConstructor constructor = switch (this) {
            case CRAFT -> (id, inv, p) -> new CraftingMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player p) {
                    return true;
                }
            };
            case ANVIL -> (id, inv, p) -> new AnvilMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player p) {
                    return true;
                }
            };
            case ENDERCHEST -> (id, inv, p) -> {
                player.getEnderChestInventory().setActiveChest(null);
                return ChestMenu.threeRows(id, inv, player.getEnderChestInventory());
            };
            case STONECUTTER -> (id, inv, p) -> new StonecutterMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player p) {
                    return true;
                }
            };
            case SMITHING -> (id, inv, p) -> new SmithingMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player p) {
                    return true;
                }
            };
            case GRINDSTONE -> (id, inv, p) -> new GrindstoneMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player p) {
                    return true;
                }
            };
            case LOOM -> (id, inv, p) -> new LoomMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player p) {
                    return true;
                }
            };
            case CARTOGRAPHY -> (id, inv, p) -> new CartographyTableMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player p) {
                    return true;
                }
            };
        };
        player.openMenu(new SimpleMenuProvider(constructor, Component.translatable(titleKey)));
    }
}
