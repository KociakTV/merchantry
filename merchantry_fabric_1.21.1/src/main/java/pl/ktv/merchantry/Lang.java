package pl.ktv.merchantry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

// Tłumaczenia po stronie serwera - klient bez moda nie zna naszych kluczy, więc wysyłamy gotowy tekst.
// Język wybierany dla każdego gracza osobno ("auto": polski dla klientów z polskim językiem, inaczej angielski).
// Teksty mogą zawierać kody kolorów §.
public final class Lang {
    public static final String ENGLISH = "en_us";
    public static final String POLISH = "pl_pl";
    public static final String AUTO = "auto";

    private static final Map<String, Map<String, String>> LANGUAGES = new HashMap<>();
    // Język bieżącego odbiorcy - ustawiany na początku obsługi komendy, kliknięcia w okno czy zdarzenia gracza
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private Lang() {
    }

    // Ustawia język dla dalszych komunikatów w tej obsłudze (komenda, kliknięcie, zdarzenie)
    public static void setContext(ServerPlayer player) {
        CONTEXT.set(player == null ? null : languageOf(player));
    }

    // Komunikat w języku konkretnego gracza (np. do odbiorcy przelewu, a nie wykonującego komendę)
    public static MutableComponent msgFor(ServerPlayer player, String key, Object... args) {
        return with(player, () -> msg(key, args));
    }

    public static <T> T with(ServerPlayer player, Supplier<T> action) {
        String previous = CONTEXT.get();
        setContext(player);
        try {
            return action.get();
        } finally {
            CONTEXT.set(previous);
        }
    }

    public static String languageOf(ServerPlayer player) {
        String configured = configuredLanguage();
        if (!AUTO.equals(configured)) {
            return configured;
        }
        String client = player.clientInformation().language();
        return client != null && client.toLowerCase(Locale.ROOT).startsWith("pl") ? POLISH : ENGLISH;
    }

    // Język, gdy nie wiadomo, kto jest odbiorcą (np. konsola, domyślne oferty)
    public static String defaultLanguage() {
        String configured = configuredLanguage();
        return AUTO.equals(configured) ? ENGLISH : configured;
    }

    private static String configuredLanguage() {
        return Config.LANGUAGE.get().toLowerCase(Locale.ROOT);
    }

    public static String get(String key) {
        String language = CONTEXT.get();
        return getIn(language != null ? language : defaultLanguage(), key);
    }

    public static String getIn(String language, String key) {
        String value = entries(language).get(key);
        if (value == null) {
            value = entries(ENGLISH).getOrDefault(key, key);
        }
        return value;
    }

    // Tekst z podstawionymi argumentami %s (bez obsługi komponentów)
    public static String str(String key, Object... args) {
        return format(get(key), args);
    }

    public static String strIn(String language, String key, Object... args) {
        return format(getIn(language, key), args);
    }

    // Tekst zapisany w danych: "@klucz" albo "@klucz:arg1:arg2" jest tłumaczony, zwykły tekst zostaje bez zmian
    public static String resolve(String text) {
        if (text == null || !text.startsWith("@")) {
            return text;
        }
        String[] parts = text.substring(1).split(":");
        Object[] args = new Object[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        return str(parts[0], args);
    }

    private static String format(String template, Object... args) {
        StringBuilder out = new StringBuilder();
        int argIndex = 0;
        int start = 0;
        int idx;
        while ((idx = template.indexOf("%s", start)) >= 0) {
            out.append(template, start, idx);
            Object arg = argIndex < args.length ? args[argIndex++] : "";
            out.append(arg instanceof Component component ? component.getString() : String.valueOf(arg));
            start = idx + 2;
        }
        out.append(template.substring(start));
        return out.toString();
    }

    // Komponent czatu; argumenty-komponenty (np. nazwy przedmiotów) tłumaczy klient
    // i dostają kolor obowiązujący w miejscu wstawienia.
    public static MutableComponent msg(String key, Object... args) {
        String template = get(key);
        MutableComponent result = Component.empty();
        StringBuilder buffer = new StringBuilder();
        ChatFormatting color = null;
        int argIndex = 0;
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '§' && i + 1 < template.length()) {
                ChatFormatting format = ChatFormatting.getByCode(template.charAt(i + 1));
                if (format != null && (format.isColor() || format == ChatFormatting.RESET)) {
                    color = format == ChatFormatting.RESET ? null : format;
                }
                buffer.append(c).append(template.charAt(i + 1));
                i += 2;
            } else if (c == '%' && i + 1 < template.length() && template.charAt(i + 1) == 's') {
                Object arg = argIndex < args.length ? args[argIndex++] : "";
                if (arg instanceof Component component) {
                    if (!buffer.isEmpty()) {
                        result.append(Component.literal(buffer.toString()));
                        buffer.setLength(0);
                    }
                    MutableComponent copy = component.copy();
                    result.append(color != null ? copy.withStyle(color) : copy);
                    // Przywróć kolor dla dalszej części tekstu
                    if (color != null) {
                        buffer.append('§').append(color.getChar());
                    }
                } else {
                    buffer.append(arg);
                }
                i += 2;
            } else {
                buffer.append(c);
                i++;
            }
        }
        if (!buffer.isEmpty()) {
            result.append(Component.literal(buffer.toString()));
        }
        return result;
    }

    // Wszystkie wpisy języka (do migracji starych nazw ofert na klucze)
    public static Map<String, String> entries(String language) {
        return LANGUAGES.computeIfAbsent(language, Lang::load);
    }

    private static Map<String, String> load(String language) {
        Map<String, String> target = new HashMap<>();
        String path = "/assets/" + Merchantry.MOD_ID + "/lang/" + language + ".json";
        try (InputStream in = Lang.class.getResourceAsStream(path)) {
            if (in == null) {
                Merchantry.LOGGER.warn("Brak pliku języka {}, używam {}", language, ENGLISH);
                return target;
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                target.put(entry.getKey(), entry.getValue().getAsString());
            }
        } catch (Exception e) {
            Merchantry.LOGGER.error("Nie udało się wczytać pliku języka {}", path, e);
        }
        return target;
    }
}
