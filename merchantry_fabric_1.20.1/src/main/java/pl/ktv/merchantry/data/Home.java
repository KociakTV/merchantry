package pl.ktv.merchantry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

// Zapisana pozycja domu gracza
public record Home(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
    public static final Codec<Home> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Home::dimension),
            Codec.DOUBLE.fieldOf("x").forGetter(Home::x),
            Codec.DOUBLE.fieldOf("y").forGetter(Home::y),
            Codec.DOUBLE.fieldOf("z").forGetter(Home::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(Home::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(Home::pitch)
    ).apply(instance, Home::new));
}
