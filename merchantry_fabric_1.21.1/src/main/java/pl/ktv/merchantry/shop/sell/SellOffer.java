package pl.ktv.merchantry.shop.sell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

// Oferta skupu: serwer płaci price $ za 1 sztukę przedmiotu
public record SellOffer(Item item, long price) {
    public static final Codec<SellOffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(SellOffer::item),
            Codec.LONG.fieldOf("price").forGetter(SellOffer::price)
    ).apply(instance, SellOffer::new));
}
