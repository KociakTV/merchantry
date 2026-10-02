package pl.ktv.merchantry.compat.trinkets;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pl.ktv.merchantry.util.PlayerItems;

import java.util.Map;
import java.util.Optional;

// Sloty Trinkets przy ładunku keepInventory: zapamiętane i wyczyszczone przy śmierci (więc nie wypadają
// i nie trafiają do grobu), oddane po odrodzeniu na te same miejsca. Używane tylko, gdy Trinkets jest zainstalowany.
public final class TrinketsCompat {
    private TrinketsCompat() {
    }

    public static ListTag saveAndClear(ServerPlayer player) {
        ListTag saved = new ListTag();
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) {
            return saved;
        }
        for (Map.Entry<String, Map<String, TrinketInventory>> group : component.get().getInventory().entrySet()) {
            for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
                TrinketInventory inventory = slot.getValue();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    CompoundTag entry = new CompoundTag();
                    entry.putString("group", group.getKey());
                    entry.putString("slot", slot.getKey());
                    entry.putInt("index", i);
                    entry.put("item", stack.save(new CompoundTag()));
                    saved.add(entry);
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        return saved;
    }

    // Oddaje przedmioty na ich sloty; jeśli slotu już nie ma albo jest zajęty - do ekwipunku
    public static void restore(ServerPlayer player, ListTag saved) {
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(player);
        for (Tag tag : saved) {
            CompoundTag entry = (CompoundTag) tag;
            ItemStack stack = ItemStack.of(entry.getCompound("item"));
            if (stack.isEmpty()) {
                continue;
            }
            TrinketInventory inventory = component
                    .map(c -> c.getInventory().get(entry.getString("group")))
                    .map(slots -> slots.get(entry.getString("slot")))
                    .orElse(null);
            int index = entry.getInt("index");
            if (inventory != null && index < inventory.getContainerSize() && inventory.getItem(index).isEmpty()) {
                inventory.setItem(index, stack);
            } else {
                PlayerItems.give(player, stack);
            }
        }
    }
}
