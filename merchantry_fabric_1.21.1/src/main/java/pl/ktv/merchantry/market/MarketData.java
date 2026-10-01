package pl.ktv.merchantry.market;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Dane rynku zapisywane w świecie (data/merchantry_market.dat):
// aktywne oferty oraz pieniądze i przedmioty czekające na graczy, którzy byli offline
public class MarketData extends SavedData {
    public static final String NAME = "merchantry_market";
    public static final SavedData.Factory<MarketData> FACTORY = new SavedData.Factory<>(MarketData::new, MarketData::load, null);

    public final List<MarketListing> listings = new ArrayList<>();
    public final Map<UUID, Long> pendingMoney = new HashMap<>();
    public final Map<UUID, List<ItemStack>> mailbox = new HashMap<>();

    public static MarketData load(CompoundTag tag, HolderLookup.Provider registries) {
        MarketData data = new MarketData();
        for (Tag entry : tag.getList("listings", Tag.TAG_COMPOUND)) {
            MarketListing listing = MarketListing.load((CompoundTag) entry, registries);
            if (!listing.stack().isEmpty()) {
                data.listings.add(listing);
            }
        }
        CompoundTag money = tag.getCompound("pending_money");
        for (String key : money.getAllKeys()) {
            data.pendingMoney.put(UUID.fromString(key), money.getLong(key));
        }
        CompoundTag mail = tag.getCompound("mailbox");
        for (String key : mail.getAllKeys()) {
            List<ItemStack> items = new ArrayList<>();
            for (Tag item : mail.getList(key, Tag.TAG_COMPOUND)) {
                ItemStack.parse(registries, item).ifPresent(items::add);
            }
            data.mailbox.put(UUID.fromString(key), items);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (MarketListing listing : listings) {
            list.add(listing.save(registries));
        }
        tag.put("listings", list);
        CompoundTag money = new CompoundTag();
        pendingMoney.forEach((uuid, amount) -> money.putLong(uuid.toString(), amount));
        tag.put("pending_money", money);
        CompoundTag mail = new CompoundTag();
        mailbox.forEach((uuid, items) -> {
            ListTag itemList = new ListTag();
            for (ItemStack item : items) {
                if (!item.isEmpty()) {
                    itemList.add(item.save(registries));
                }
            }
            mail.put(uuid.toString(), itemList);
        });
        tag.put("mailbox", mail);
        return tag;
    }

    public MarketListing find(UUID id) {
        for (MarketListing listing : listings) {
            if (listing.id().equals(id)) {
                return listing;
            }
        }
        return null;
    }
}
