package pl.ktv.merchantry.data;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import pl.ktv.merchantry.Merchantry;

import java.util.function.Supplier;

// Rejestr danych dołączanych do graczy (nie jest synchronizowany z klientem)
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Merchantry.MOD_ID);

    public static final Supplier<AttachmentType<PlayerData>> PLAYER_DATA = ATTACHMENT_TYPES.register("player_data",
            () -> AttachmentType.builder(PlayerData::new).serialize(PlayerData.CODEC).copyOnDeath().build());

    private ModAttachments() {
    }

    public static PlayerData get(Player player) {
        return player.getData(PLAYER_DATA);
    }
}
