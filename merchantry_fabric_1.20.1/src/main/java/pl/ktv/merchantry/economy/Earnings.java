package pl.ktv.merchantry.economy;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import pl.ktv.merchantry.Config;
import pl.ktv.merchantry.Lang;
import pl.ktv.merchantry.data.ModAttachments;
import pl.ktv.merchantry.data.PlayerData;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Zarabianie $ w sposób trudny do zautomatyzowania: osiągnięcia, odkrywanie świata, aktywna gra
public final class Earnings {
    private static final Map<UUID, Activity> ACTIVITY = new HashMap<>();

    private Earnings() {
    }

    // Wywoływane co sekundę dla każdego gracza, gdy waluta jest włączona
    public static void tick(ServerPlayer player) {
        if (player.isSpectator() || player.isCreative()) {
            return;
        }
        checkBiome(player);
        trackPlaytime(player);
    }

    public static void onAdvancement(ServerPlayer player, Advancement advancement) {
        Optional<DisplayInfo> display = Optional.ofNullable(advancement.getDisplay());
        // Osiągnięcia bez wyświetlania to m.in. odblokowania receptur - pomijamy
        if (display.isEmpty()) {
            return;
        }
        PlayerData data = ModAttachments.get(player);
        if (!data.rewardedAdvancements.add(advancement.getId().toString())) {
            return;
        }
        long reward = switch (display.get().getFrame()) {
            case TASK -> Config.ADVANCEMENT_TASK_REWARD.get();
            case GOAL -> Config.ADVANCEMENT_GOAL_REWARD.get();
            case CHALLENGE -> Config.ADVANCEMENT_CHALLENGE_REWARD.get();
        };
        reward(player, reward, Lang.msg("earn.advancement", Economy.format(reward), display.get().getTitle()));
    }

    public static void onDimension(ServerPlayer player, ResourceKey<Level> dimension, boolean silent) {
        if (player.isSpectator() || player.isCreative()) {
            return;
        }
        PlayerData data = ModAttachments.get(player);
        if (!data.discoveredDimensions.add(dimension.location().toString()) || silent) {
            return;
        }
        long reward = Config.DIMENSION_REWARD.get();
        reward(player, reward, Lang.msg("earn.dimension", Economy.format(reward), dimension.location().toString()));
    }

    public static void clear(UUID player) {
        ACTIVITY.remove(player);
    }

    private static void checkBiome(ServerPlayer player) {
        player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(key -> {
            PlayerData data = ModAttachments.get(player);
            if (data.discoveredBiomes.add(key.location().toString())) {
                long reward = Config.BIOME_REWARD.get();
                Component name = Component.translatable(key.location().toLanguageKey("biome"));
                reward(player, reward, Lang.msg("earn.biome", Economy.format(reward), name));
            }
        });
    }

    private static void trackPlaytime(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Activity activity = ACTIVITY.computeIfAbsent(player.getUUID(), id -> new Activity(player, now));
        if (activity.update(player)) {
            activity.lastActive = now;
        }
        if (now - activity.lastActive > Config.AFK_TIMEOUT_MINUTES.get() * 60_000L) {
            return;
        }

        PlayerData data = ModAttachments.get(player);
        resetDayIfNeeded(data);

        data.activeTicks += 20;
        int intervalTicks = Config.PLAYTIME_INTERVAL_MINUTES.get() * 60 * 20;
        if (data.activeTicks < intervalTicks) {
            return;
        }
        data.activeTicks -= intervalTicks;

        long amount = Config.PLAYTIME_REWARD.get();
        long limit = Config.PLAYTIME_DAILY_LIMIT.get();
        if (limit > 0) {
            amount = Math.min(amount, limit - data.playtimeEarnedToday);
        }
        if (amount <= 0) {
            return;
        }
        data.playtimeEarnedToday += amount;
        reward(player, amount, Lang.msg("earn.playtime", Economy.format(amount)));
        if (limit > 0 && data.playtimeEarnedToday >= limit) {
            player.sendSystemMessage(Lang.msg("earn.playtime_limit"));
        }
    }

    public static boolean isAfk(ServerPlayer player) {
        Activity activity = ACTIVITY.get(player.getUUID());
        return activity != null
                && System.currentTimeMillis() - activity.lastActive > Config.AFK_TIMEOUT_MINUTES.get() * 60_000L;
    }

    // Sekundy aktywnej gry do następnej wypłaty
    public static int secondsToNextPayout(ServerPlayer player) {
        int intervalTicks = Config.PLAYTIME_INTERVAL_MINUTES.get() * 60 * 20;
        return Math.max(0, (intervalTicks - ModAttachments.get(player).activeTicks + 19) / 20);
    }

    // Ile wypłat za czas gry można jeszcze dostać do północy (czas serwera): mniejsza z wartości
    // wynikającej z dziennego limitu i z liczby przedziałów, które zdążą minąć przy ciągłej grze
    public static long remainingPayoutsToday(ServerPlayer player) {
        PlayerData data = ModAttachments.get(player);
        resetDayIfNeeded(data);
        long reward = Config.PLAYTIME_REWARD.get();
        if (reward <= 0) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        long secondsToMidnight = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).getSeconds();
        long intervalSeconds = Config.PLAYTIME_INTERVAL_MINUTES.get() * 60L;
        long first = secondsToNextPayout(player);
        long byTime = secondsToMidnight < first ? 0 : 1 + (secondsToMidnight - first) / intervalSeconds;

        long limit = Config.PLAYTIME_DAILY_LIMIT.get();
        if (limit <= 0) {
            return byTime;
        }
        long left = Math.max(0, limit - data.playtimeEarnedToday);
        long byLimit = (left + reward - 1) / reward;
        return Math.min(byTime, byLimit);
    }

    public static boolean dailyLimitReached(ServerPlayer player) {
        PlayerData data = ModAttachments.get(player);
        resetDayIfNeeded(data);
        long limit = Config.PLAYTIME_DAILY_LIMIT.get();
        return limit > 0 && data.playtimeEarnedToday >= limit;
    }

    private static void resetDayIfNeeded(PlayerData data) {
        long today = LocalDate.now().toEpochDay();
        if (data.playtimeDay != today) {
            data.playtimeDay = today;
            data.playtimeEarnedToday = 0;
        }
    }

    private static void reward(ServerPlayer player, long amount, Component message) {
        if (amount <= 0) {
            return;
        }
        Economy.deposit(player, amount);
        player.sendSystemMessage(message);
    }

    // Ostatnia znana pozycja i obrót gracza - do wykrywania AFK
    private static final class Activity {
        private double x, y, z;
        private float yRot, xRot;
        private long lastActive;

        private Activity(ServerPlayer player, long now) {
            update(player);
            lastActive = now;
        }

        // Zwraca true, jeśli gracz się poruszył lub rozejrzał od ostatniego sprawdzenia
        private boolean update(ServerPlayer player) {
            boolean moved = player.distanceToSqr(x, y, z) > 0.01
                    || Math.abs(player.getYRot() - yRot) > 1.0F
                    || Math.abs(player.getXRot() - xRot) > 1.0F;
            x = player.getX();
            y = player.getY();
            z = player.getZ();
            yRot = player.getYRot();
            xRot = player.getXRot();
            return moved;
        }
    }
}
