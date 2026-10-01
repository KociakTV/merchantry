package pl.ktv.merchantry.market;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

// Oferta gracza na rynku: przedmiot (cały stack) za podaną cenę w $
public record MarketListing(UUID id, UUID seller, String sellerName, ItemStack stack, long price, long created) {

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putUUID("seller", seller);
        tag.putString("seller_name", sellerName);
        tag.put("item", stack.save(registries));
        tag.putLong("price", price);
        tag.putLong("created", created);
        return tag;
    }

    public static MarketListing load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parse(registries, tag.getCompound("item")).orElse(ItemStack.EMPTY);
        return new MarketListing(tag.getUUID("id"), tag.getUUID("seller"), tag.getString("seller_name"), stack,
                tag.getLong("price"), tag.getLong("created"));
    }
}
