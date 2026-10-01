package pl.ktv.merchantry.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.data.ModAttachments;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Osobny panel boczny dla każdego gracza: nick, saldo, czas do wypłaty i wypłaty do północy.
// Wysyłany pakietami tylko do danego gracza, bez zmieniania scoreboardu serwera.
public final class Sidebar {
    private static final String OBJECTIVE = "merchantry_sb";
    private static final Objective DUMMY = new Objective(new Scoreboard(), OBJECTIVE, ObjectiveCriteria.DUMMY,
            Component.empty(), ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
    // Ostatnio wysłane linie - wysyłamy tylko zmiany
    private static final Map<UUID, String[]> SHOWN = new HashMap<>();

    private Sidebar() {
    }

    // Wywoływane co sekundę dla każdego gracza
    public static void update(ServerPlayer player) {
        boolean visible = Economy.isEnabled() && Config.ENABLE_SIDEBAR.get() && !ModAttachments.get(player).sidebarHidden;
        String[] shown = SHOWN.get(player.getUUID());
        if (!visible) {
            if (shown != null) {
                hide(player);
            }
            return;
        }
        if (shown == null) {
            shown = new String[0];
            Objective objective = new Objective(new Scoreboard(), OBJECTIVE, ObjectiveCriteria.DUMMY,
                    Lang.msg("sidebar.title"), ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
            player.connection.send(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD));
            player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
        }

        List<Component> lines = lines(player);
        String[] current = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            current[i] = lines.get(i).getString();
            if (i >= shown.length || !current[i].equals(shown[i])) {
                // Wynik malejący ustala kolejność linii; liczby są ukryte (BlankFormat)
                player.connection.send(new ClientboundSetScorePacket("line" + i, OBJECTIVE, lines.size() - i,
                        Optional.of(lines.get(i)), Optional.of(BlankFormat.INSTANCE)));
            }
        }
        for (int i = lines.size(); i < shown.length; i++) {
            player.connection.send(new ClientboundResetScorePacket("line" + i, OBJECTIVE));
        }
        SHOWN.put(player.getUUID(), current);
    }

    public static void hide(ServerPlayer player) {
        if (SHOWN.remove(player.getUUID()) != null) {
            player.connection.send(new ClientboundSetObjectivePacket(DUMMY, ClientboundSetObjectivePacket.METHOD_REMOVE));
        }
    }

    public static void clear(UUID player) {
        SHOWN.remove(player);
    }

    private static List<Component> lines(ServerPlayer player) {
        Component nextPayout;
        if (Config.PLAYTIME_REWARD.get() <= 0) {
            nextPayout = Lang.msg("sidebar.payout_off");
        } else if (Earnings.dailyLimitReached(player)) {
            nextPayout = Lang.msg("sidebar.limit_reached");
        } else if (Earnings.isAfk(player)) {
            nextPayout = Lang.msg("sidebar.afk", time(Earnings.secondsToNextPayout(player)));
        } else {
            nextPayout = Lang.msg("sidebar.time", time(Earnings.secondsToNextPayout(player)));
        }
        return List.of(
                Lang.msg("sidebar.player", player.getGameProfile().getName()),
                Lang.msg("sidebar.balance", Economy.format(Economy.getBalance(player))),
                Component.empty(),
                Lang.msg("sidebar.next_payout"),
                nextPayout,
                Lang.msg("sidebar.payouts_left", Earnings.remainingPayoutsToday(player))
        );
    }

    private static String time(int seconds) {
        int hours = seconds / 3600;
        int minutes = seconds % 3600 / 60;
        int secs = seconds % 60;
        return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, secs) : String.format("%d:%02d", minutes, secs);
    }
}
