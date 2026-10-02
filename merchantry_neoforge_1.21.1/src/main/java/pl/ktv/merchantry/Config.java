package pl.ktv.merchantry;

import net.neoforged.neoforge.common.ModConfigSpec;

// Ustawienia serwerowe moda (plik config/merchantry-server.toml)
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> LANGUAGE;
    public static final ModConfigSpec.BooleanValue OPS_BYPASS_UNLOCKS;

    public static final ModConfigSpec.BooleanValue ENABLE_CURRENCY;
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_SYMBOL;
    public static final ModConfigSpec.LongValue STARTING_BALANCE;
    public static final ModConfigSpec.ConfigValue<String> SCOREBOARD_OBJECTIVE;
    public static final ModConfigSpec.BooleanValue ENABLE_SIDEBAR;
    public static final ModConfigSpec.ConfigValue<String> PAYOUTS_LEFT_OBJECTIVE;
    public static final ModConfigSpec.ConfigValue<String> NEXT_PAYOUT_OBJECTIVE;

    public static final ModConfigSpec.LongValue ADVANCEMENT_TASK_REWARD;
    public static final ModConfigSpec.LongValue ADVANCEMENT_GOAL_REWARD;
    public static final ModConfigSpec.LongValue ADVANCEMENT_CHALLENGE_REWARD;
    public static final ModConfigSpec.LongValue BIOME_REWARD;
    public static final ModConfigSpec.LongValue DIMENSION_REWARD;
    public static final ModConfigSpec.LongValue PLAYTIME_REWARD;
    public static final ModConfigSpec.IntValue PLAYTIME_INTERVAL_MINUTES;
    public static final ModConfigSpec.LongValue PLAYTIME_DAILY_LIMIT;
    public static final ModConfigSpec.IntValue AFK_TIMEOUT_MINUTES;

    public static final ModConfigSpec.IntValue DEFAULT_HOMES;
    public static final ModConfigSpec.IntValue MAX_HOMES;
    public static final ModConfigSpec.IntValue HOME_COOLDOWN_SECONDS;

    public static final ModConfigSpec.DoubleValue HOME_PRICE_MULTIPLIER;
    public static final ModConfigSpec.IntValue MAX_PURCHASE_QUANTITY;
    public static final ModConfigSpec.IntValue MAX_KEEP_INVENTORY_CHARGES;

    public static final ModConfigSpec.BooleanValue BLOCK_GRAVES;
    public static final ModConfigSpec.BooleanValue HIDE_DEATH_WAYPOINTS;

    public static final ModConfigSpec.BooleanValue ULTIMINE_REQUIRES_UNLOCK;

    public static final ModConfigSpec.BooleanValue ENABLE_MARKET;
    public static final ModConfigSpec.DoubleValue MARKET_FEE_PERCENT;
    public static final ModConfigSpec.IntValue MAX_MARKET_LISTINGS;

    public static final ModConfigSpec.BooleanValue ENABLE_EXCHANGE;
    public static final ModConfigSpec.DoubleValue DOLLARS_PER_SPUR;
    public static final ModConfigSpec.DoubleValue EXCHANGE_FEE_PERCENT;

    public static final ModConfigSpec.DoubleValue REPAIR_COST_PER_PERCENT;
    public static final ModConfigSpec.DoubleValue SPECIAL_ITEM_TIER;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("general");
        LANGUAGE = BUILDER
                .comment("Language of messages: auto (Polish for players with Polish game language, English for everyone else),",
                        "en_us or pl_pl (forces one language for all players)")
                .define("language", "auto");
        OPS_BYPASS_UNLOCKS = BUILDER
                .comment("Whether operators can use unlockable commands (/craft, /anvil...) without buying them")
                .define("opsBypassUnlocks", true);
        BUILDER.pop();

        BUILDER.push("currency");
        ENABLE_CURRENCY = BUILDER
                .comment("Enables the currency ($). When disabled, the shop accepts payment in items (e.g. diamonds).")
                .define("enableCurrency", false);
        CURRENCY_SYMBOL = BUILDER
                .comment("Currency symbol")
                .define("currencySymbol", "$");
        STARTING_BALANCE = BUILDER
                .comment("Starting balance of a new player")
                .defineInRange("startingBalance", 0L, 0L, Long.MAX_VALUE);
        SCOREBOARD_OBJECTIVE = BUILDER
                .comment("Scoreboard objective the balance is copied to, e.g. for datapacks (empty = disabled)")
                .define("scoreboardObjective", "money");
        ENABLE_SIDEBAR = BUILDER
                .comment("Shows each player their own sidebar: name, balance, time to next payout, payouts left today.",
                        "Players can hide it with /sidebar.")
                .define("enableSidebar", true);
        PAYOUTS_LEFT_OBJECTIVE = BUILDER
                .comment("Scoreboard objective with the number of playtime payouts left today, for leaderboard mods (empty = disabled)")
                .define("payoutsLeftObjective", "merchantry_payouts_left");
        NEXT_PAYOUT_OBJECTIVE = BUILDER
                .comment("Scoreboard objective with seconds until the next playtime payout, for leaderboard mods (empty = disabled)")
                .define("nextPayoutObjective", "merchantry_next_payout");
        BUILDER.pop();

        BUILDER.push("earning");
        ADVANCEMENT_TASK_REWARD = BUILDER
                .comment("Reward for a regular (task) advancement, paid once")
                .defineInRange("advancementTaskReward", 10L, 0L, Long.MAX_VALUE);
        ADVANCEMENT_GOAL_REWARD = BUILDER
                .comment("Reward for a goal advancement")
                .defineInRange("advancementGoalReward", 25L, 0L, Long.MAX_VALUE);
        ADVANCEMENT_CHALLENGE_REWARD = BUILDER
                .comment("Reward for a challenge advancement")
                .defineInRange("advancementChallengeReward", 50L, 0L, Long.MAX_VALUE);
        BIOME_REWARD = BUILDER
                .comment("Reward for discovering a biome for the first time")
                .defineInRange("biomeDiscoveryReward", 5L, 0L, Long.MAX_VALUE);
        DIMENSION_REWARD = BUILDER
                .comment("Reward for entering a dimension for the first time")
                .defineInRange("dimensionDiscoveryReward", 50L, 0L, Long.MAX_VALUE);
        PLAYTIME_REWARD = BUILDER
                .comment("Reward for each interval of active playtime")
                .defineInRange("playtimeReward", 10L, 0L, Long.MAX_VALUE);
        PLAYTIME_INTERVAL_MINUTES = BUILDER
                .comment("Length of the active playtime interval in minutes")
                .defineInRange("playtimeIntervalMinutes", 30, 1, 1440);
        PLAYTIME_DAILY_LIMIT = BUILDER
                .comment("Daily limit of playtime earnings (0 = no limit)")
                .defineInRange("playtimeDailyLimit", 100L, 0L, Long.MAX_VALUE);
        AFK_TIMEOUT_MINUTES = BUILDER
                .comment("Minutes without moving after which a player counts as AFK and stops earning")
                .defineInRange("afkTimeoutMinutes", 5, 1, 120);
        BUILDER.pop();

        BUILDER.push("homes");
        DEFAULT_HOMES = BUILDER
                .comment("Number of homes available from the start")
                .defineInRange("defaultHomes", 1, 0, 100);
        MAX_HOMES = BUILDER
                .comment("Maximum number of homes per player (including bought ones)")
                .defineInRange("maxHomes", 10, 1, 100);
        HOME_COOLDOWN_SECONDS = BUILDER
                .comment("Cooldown between home teleports in seconds")
                .defineInRange("homeCooldownSeconds", 30, 0, 86400);
        BUILDER.pop();

        BUILDER.push("shop");
        HOME_PRICE_MULTIPLIER = BUILDER
                .comment("Price multiplier for each next home: price = base_price * multiplier ^ homes_bought")
                .defineInRange("homePriceMultiplier", 1.2, 1.0, 10.0);
        MAX_PURCHASE_QUANTITY = BUILDER
                .comment("Maximum quantity bought in one transaction (multiples of the offer)")
                .defineInRange("maxPurchaseQuantity", 64, 1, 640);
        MAX_KEEP_INVENTORY_CHARGES = BUILDER
                .comment("Maximum number of keepInventory charges a player can hold (0 = no limit)")
                .defineInRange("maxKeepInventoryCharges", 0, 0, 100_000);
        BUILDER.pop();

        BUILDER.push("keepInventory");
        BLOCK_GRAVES = BUILDER
                .comment("No grave when a keepInventory charge saved the inventory (Gravestone, Corpse, You're in Grave Danger",
                        "and other grave mods that use the drops event)")
                .define("blockGraves", true);
        HIDE_DEATH_WAYPOINTS = BUILDER
                .comment("No death waypoint when a keepInventory charge saved the inventory: FTB Chunks always,",
                        "Xaero's Minimap and JourneyMap when the player also has Merchantry installed on the client")
                .define("hideDeathWaypoints", true);
        BUILDER.pop();

        BUILDER.push("market");
        ENABLE_MARKET = BUILDER
                .comment("Player market /market: players list their own items for $ (requires currency)")
                .define("enableMarket", true);
        MARKET_FEE_PERCENT = BUILDER
                .comment("Fee in percent taken from the seller's earnings (0 = no fee)")
                .defineInRange("marketFeePercent", 5.0, 0.0, 100.0);
        MAX_MARKET_LISTINGS = BUILDER
                .comment("Maximum number of active listings per player")
                .defineInRange("maxListingsPerPlayer", 10, 1, 1000);
        BUILDER.pop();

        BUILDER.push("exchange");
        ENABLE_EXCHANGE = BUILDER
                .comment("Exchange /exchange: $ <-> Create: Numismatics (works when Numismatics is installed and currency is enabled)")
                .define("enableExchange", true);
        DOLLARS_PER_SPUR = BUILDER
                .comment("Rate: how many $ one spur (the smallest coin) is worth.",
                        "Coins: spur 1, bevel 8, sprocket 16, cog 64, crown 512, sun 4096 spurs.")
                .defineInRange("dollarsPerSpur", 1.0, 0.0001, 1_000_000.0);
        EXCHANGE_FEE_PERCENT = BUILDER
                .comment("Exchange fee in percent, taken on every exchange (0 = no fee)")
                .defineInRange("exchangeFeePercent", 0.0, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.push("compat");
        ULTIMINE_REQUIRES_UNLOCK = BUILDER
                .comment("FTB Ultimine works only for players who bought the \"ultimine\" unlock in the shop",
                        "(operators too, when opsBypassUnlocks is enabled). false = everyone can use ultimine.")
                .define("ultimineRequiresUnlock", true);
        BUILDER.pop();

        BUILDER.push("repair");
        REPAIR_COST_PER_PERCENT = BUILDER
                .comment("Repair price = damage percent (0-100) * item tier * this rate.",
                        "Tiers: wood/leather 1, stone/chainmail 2, gold 2.5, iron 3, diamond 4, netherite 5.",
                        "Repairs only work when currency is enabled.")
                .defineInRange("repairCostPerPercent", 2.0, 0.0, 1_000_000.0);
        SPECIAL_ITEM_TIER = BUILDER
                .comment("Tier of items without a material: elytra, trident, mace, bow, crossbow, shield, items from other mods")
                .defineInRange("specialItemTier", 4.0, 0.0, 100.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private Config() {
    }
}
