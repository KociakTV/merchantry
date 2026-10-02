package pl.ktv.merchantry.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// Dane przedmiotów w formacie 1.20.1 (NBT). W wersjach 1.21 to samo robią komponenty danych
// (custom_name, lore, enchantment_glint_override, custom_data, hide_tooltip).
public final class Stacks {
    private static final String DISPLAY = "display";
    private static final String LORE = "Lore";
    private static final String ENCHANTMENTS = "Enchantments";
    private static final String HIDE_FLAGS = "HideFlags";
    // Flagi HideFlags: 1 = zaklęcia, 32 = dodatkowe informacje (efekty mikstur, zaklęcia książek itp.)
    private static final int HIDE_ENCHANTMENTS = 1;
    private static final int HIDE_ADDITIONAL = 32;
    private static final int HIDE_ALL = 255;

    private Stacks() {
    }

    // Własna nazwa bez kursywy (klient dodaje kursywę do zmienionych nazw)
    public static void setName(ItemStack stack, Component name) {
        stack.setHoverName(name.copy().withStyle(style -> style.withItalic(false)));
    }

    public static boolean hasCustomName(ItemStack stack) {
        return stack.hasCustomHoverName();
    }

    public static List<Component> getLore(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        CompoundTag display = stack.getTagElement(DISPLAY);
        if (display == null) {
            return lines;
        }
        for (Tag line : display.getList(LORE, Tag.TAG_STRING)) {
            try {
                Component component = Component.Serializer.fromJson(line.getAsString());
                if (component != null) {
                    lines.add(component);
                }
            } catch (RuntimeException ignored) {
                // Uszkodzony wpis opisu - pomijamy
            }
        }
        return lines;
    }

    public static void setLore(ItemStack stack, List<? extends Component> lines) {
        ListTag lore = new ListTag();
        for (Component line : lines) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        stack.getOrCreateTagElement(DISPLAY).put(LORE, lore);
    }

    // Połysk zaklęcia bez prawdziwego zaklęcia: pusty wpis na liście zaklęć (klient go nie wyświetla)
    public static void setGlint(ItemStack stack, boolean glint) {
        if (glint) {
            if (!stack.isEnchanted()) {
                ListTag fake = new ListTag();
                fake.add(new CompoundTag());
                stack.getOrCreateTag().put(ENCHANTMENTS, fake);
                addHideFlags(stack, HIDE_ENCHANTMENTS);
            }
        } else if (stack.getTag() != null && isFakeGlint(stack.getTag().getList(ENCHANTMENTS, Tag.TAG_COMPOUND))) {
            stack.getTag().remove(ENCHANTMENTS);
        }
    }

    private static boolean isFakeGlint(ListTag enchantments) {
        return enchantments.size() == 1 && enchantments.getCompound(0).isEmpty();
    }

    // Ukrywa dodatkowe linie tooltipu (efekty mikstur, zaklęcia książek itp.), jak hide_additional_tooltip w 1.21
    public static void hideAdditional(ItemStack stack) {
        addHideFlags(stack, HIDE_ADDITIONAL);
    }

    private static void addHideFlags(ItemStack stack, int flags) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(HIDE_FLAGS, tag.getInt(HIDE_FLAGS) | flags);
    }

    // Wypełniacz okna: w 1.20.1 nie da się całkiem ukryć tooltipu, więc nazwa jest pusta
    public static void hideTooltip(ItemStack stack) {
        stack.setHoverName(Component.literal(" "));
        addHideFlags(stack, HIDE_ALL);
    }

    // Własne dane przedmiotu (kopia); pusty tag, gdy przedmiot ich nie ma
    public static CompoundTag customData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.copy();
    }

    public static void putCustomData(ItemStack stack, String key, Tag value) {
        stack.getOrCreateTag().put(key, value);
    }
}
