package pl.ktv.merchantry.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.data.ModAttachments;
import pl.ktv.merchantry.data.PlayerData;

import java.util.Locale;

// Obsługa waluty ($): saldo gracza, formatowanie i kopia na scoreboardzie
public final class Economy {
    private Economy() {
    }

    public static boolean isEnabled() {
        return Config.ENABLE_CURRENCY.get();
    }

    public static long getBalance(ServerPlayer player) {
        return ModAttachments.get(player).balance;
    }

    public static void deposit(ServerPlayer player, long amount) {
        PlayerData data = ModAttachments.get(player);
        data.balance = Long.MAX_VALUE - data.balance < amount ? Long.MAX_VALUE : data.balance + amount;
        syncScoreboard(player);
    }

    public static boolean withdraw(ServerPlayer player, long amount) {
        PlayerData data = ModAttachments.get(player);
        if (data.balance < amount) {
            return false;
        }
        data.balance -= amount;
        syncScoreboard(player);
        return true;
    }

    public static void setBalance(ServerPlayer player, long amount) {
        ModAttachments.get(player).balance = Math.max(0, amount);
        syncScoreboard(player);
    }

    public static String format(long amount) {
        return String.format(Locale.ROOT, "%,d", amount).replace(',', ' ') + " " + Config.CURRENCY_SYMBOL.get();
    }

    // Kopiuje saldo do celu scoreboardu, żeby dało się je wyświetlić lub użyć w datapackach
    public static void syncScoreboard(ServerPlayer player) {
        String name = Config.SCOREBOARD_OBJECTIVE.get();
        if (!isEnabled() || name.isBlank()) {
            return;
        }
        ServerScoreboard scoreboard = player.server.getScoreboard();
        Objective objective = scoreboard.getObjective(name);
        if (objective == null) {
            objective = scoreboard.addObjective(name, ObjectiveCriteria.DUMMY,
                    Component.literal(Config.CURRENCY_SYMBOL.get()), ObjectiveCriteria.RenderType.INTEGER, true, null);
        }
        long balance = getBalance(player);
        scoreboard.getOrCreatePlayerScore(player, objective).set((int) Math.min(balance, Integer.MAX_VALUE));
    }

    // Wypłaty za czas gry jako wyniki scoreboardu - dla modów tablic wyników (leaderboardów) i datapacków.
    // Wywoływane co sekundę; wynik zapisywany tylko przy zmianie.
    public static void syncPayoutScores(ServerPlayer player) {
        if (!isEnabled()) {
            return;
        }
        setScore(player, Config.PAYOUTS_LEFT_OBJECTIVE.get(), "scoreboard.payouts_left",
                (int) Math.min(Earnings.remainingPayoutsToday(player), Integer.MAX_VALUE));
        int next = Config.PLAYTIME_REWARD.get() <= 0 || Earnings.dailyLimitReached(player)
                ? 0 : Earnings.secondsToNextPayout(player);
        setScore(player, Config.NEXT_PAYOUT_OBJECTIVE.get(), "scoreboard.next_payout", next);
    }

    private static void setScore(ServerPlayer player, String name, String titleKey, int value) {
        if (name.isBlank()) {
            return;
        }
        ServerScoreboard scoreboard = player.server.getScoreboard();
        Objective objective = scoreboard.getObjective(name);
        if (objective == null) {
            objective = scoreboard.addObjective(name, ObjectiveCriteria.DUMMY,
                    Component.literal(Lang.strIn(Lang.defaultLanguage(), titleKey)), ObjectiveCriteria.RenderType.INTEGER,
                    true, null);
        }
        var score = scoreboard.getOrCreatePlayerScore(player, objective);
        if (score.get() != value) {
            score.set(value);
        }
    }
}
