package pl.ktv.merchantry.shop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

// Cena w przedmiotach, np. 5 diamentów (liczba może przekraczać rozmiar stacka)
public record ItemPrice(Item item, int count) {
    public static final Codec<ItemPrice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(ItemPrice::item),
            Codec.intRange(1, 1_000_000).fieldOf("count").forGetter(ItemPrice::count)
    ).apply(instance, ItemPrice::new));
}
