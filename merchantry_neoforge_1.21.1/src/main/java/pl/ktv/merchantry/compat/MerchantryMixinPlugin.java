package pl.ktv.merchantry.compat;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

// Mixiny do innych modów nakładamy tylko wtedy, gdy dany mod jest zainstalowany
public class MerchantryMixinPlugin implements IMixinConfigPlugin {
    private static final Map<String, String> REQUIRED_MODS = Map.of(
            "FtbChunksMixin", "ftbchunks",
            "XaeroDeathpointMixin", "xaerominimap",
            "JourneyMapDeathpointMixin", "journeymap"
    );

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        String modId = REQUIRED_MODS.get(simpleName);
        // Mixiny są nakładane przed załadowaniem modów - lista plików modów jest już znana
        return modId == null || LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
