package pl.ktv.merchantry.repair;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import pl.ktv.merchantry.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Naprawa przedmiotów. Cena = procent zużycia (0-100) * tier * stawka z konfiguracji.
public final class Repair {
    // Proste narzędzia bez materiału - liczone jak kamień, żeby naprawa nożyc nie kosztowała jak diamentu
    private static final Set<Item> BASIC_TOOLS = Set.of(Items.SHEARS, Items.FLINT_AND_STEEL, Items.FISHING_ROD,
            Items.CARROT_ON_A_STICK, Items.WARPED_FUNGUS_ON_A_STICK, Items.BRUSH);

    private Repair() {
    }

    public static double tier(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof TieredItem tiered) {
            Tier tier = tiered.getTier();
            if (tier == Tiers.WOOD) return 1;
            if (tier == Tiers.STONE) return 2;
            if (tier == Tiers.GOLD) return 2.5;
            if (tier == Tiers.IRON) return 3;
            if (tier == Tiers.DIAMOND) return 4;
            if (tier == Tiers.NETHERITE) return 5;
        }
        if (item instanceof ArmorItem armor) {
            Holder<ArmorMaterial> material = armor.getMaterial();
            if (is(material, ArmorMaterials.LEATHER)) return 1;
            if (is(material, ArmorMaterials.CHAIN)) return 2;
            if (is(material, ArmorMaterials.GOLD)) return 2.5;
            if (is(material, ArmorMaterials.IRON) || is(material, ArmorMaterials.TURTLE)) return 3;
            if (is(material, ArmorMaterials.DIAMOND)) return 4;
            if (is(material, ArmorMaterials.NETHERITE)) return 5;
        }
        if (BASIC_TOOLS.contains(item)) {
            return 2;
        }
        // Elytra, trójząb, buzdygan, łuk, kusza, tarcza i przedmioty z innych modów
        return Config.SPECIAL_ITEM_TIER.get();
    }

    public static long cost(ItemStack stack) {
        if (!stack.isDamageableItem() || !stack.isDamaged()) {
            return 0;
        }
        double percent = stack.getDamageValue() * 100.0 / stack.getMaxDamage();
        return (long) Math.ceil(percent * tier(stack) * Config.REPAIR_COST_PER_PERCENT.get()) * stack.getCount();
    }

    // Przedmioty do naprawy: przedmiot w ręce albo cały ekwipunek (z pancerzem i drugą ręką)
    public static List<ItemStack> targets(ServerPlayer player, boolean all) {
        List<ItemStack> result = new ArrayList<>();
        if (!all) {
            ItemStack hand = player.getMainHandItem();
            if (cost(hand) > 0) {
                result.add(hand);
            }
            return result;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (cost(stack) > 0) {
                result.add(stack);
            }
        }
        return result;
    }

    public static long totalCost(ServerPlayer player, boolean all) {
        long total = 0;
        for (ItemStack stack : targets(player, all)) {
            total += cost(stack);
        }
        return total;
    }

    public static int repair(ServerPlayer player, boolean all) {
        List<ItemStack> targets = targets(player, all);
        for (ItemStack stack : targets) {
            stack.setDamageValue(0);
        }
        player.inventoryMenu.broadcastChanges();
        return targets.size();
    }

    private static boolean is(Holder<ArmorMaterial> material, Holder<ArmorMaterial> expected) {
        return material.value() == expected.value();
    }
}
