package pl.ktv.merchantry.compat.curios;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;
import java.util.Optional;

// Sloty Curios przy ładunku keepInventory: zapamiętane i wyczyszczone przy śmierci (więc nie wypadają
// i nie trafiają do grobu), oddane po odrodzeniu na te same miejsca. Używane tylko, gdy Curios jest zainstalowany.
public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static ListTag saveAndClear(ServerPlayer player) {
        ListTag saved = new ListTag();
        Optional<ICuriosItemHandler> inventory = CuriosApi.getCuriosInventory(player);
        if (inventory.isEmpty()) {
            return saved;
        }
        for (Map.Entry<String, ICurioStacksHandler> slot : inventory.get().getCurios().entrySet()) {
            save(player, saved, slot.getKey(), false, slot.getValue().getStacks());
            save(player, saved, slot.getKey(), true, slot.getValue().getCosmeticStacks());
        }
        return saved;
    }

    private static void save(ServerPlayer player, ListTag saved, String slot, boolean cosmetic, IDynamicStackHandler stacks) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putString("slot", slot);
            entry.putBoolean("cosmetic", cosmetic);
            entry.putInt("index", i);
            entry.put("item", stack.save(player.registryAccess()));
            saved.add(entry);
            stacks.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // Oddaje przedmioty na ich sloty; jeśli slotu już nie ma albo jest zajęty - do ekwipunku
    public static void restore(ServerPlayer player, ListTag saved) {
        Optional<ICuriosItemHandler> inventory = CuriosApi.getCuriosInventory(player);
        for (Tag tag : saved) {
            CompoundTag entry = (CompoundTag) tag;
            ItemStack stack = ItemStack.parseOptional(player.registryAccess(), entry.getCompound("item"));
            if (stack.isEmpty()) {
                continue;
            }
            IDynamicStackHandler stacks = inventory
                    .map(handler -> handler.getCurios().get(entry.getString("slot")))
                    .map(handler -> entry.getBoolean("cosmetic") ? handler.getCosmeticStacks() : handler.getStacks())
                    .orElse(null);
            int index = entry.getInt("index");
            if (stacks != null && index < stacks.getSlots() && stacks.getStackInSlot(index).isEmpty()) {
                stacks.setStackInSlot(index, stack);
            } else {
                ItemHandlerHelper.giveItemToPlayer(player, stack);
            }
        }
    }
}
