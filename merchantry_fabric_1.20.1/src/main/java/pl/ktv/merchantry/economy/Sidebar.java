package pl.ktv.merchantry.economy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.data.ModAttachments;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Osobny panel boczny dla każdego gracza: nick, saldo, czas do wypłaty i wypłaty do północy.
// Wysyłany pakietami tylko do danego gracza, bez zmieniania scoreboardu serwera.
// W 1.20.1 linia panelu to nazwa "gracza" w wyniku, więc tekst (z kolorami) wstawiamy jako prefiks
// osobnej drużyny dla każdej linii, a nazwą jest niewidoczny kod koloru. Liczby po prawej stronie
// klient 1.20.1 zawsze pokazuje (ukrywanie ich dodano dopiero w 1.20.3).
public final class Sidebar {
    private static final String OBJECTIVE = "merchantry_sb";
    private static final String TEAM_PREFIX = "merchantry_sb";
    private static final int SIDEBAR_SLOT = 1;
    private static final int MAX_LINES = 15;
    private static final Scoreboard CLIENT_SCOREBOARD = new Scoreboard();
    private static final Objective DUMMY = objective(Component.empty());
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
            Objective objective = objective(Lang.msg("sidebar.title"));
            player.connection.send(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD));
            player.connection.send(new ClientboundSetDisplayObjectivePacket(SIDEBAR_SLOT, objective));
        }

        List<Component> lines = lines(player);
        String[] current = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            current[i] = lines.get(i).getString();
            if (i >= shown.length || !current[i].equals(shown[i])) {
                PlayerTeam team = team(i, lines.get(i));
                // Nowa linia: drużyna z "graczem" linii i wynik (malejący wynik ustala kolejność linii)
                player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, i >= shown.length));
                if (i >= shown.length) {
                    player.connection.send(new ClientboundSetScorePacket(ServerScoreboard.Method.CHANGE, OBJECTIVE,
                            entry(i), lines.size() - i));
                }
            }
        }
        for (int i = lines.size(); i < shown.length; i++) {
            removeLine(player, i);
        }
        SHOWN.put(player.getUUID(), current);
    }

    public static void hide(ServerPlayer player) {
        String[] shown = SHOWN.remove(player.getUUID());
        if (shown != null) {
            player.connection.send(new ClientboundSetObjectivePacket(DUMMY, ClientboundSetObjectivePacket.METHOD_REMOVE));
            for (int i = 0; i < shown.length; i++) {
                player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(team(i, Component.empty())));
            }
        }
    }

    public static void clear(UUID player) {
        SHOWN.remove(player);
    }

    private static void removeLine(ServerPlayer player, int index) {
        player.connection.send(new ClientboundSetScorePacket(ServerScoreboard.Method.REMOVE, OBJECTIVE, entry(index), 0));
        player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(team(index, Component.empty())));
    }

    private static Objective objective(Component title) {
        return new Objective(CLIENT_SCOREBOARD, OBJECTIVE, ObjectiveCriteria.DUMMY, title, ObjectiveCriteria.RenderType.INTEGER);
    }

    // Niewidoczna, unikalna nazwa linii: kod koloru (§0, §1...) + reset
    private static String entry(int index) {
        return "§" + ChatFormatting.values()[index % MAX_LINES].getChar() + "§r";
    }

    private static PlayerTeam team(int index, Component text) {
        PlayerTeam team = new PlayerTeam(CLIENT_SCOREBOARD, TEAM_PREFIX + index);
        team.setPlayerPrefix(text);
        team.getPlayers().add(entry(index));
        return team;
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
