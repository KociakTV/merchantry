package pl.ktv.merchantry.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import pl.ktv.merchantry.shop.editor.OfferEditMenu;

import java.util.ArrayList;
import java.util.List;

// Opcjonalna część klienta (tylko dla admina): pola edytora ofert są oznaczone w custom_data.
// Po upuszczeniu przedmiotu z JEI/EMI wysyłamy zwykłą komendę /shopconfig drop - serwer nie potrzebuje nic więcej.
public final class EditorDrop {
    // Limit długości komendy w pakiecie czatu; dłuższe dane przedmiotu pomijamy (zostaje samo ID)
    private static final int MAX_COMMAND_LENGTH = 250;

    private EditorDrop() {
    }

    public record Target(Rect2i area, String field, String offer) {
    }

    public static List<Target> targets(AbstractContainerScreen<?> screen) {
        List<Target> targets = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            CustomData data = slot.getItem().get(DataComponents.CUSTOM_DATA);
            if (data == null) {
                continue;
            }
            CompoundTag tag = data.copyTag().getCompound(OfferEditMenu.DROP_TAG);
            if (tag.isEmpty()) {
                continue;
            }
            Rect2i area = new Rect2i(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y, 16, 16);
            targets.add(new Target(area, tag.getString("field"), tag.getString("offer")));
        }
        return targets;
    }

    public static void send(Target target, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || stack.isEmpty()) {
            return;
        }
        String prefix = "shopconfig drop " + target.offer() + " " + target.field() + " ";
        String item = new ItemInput(stack.getItemHolder(), stack.getComponentsPatch()).serialize(minecraft.level.registryAccess());
        if (prefix.length() + item.length() > MAX_COMMAND_LENGTH) {
            item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }
        minecraft.player.connection.sendCommand(prefix + item);
    }
}
