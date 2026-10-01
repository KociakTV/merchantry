package pl.ktv.merchantry.data;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import pl.ktv.merchantry.Merchantry;

// Dane dołączane do graczy (zapisywane z graczem, nie są synchronizowane z klientem)
@SuppressWarnings("UnstableApiUsage")
public final class ModAttachments {
    public static final AttachmentType<PlayerData> PLAYER_DATA = AttachmentRegistry.create(
            ResourceLocation.fromNamespaceAndPath(Merchantry.MOD_ID, "player_data"),
            builder -> builder
                    .initializer(PlayerData::new)
                    .persistent(PlayerData.CODEC)
                    .copyOnDeath());

    private ModAttachments() {
    }

    // Wymusza rejestrację typu przy starcie moda (przed wczytaniem pierwszego gracza)
    public static void init() {
    }

    public static PlayerData get(Player player) {
        return player.getAttachedOrCreate(PLAYER_DATA);
    }
}
