package pl.ktv.merchantry.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

// Dawanie przedmiotów graczowi (odpowiednik ItemHandlerHelper.giveItemToPlayer z NeoForge)
public final class PlayerItems {
    private PlayerItems() {
    }

    // Wkłada przedmiot do ekwipunku; co się nie zmieści, upada pod nogi gracza
    public static void give(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remaining = stack.copy();
        boolean added = player.getInventory().add(remaining);
        if (added || remaining.getCount() < stack.getCount()) {
            player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                    ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
        }
        if (!remaining.isEmpty()) {
            player.drop(remaining, false);
        }
        player.containerMenu.broadcastChanges();
    }
}
