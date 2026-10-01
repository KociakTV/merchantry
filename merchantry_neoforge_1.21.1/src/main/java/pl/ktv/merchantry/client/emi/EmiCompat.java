package pl.ktv.merchantry.client.emi;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import pl.ktv.merchantry.client.EditorDrop;

import java.util.List;

// Wtyczka EMI: przeciąganie przedmiotu z listy EMI na pola edytora ofert (produkt, ikona, przedmiot ceny)
@EmiEntrypoint
public class EmiCompat implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addDragDropHandler(ContainerScreen.class, new EmiDragDropHandler<>() {
            @Override
            public boolean dropStack(ContainerScreen screen, EmiIngredient ingredient, int x, int y) {
                List<EmiStack> stacks = ingredient.getEmiStacks();
                if (stacks.isEmpty()) {
                    return false;
                }
                ItemStack stack = stacks.get(0).getItemStack();
                if (stack.isEmpty()) {
                    return false;
                }
                for (EditorDrop.Target target : EditorDrop.targets(screen)) {
                    if (target.area().contains(x, y)) {
                        EditorDrop.send(target, stack);
                        return true;
                    }
                }
                return false;
            }

            // Podświetla pola, na które można upuścić przedmiot
            @Override
            public void render(ContainerScreen screen, EmiIngredient dragged, GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                for (EditorDrop.Target target : EditorDrop.targets(screen)) {
                    Rect2i area = target.area();
                    graphics.fill(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight(), 0x8822CC44);
                }
            }
        });
    }
}
