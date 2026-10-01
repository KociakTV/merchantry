package pl.ktv.merchantry.market;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import java.util.function.Supplier;

// Kategorie rynku, przypisywane automatycznie na podstawie przedmiotu
public enum MarketCategory {
    ALL(() -> Items.COMPASS),
    WEAPONS(() -> Items.IRON_SWORD),
    ARMOR(() -> Items.IRON_CHESTPLATE),
    TOOLS(() -> Items.IRON_PICKAXE),
    FOOD(() -> Items.COOKED_BEEF),
    BLOCKS(() -> Items.BRICKS),
    MAGIC(() -> Items.ENCHANTED_BOOK),
    MISC(() -> Items.STRING);

    private final Supplier<Item> icon;

    MarketCategory(Supplier<Item> icon) {
        this.icon = icon;
    }

    public Item icon() {
        return icon.get();
    }

    public String key() {
        return "market.category." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean matches(ItemStack stack) {
        return this == ALL || of(stack) == this;
    }

    public static MarketCategory of(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof BowItem || item instanceof CrossbowItem
                || item instanceof TridentItem || item instanceof MaceItem) {
            return WEAPONS;
        }
        if (item instanceof ArmorItem || item instanceof ElytraItem || item instanceof ShieldItem) {
            return ARMOR;
        }
        if (item instanceof DiggerItem || item instanceof ShearsItem || item instanceof FishingRodItem
                || item instanceof FlintAndSteelItem || item instanceof BrushItem) {
            return TOOLS;
        }
        if (item instanceof PotionItem || item instanceof EnchantedBookItem || stack.is(Items.TOTEM_OF_UNDYING)
                || stack.is(Items.EXPERIENCE_BOTTLE)) {
            return MAGIC;
        }
        if (stack.has(DataComponents.FOOD)) {
            return FOOD;
        }
        if (item instanceof BlockItem) {
            return BLOCKS;
        }
        return MISC;
    }
}
