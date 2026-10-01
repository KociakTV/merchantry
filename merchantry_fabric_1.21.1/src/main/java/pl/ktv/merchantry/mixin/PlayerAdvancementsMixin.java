package pl.ktv.merchantry.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.ktv.merchantry.event.PlayerEvents;

// Zdobycie osiągnięcia (w NeoForge: AdvancementEvent.AdvancementEarnEvent).
// Nagrody osiągnięcia są przyznawane dokładnie raz - w chwili jego ukończenia.
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void merchantry$onEarn(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
        PlayerEvents.onAdvancement(player, advancement);
    }
}
