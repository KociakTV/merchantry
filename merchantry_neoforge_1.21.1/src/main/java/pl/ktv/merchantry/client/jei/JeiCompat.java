package pl.ktv.merchantry.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import pl.ktv.merchantry.Merchantry;
import pl.ktv.merchantry.client.EditorDrop;

import java.util.List;
import java.util.Optional;

// Wtyczka JEI: przeciąganie przedmiotu z listy JEI na pola edytora ofert (produkt, ikona, przedmiot ceny)
@JeiPlugin
public class JeiCompat implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Merchantry.MOD_ID, "editor_drop");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(ContainerScreen.class, new IGhostIngredientHandler<>() {
            @Override
            public <I> List<Target<I>> getTargetsTyped(ContainerScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
                Optional<ItemStack> stack = ingredient.getItemStack();
                if (stack.isEmpty()) {
                    return List.of();
                }
                return EditorDrop.targets(screen).stream()
                        .map(target -> (Target<I>) new Target<I>() {
                            @Override
                            public Rect2i getArea() {
                                return target.area();
                            }

                            @Override
                            public void accept(I ignored) {
                                EditorDrop.send(target, stack.get());
                            }
                        })
                        .toList();
            }

            @Override
            public void onComplete() {
            }
        });
    }
}
