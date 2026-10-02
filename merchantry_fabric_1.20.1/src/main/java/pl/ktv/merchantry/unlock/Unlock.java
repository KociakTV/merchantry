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

import pl.ktv.merchantry.compat.Compat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Odblokowania kupowane na zawsze: komendy otwierające okno bloku bez stawiania go
// oraz prawa do funkcji innych modów (np. FTB Ultimine - bez komendy)
public enum Unlock {
    CRAFT("craft", () -> Items.CRAFTING_TABLE, "container.crafting"),
    ANVIL("anvil", () -> Items.ANVIL, "container.repair"),
    ENDERCHEST("enderchest", () -> Items.ENDER_CHEST, "container.enderchest"),
    STONECUTTER("stonecutter", () -> Items.STONECUTTER, "container.stonecutter"),
    SMITHING("smithing", () -> Items.SMITHING_TABLE, "container.upgrade"),
    GRINDSTONE("grindstone", () -> Items.GRINDSTONE, "container.grindstone_title"),
    LOOM("loom", () -> Items.LOOM, "container.loom"),
    CARTOGRAPHY("cartography", () -> Items.CARTOGRAPHY_TABLE, "container.cartography_table"),
    // Prawo do używania FTB Ultimine (sprawdza je UltimineCompat); tylko gdy mod jest zainstalowany
    ULTIMINE("ultimine", () -> Items.DIAMOND_PICKAXE, null, Compat.FTB_ULTIMINE);

    private final String id;
    private final Supplier<Item> icon;
    private final String titleKey;
    private final Compat requiredMod;

    Unlock(String id, Supplier<Item> icon, String titleKey) {
        this(id, icon, titleKey, null);
    }

    Unlock(String id, Supplier<Item> icon, String titleKey, Compat requiredMod) {
        this.id = id;
        this.icon = icon;
        this.titleKey = titleKey;
        this.requiredMod = requiredMod;
    }

    // Odblokowania dostępne na tym serwerze (bez tych, których mod nie jest zainstalowany)
    public static List<Unlock> available() {
        List<Unlock> list = new ArrayList<>();
        for (Unlock unlock : values()) {
            if (unlock.requiredMod == null || unlock.requiredMod.isLoaded()) {
                list.add(unlock);
            }
        }
        return list;
    }

    // Czy odblokowanie ma własną komendę otwierającą okno (/craft, /anvil...)
    public boolean hasCommand() {
        return titleKey != null;
    }

    // Domyślny opis oferty (klucz tłumaczenia)
    public String defaultDescription() {
        return hasCommand() ? "@default.unlock.desc:" + id : "@default.unlock." + id + ".desc";
    }

    public String id() {
        return id;
    }

    public Item icon() {
        return icon.get();
    }

    public static Unlock byId(String id) {
        for (Unlock unlock : available()) {
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
            case ULTIMINE -> null;
        };
        if (constructor == null) {
            return;
        }
        player.openMenu(new SimpleMenuProvider(constructor, Component.translatable(titleKey)));
    }
}
