package pl.ktv.merchantry;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// Ustawienia serwerowe moda (plik config/merchantry-server.toml - ten sam format co w wersji NeoForge).
// Fabric nie ma własnego systemu konfiguracji, więc prosty odczyt/zapis TOML jest tutaj.
public final class Config {
    private static final String FILE_NAME = Merchantry.MOD_ID + "-server.toml";
    // Kolejność sekcji i wpisów jak w pliku
    private static final Map<String, List<Value<?>>> SECTIONS = new LinkedHashMap<>();
    private static String currentSection;

    public static final Value<String> LANGUAGE;
    public static final Value<Boolean> OPS_BYPASS_UNLOCKS;

    public static final Value<Boolean> ENABLE_CURRENCY;
    public static final Value<String> CURRENCY_SYMBOL;
    public static final Value<Long> STARTING_BALANCE;
    public static final Value<String> SCOREBOARD_OBJECTIVE;
    public static final Value<Boolean> ENABLE_SIDEBAR;
    public static final Value<String> PAYOUTS_LEFT_OBJECTIVE;
    public static final Value<String> NEXT_PAYOUT_OBJECTIVE;

    public static final Value<Long> ADVANCEMENT_TASK_REWARD;
    public static final Value<Long> ADVANCEMENT_GOAL_REWARD;
    public static final Value<Long> ADVANCEMENT_CHALLENGE_REWARD;
    public static final Value<Long> BIOME_REWARD;
    public static final Value<Long> DIMENSION_REWARD;
    public static final Value<Long> PLAYTIME_REWARD;
    public static final Value<Integer> PLAYTIME_INTERVAL_MINUTES;
    public static final Value<Long> PLAYTIME_DAILY_LIMIT;
    public static final Value<Integer> AFK_TIMEOUT_MINUTES;

    public static final Value<Integer> DEFAULT_HOMES;
    public static final Value<Integer> MAX_HOMES;
    public static final Value<Integer> HOME_COOLDOWN_SECONDS;

    public static final Value<Double> HOME_PRICE_MULTIPLIER;
    public static final Value<Integer> MAX_PURCHASE_QUANTITY;
    public static final Value<Integer> MAX_KEEP_INVENTORY_CHARGES;

    public static final Value<Boolean> BLOCK_GRAVES;
    public static final Value<Boolean> HIDE_DEATH_WAYPOINTS;

    public static final Value<Boolean> ULTIMINE_REQUIRES_UNLOCK;

    public static final Value<Boolean> ENABLE_MARKET;
    public static final Value<Double> MARKET_FEE_PERCENT;
    public static final Value<Integer> MAX_MARKET_LISTINGS;

    public static final Value<Boolean> ENABLE_EXCHANGE;
    public static final Value<Double> DOLLARS_PER_SPUR;
    public static final Value<Double> EXCHANGE_FEE_PERCENT;

    public static final Value<Boolean> ENABLE_SELLER;
    public static final Value<Double> SELLER_TAX_PERCENT;
    public static final Value<Integer> SELLER_MAX_CRATES;
    public static final Value<Integer> SELLER_DAILY_SELLS;
    public static final Value<Boolean> SELLER_ONLY_WHEN_FULL;
    public static final Value<Integer> SELLER_INTERVAL_SECONDS;
    public static final Value<Integer> SELLER_ENERGY_PER_SELL;
    public static final Value<Integer> SELLER_ENERGY_CAPACITY;
    public static final Value<Integer> SELLER_MAX_RECEIVE;
    public static final Value<Integer> SELLER_PAYOUT_DELAY_DAYS;
    public static final Value<String> SELLER_EARNED_OBJECTIVE;

    public static final Value<Double> REPAIR_COST_PER_PERCENT;
    public static final Value<Double> SPECIAL_ITEM_TIER;

    static {
        section("general");
        LANGUAGE = define("language", "auto",
                "Language of messages: auto (Polish for players with Polish game language, English for everyone else),",
                "en_us or pl_pl (forces one language for all players)");
        OPS_BYPASS_UNLOCKS = define("opsBypassUnlocks", true,
                "Whether operators can use unlockable commands (/craft, /anvil...) without buying them");

        section("currency");
        ENABLE_CURRENCY = define("enableCurrency", false,
                "Enables the currency ($). When disabled, the shop accepts payment in items (e.g. diamonds).");
        CURRENCY_SYMBOL = define("currencySymbol", "$",
                "Currency symbol");
        STARTING_BALANCE = defineInRange("startingBalance", 0L, 0L, Long.MAX_VALUE,
                "Starting balance of a new player");
        SCOREBOARD_OBJECTIVE = define("scoreboardObjective", "money",
                "Scoreboard objective the balance is copied to, e.g. for datapacks (empty = disabled)");
        ENABLE_SIDEBAR = define("enableSidebar", true,
                "Shows each player their own sidebar: name, balance, time to next payout, payouts left today.",
                "Players can hide it with /sidebar.");
        PAYOUTS_LEFT_OBJECTIVE = define("payoutsLeftObjective", "merchantry_payouts_left",
                "Scoreboard objective with the number of playtime payouts left today, for leaderboard mods (empty = disabled)");
        NEXT_PAYOUT_OBJECTIVE = define("nextPayoutObjective", "merchantry_next_payout",
                "Scoreboard objective with seconds until the next playtime payout, for leaderboard mods (empty = disabled)");

        section("earning");
        ADVANCEMENT_TASK_REWARD = defineInRange("advancementTaskReward", 10L, 0L, Long.MAX_VALUE,
                "Reward for a regular (task) advancement, paid once");
        ADVANCEMENT_GOAL_REWARD = defineInRange("advancementGoalReward", 25L, 0L, Long.MAX_VALUE,
                "Reward for a goal advancement");
        ADVANCEMENT_CHALLENGE_REWARD = defineInRange("advancementChallengeReward", 50L, 0L, Long.MAX_VALUE,
                "Reward for a challenge advancement");
        BIOME_REWARD = defineInRange("biomeDiscoveryReward", 5L, 0L, Long.MAX_VALUE,
                "Reward for discovering a biome for the first time");
        DIMENSION_REWARD = defineInRange("dimensionDiscoveryReward", 50L, 0L, Long.MAX_VALUE,
                "Reward for entering a dimension for the first time");
        PLAYTIME_REWARD = defineInRange("playtimeReward", 10L, 0L, Long.MAX_VALUE,
                "Reward for each interval of active playtime");
        PLAYTIME_INTERVAL_MINUTES = defineInRange("playtimeIntervalMinutes", 30, 1, 1440,
                "Length of the active playtime interval in minutes");
        PLAYTIME_DAILY_LIMIT = defineInRange("playtimeDailyLimit", 100L, 0L, Long.MAX_VALUE,
                "Daily limit of playtime earnings (0 = no limit)");
        AFK_TIMEOUT_MINUTES = defineInRange("afkTimeoutMinutes", 5, 1, 120,
                "Minutes without moving after which a player counts as AFK and stops earning");

        section("homes");
        DEFAULT_HOMES = defineInRange("defaultHomes", 1, 0, 100,
                "Number of homes available from the start");
        MAX_HOMES = defineInRange("maxHomes", 10, 1, 100,
                "Maximum number of homes per player (including bought ones)");
        HOME_COOLDOWN_SECONDS = defineInRange("homeCooldownSeconds", 30, 0, 86400,
                "Cooldown between home teleports in seconds");

        section("shop");
        HOME_PRICE_MULTIPLIER = defineInRange("homePriceMultiplier", 1.2, 1.0, 10.0,
                "Price multiplier for each next home: price = base_price * multiplier ^ homes_bought");
        MAX_PURCHASE_QUANTITY = defineInRange("maxPurchaseQuantity", 64, 1, 640,
                "Maximum quantity bought in one transaction (multiples of the offer)");
        MAX_KEEP_INVENTORY_CHARGES = defineInRange("maxKeepInventoryCharges", 0, 0, 100_000,
                "Maximum number of keepInventory charges a player can hold (0 = no limit)");

        section("keepInventory");
        BLOCK_GRAVES = define("blockGraves", true,
                "No grave when a keepInventory charge saved the inventory (Universal Graves, You're in Grave Danger).",
                "Other grave mods (Gravestones, Forgotten Graves) get an empty inventory anyway.");
        HIDE_DEATH_WAYPOINTS = define("hideDeathWaypoints", true,
                "No death waypoint when a keepInventory charge saved the inventory: FTB Chunks always,",
                "Xaero's Minimap and JourneyMap when the player also has Merchantry installed on the client");

        section("market");
        ENABLE_MARKET = define("enableMarket", true,
                "Player market /market: players list their own items for $ (requires currency)");
        MARKET_FEE_PERCENT = defineInRange("marketFeePercent", 5.0, 0.0, 100.0,
                "Fee in percent taken from the seller's earnings (0 = no fee)");
        MAX_MARKET_LISTINGS = defineInRange("maxListingsPerPlayer", 10, 1, 1000,
                "Maximum number of active listings per player");

        section("exchange");
        ENABLE_EXCHANGE = define("enableExchange", true,
                "Exchange /exchange: $ <-> Create: Numismatics (works when Numismatics is installed and currency is enabled)");
        DOLLARS_PER_SPUR = defineInRange("dollarsPerSpur", 1.0, 0.0001, 1_000_000.0,
                "Rate: how many $ one spur (the smallest coin) is worth.",
                "Coins: spur 1, bevel 8, sprocket 16, cog 64, crown 512, sun 4096 spurs.");
        EXCHANGE_FEE_PERCENT = defineInRange("exchangeFeePercent", 0.0, 0.0, 100.0,
                "Exchange fee in percent, taken on every exchange (0 = no fee)");

        section("compat");
        ULTIMINE_REQUIRES_UNLOCK = define("ultimineRequiresUnlock", true,
                "FTB Ultimine works only for players who bought the \"ultimine\" unlock in the shop",
                "(operators too, when opsBypassUnlocks is enabled). false = everyone can use ultimine.");

        section("seller");
        ENABLE_SELLER = define("enableSeller", false,
                "Selling chests: a normal chest with custom data that sells its whole contents at once at the server's",
                "buy-back prices (config/merchantry_sell.json). Disabled by default. Works only when currency is enabled.",
                "The shop offers for the chest are added the first time the server starts with this enabled.",
                "IMPORTANT: a selling chest works only when it receives energy, so the modpack needs a mod that produces",
                "energy through Team Reborn Energy (Create Crafts & Additions, TechReborn...) - unless sellerEnergyPerSell = 0.");
        SELLER_TAX_PERCENT = defineInRange("sellerTaxPercent", 40.0, 0.0, 100.0,
                "Tax in percent taken from every sale of a selling chest (0 = no tax).",
                "Keep it high - the farm behind the chest produces items for free.");
        SELLER_MAX_CRATES = defineInRange("sellerMaxCratesPerPlayer", 4, 1, 1000,
                "Maximum number of selling chests per player");
        SELLER_DAILY_SELLS = defineInRange("sellerDailySells", 0, 0, 1_000_000,
                "Daily limit of sells per player (all their chests together). One sell = one chest sold whole.",
                "0 = the same as sellerMaxCratesPerPlayer (the limit then follows that value automatically).",
                "If you set it yourself, keep it equal to or greater than sellerMaxCratesPerPlayer,",
                "otherwise some chests will stand idle for part of the day.",
                "Players can buy extra daily sells in the shop (offer seller_extra_sell). The counter resets at midnight (server time).");
        SELLER_ONLY_WHEN_FULL = define("sellerOnlyWhenFull", true,
                "true = a chest sells only when it is full (no free slot), so a sell is always a whole chest.",
                "false = it sells whenever it holds anything the server buys, which wastes daily sells on tiny batches.");
        SELLER_INTERVAL_SECONDS = defineInRange("sellerIntervalSeconds", 60, 1, 3600,
                "How often (in seconds) every selling chest checks whether it can sell");
        SELLER_ENERGY_PER_SELL = defineInRange("sellerEnergyPerSell", 100_000, 0, 1_000_000_000,
                "Energy one sell costs. The chest takes energy from any mod's cables or generators (Team Reborn Energy).",
                "0 = no power needed (then no energy mod is required).");
        SELLER_ENERGY_CAPACITY = defineInRange("sellerEnergyCapacity", 2_000_000, 1, 1_000_000_000,
                "How much energy a selling chest can store");
        SELLER_MAX_RECEIVE = defineInRange("sellerMaxReceive", 10_000, 1, 1_000_000_000,
                "Maximum energy a selling chest accepts in one transfer (limits how fast it charges)");
        SELLER_PAYOUT_DELAY_DAYS = defineInRange("sellerPayoutDelayDays", 1, 0, 30,
                "Earnings from a given day can be collected as a cheque only after this many days",
                "(real days, server time). Prevents quick money loops. 0 = collect immediately.");
        SELLER_EARNED_OBJECTIVE = define("sellerEarnedObjective", "merchantry_seller_earned",
                "Scoreboard objective with the total money each player earned from selling chests (after tax),",
                "for leaderboard mods and datapacks (empty = disabled)");

        section("repair");
        REPAIR_COST_PER_PERCENT = defineInRange("repairCostPerPercent", 2.0, 0.0, 1_000_000.0,
                "Repair price = damage percent (0-100) * item tier * this rate.",
                "Tiers: wood/leather 1, stone/chainmail 2, gold 2.5, iron 3, diamond 4, netherite 5.",
                "Repairs only work when currency is enabled.");
        SPECIAL_ITEM_TIER = defineInRange("specialItemTier", 4.0, 0.0, 100.0,
                "Tier of items without a material: elytra, trident, mace, bow, crossbow, shield, items from other mods");
    }

    private Config() {
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    // Wczytuje plik; brakujące lub błędne wpisy dostają wartość domyślną i plik jest zapisywany od nowa
    public static synchronized void load() {
        Path path = path();
        boolean rewrite = !Files.exists(path);
        if (!rewrite) {
            Map<String, String> raw;
            try {
                raw = parse(Files.readAllLines(path, StandardCharsets.UTF_8));
            } catch (IOException e) {
                Merchantry.LOGGER.error("Nie udało się wczytać {}, używam wartości domyślnych", path, e);
                raw = Map.of();
            }
            for (Map.Entry<String, List<Value<?>>> section : SECTIONS.entrySet()) {
                for (Value<?> value : section.getValue()) {
                    String text = raw.get(section.getKey() + "." + value.key);
                    if (!value.set(text)) {
                        if (text != null) {
                            Merchantry.LOGGER.warn("Błędna wartość {}.{} = {} w {}, przywracam domyślną {}",
                                    section.getKey(), value.key, text, FILE_NAME, value.defaultValue);
                        }
                        rewrite = true;
                    }
                }
            }
        }
        if (rewrite) {
            save(path);
        }
    }

    private static void save(Path path) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, List<Value<?>>> section : SECTIONS.entrySet()) {
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append('[').append(section.getKey()).append("]\n");
            for (Value<?> value : section.getValue()) {
                for (String line : value.comment) {
                    out.append("\t#").append(line).append('\n');
                }
                if (value.range != null) {
                    out.append("\t# Default: ").append(value.defaultValue).append('\n');
                    out.append("\t# Range: ").append(value.range).append('\n');
                }
                out.append('\t').append(value.key).append(" = ").append(value.serialize()).append('\n');
            }
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Merchantry.LOGGER.error("Nie udało się zapisać {}", path, e);
        }
    }

    // Prosty TOML: [sekcja], klucz = wartość, komentarze #. Wynik: "sekcja.klucz" -> tekst wartości.
    private static Map<String, String> parse(List<String> lines) {
        Map<String, String> result = new HashMap<>();
        String section = "";
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq > 0) {
                result.put(section + "." + line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }
        return result;
    }

    // Usuwa komentarz z końca linii, ale nie ruszamy znaku # w cudzysłowie
    private static String stripComment(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && quoted) {
                i++;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (c == '#' && !quoted) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static void section(String name) {
        currentSection = name;
        SECTIONS.put(name, new ArrayList<>());
    }

    private static <T> Value<T> register(Value<T> value) {
        SECTIONS.get(currentSection).add(value);
        return value;
    }

    private static Value<Boolean> define(String key, boolean defaultValue, String... comment) {
        return register(new Value<>(key, defaultValue, null, comment, text -> switch (text) {
            case "true" -> true;
            case "false" -> false;
            default -> null;
        }));
    }

    private static Value<String> define(String key, String defaultValue, String... comment) {
        return register(new Value<>(key, defaultValue, null, comment, Config::unquote));
    }

    private static Value<Long> defineInRange(String key, long defaultValue, long min, long max, String... comment) {
        return register(new Value<>(key, defaultValue, min + " ~ " + max, comment, text -> {
            long parsed = Long.parseLong(text.replace("_", ""));
            return parsed >= min && parsed <= max ? parsed : null;
        }));
    }

    private static Value<Integer> defineInRange(String key, int defaultValue, int min, int max, String... comment) {
        return register(new Value<>(key, defaultValue, min + " ~ " + max, comment, text -> {
            int parsed = Integer.parseInt(text.replace("_", ""));
            return parsed >= min && parsed <= max ? parsed : null;
        }));
    }

    private static Value<Double> defineInRange(String key, double defaultValue, double min, double max, String... comment) {
        return register(new Value<>(key, defaultValue, min + " ~ " + max, comment, text -> {
            double parsed = Double.parseDouble(text.replace("_", ""));
            return parsed >= min && parsed <= max ? parsed : null;
        }));
    }

    private static String unquote(String text) {
        if (text.length() < 2 || !text.startsWith("\"") || !text.endsWith("\"")) {
            return null;
        }
        return text.substring(1, text.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    // Jedna wartość konfiguracji; get() zwraca wartość z pliku albo domyślną
    public static final class Value<T> {
        private final String key;
        private final T defaultValue;
        private final String range;
        private final String[] comment;
        private final Function<String, T> parser;
        private volatile T value;

        private Value(String key, T defaultValue, String range, String[] comment, Function<String, T> parser) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.range = range;
            this.comment = comment;
            this.parser = parser;
            this.value = defaultValue;
        }

        public T get() {
            return value;
        }

        // Ustawia wartość z tekstu z pliku; false = brak albo błąd (zostaje wartość domyślna)
        private boolean set(String text) {
            T parsed = null;
            if (text != null) {
                try {
                    parsed = parser.apply(text);
                } catch (RuntimeException ignored) {
                    // Błędna liczba - jak brak wartości
                }
            }
            value = parsed != null ? parsed : defaultValue;
            return parsed != null;
        }

        private String serialize() {
            if (value instanceof String text) {
                return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            }
            return String.valueOf(value);
        }
    }
}
