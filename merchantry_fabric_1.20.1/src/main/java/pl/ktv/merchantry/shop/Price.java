package pl.ktv.merchantry.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import pl.ktv.merchantry.economy.Economy;

// Konkretna cena do zapłaty: kwota w $ albo liczba przedmiotów
public record Price(boolean money, long amount, Item item) {
    public static Price ofMoney(long amount) {
        return new Price(true, amount, null);
    }

    public static Price ofItems(Item item, long count) {
        return new Price(false, count, item);
    }

    public Component describe() {
        if (money) {
            return Component.literal(Economy.format(amount));
        }
        return Component.literal(amount + "x ").append(item.getDescription());
    }
}
