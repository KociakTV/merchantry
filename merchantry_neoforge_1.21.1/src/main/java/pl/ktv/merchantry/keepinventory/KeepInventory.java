package pl.ktv.merchantry.keepinventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.compat.Compat;
import pl.ktv.merchantry.compat.curios.CuriosCompat;
import pl.ktv.merchantry.data.ModAttachments;
import pl.ktv.merchantry.data.PlayerData;
import pl.ktv.merchantry.network.DeathSavedPacket;


// Jednorazowy keepInventory na ładunki. Przy śmierci ekwipunek, sloty Curios i XP są zapamiętywane
// i czyszczone (więc nic nie wypada i mody grobów nie mają czego zabrać), a po odrodzeniu oddawane graczowi.
// Reguła gry keepInventory nie jest zmieniana.
public final class KeepInventory {
    private KeepInventory() {
    }

    // Wywoływane przy śmierci, zanim przedmioty wypadną
    public static void onDeath(ServerPlayer player) {
        PlayerData data = ModAttachments.get(player);
        if (data.keepInventoryCharges <= 0 || data.pendingRestore != null
                || player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        data.keepInventoryCharges--;

        CompoundTag snapshot = new CompoundTag();
        snapshot.put("items", player.getInventory().save(new ListTag()));
        if (Compat.CURIOS.isLoaded()) {
            snapshot.put("curios", CuriosCompat.saveAndClear(player));
        }
        snapshot.putInt("xp_level", player.experienceLevel);
        snapshot.putFloat("xp_progress", player.experienceProgress);
        snapshot.putInt("xp_total", player.totalExperience);
        data.pendingRestore = snapshot;

        // Pusty ekwipunek i zerowe XP - nic nie wypadnie na ziemię
        player.getInventory().clearContent();
        player.experienceLevel = 0;
        player.experienceProgress = 0;
        player.totalExperience = 0;

        // Klient z Merchantry nie stworzy waypointu śmierci (Xaero's, JourneyMap) - nie ma po co wracać
        DeathSavedPacket.send(player);

        player.sendSystemMessage(Lang.msg("keepinv.used", data.keepInventoryCharges));
        Merchantry.LOGGER.info("{} zużył ładunek keepInventory (zostało {})", player.getGameProfile().getName(),
                data.keepInventoryCharges);
    }

    // Wywoływane po odrodzeniu: oddaje zapamiętany ekwipunek nowej postaci
    public static void onRespawn(ServerPlayer original, ServerPlayer player) {
        CompoundTag snapshot = ModAttachments.get(original).pendingRestore;
        if (snapshot == null) {
            return;
        }
        player.getInventory().load(snapshot.getList("items", Tag.TAG_COMPOUND));
        if (snapshot.contains("curios")) {
            if (Compat.CURIOS.isLoaded()) {
                CuriosCompat.restore(player, snapshot.getList("curios", Tag.TAG_COMPOUND));
            } else {
                // Curios odinstalowany między śmiercią a odrodzeniem - przedmioty trafiają do ekwipunku
                for (Tag entry : snapshot.getList("curios", Tag.TAG_COMPOUND)) {
                    ItemHandlerHelper.giveItemToPlayer(player, ItemStack.parseOptional(player.registryAccess(),
                            ((CompoundTag) entry).getCompound("item")));
                }
            }
        }
        player.experienceLevel = snapshot.getInt("xp_level");
        player.experienceProgress = snapshot.getFloat("xp_progress");
        player.totalExperience = snapshot.getInt("xp_total");
        ModAttachments.get(player).pendingRestore = null;
    }

    // Czy ostatnią śmierć gracza uratował ładunek keepInventory (od śmierci do odrodzenia).
    // Wtedy nie powstaje grób (Gravestone, Corpse, YIGD) ani waypoint śmierci (FTB Chunks).
    public static boolean isDeathSaved(ServerPlayer player) {
        return ModAttachments.get(player).pendingRestore != null;
    }
}
