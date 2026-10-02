package pl.ktv.merchantry.economy;

import pl.ktv.merchantry.util.Stacks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.Lang;

import java.util.List;
import java.util.UUID;

// Czek - zwykły papier z zapisaną kwotą. Pieniądze są pobierane przy wystawieniu,
// więc czek jest zawsze pokryty i można go przekazać innemu graczowi.
public final class Cheque {
    private static final String TAG = Merchantry.MOD_ID + "_cheque";

    private Cheque() {
    }

    public static ItemStack create(ServerPlayer issuer, long amount) {
        CompoundTag cheque = new CompoundTag();
        cheque.putLong("amount", amount);
        cheque.putString("issuer", issuer.getGameProfile().getName());
        cheque.putUUID("issuer_uuid", issuer.getUUID());
        // Unikalny identyfikator - czeki się nie stackują
        cheque.putUUID("id", UUID.randomUUID());

        // Każdy czek ma inne id, więc i tak się nie stackuje (w 1.20.1 nie ma max_stack_size)
        ItemStack stack = new ItemStack(Items.PAPER);
        Stacks.putCustomData(stack, TAG, cheque);
        Stacks.setGlint(stack, true);
        Stacks.setName(stack, Lang.msg("cheque.name", Economy.format(amount)));
        Stacks.setLore(stack, List.of(
                noItalic(Lang.msg("cheque.lore.amount", Economy.format(amount))),
                noItalic(Lang.msg("cheque.lore.issuer", issuer.getGameProfile().getName())),
                noItalic(Lang.msg("cheque.lore.use"))
        ));
        return stack;
    }

    public static boolean isCheque(ItemStack stack) {
        return stack.is(Items.PAPER) && stack.getTag() != null && stack.getTag().contains(TAG);
    }

    public static void redeem(ServerPlayer player, ItemStack stack) {
        CompoundTag cheque = Stacks.customData(stack).getCompound(TAG);
        long amount = cheque.getLong("amount");
        String issuer = cheque.getString("issuer");
        if (amount <= 0) {
            return;
        }
        stack.shrink(1);
        Economy.deposit(player, amount);
        player.sendSystemMessage(Lang.msg("cheque.redeemed", Economy.format(amount), issuer));
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        Merchantry.LOGGER.info("{} zrealizował czek na {} od {}", player.getGameProfile().getName(), amount, issuer);
    }

    private static Component noItalic(Component component) {
        return component.copy().withStyle(style -> style.withItalic(false));
    }
}
