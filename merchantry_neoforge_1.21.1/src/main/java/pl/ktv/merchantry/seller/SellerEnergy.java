package pl.ktv.merchantry.seller;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import pl.ktv.merchantry.Config;

// Przyjmowanie prądu (FE) przez skrzynkę sprzedającą. NeoForge pozwala dopisać "zdolność" do istniejącego bloku
// (tu: zwykłej skrzynki), więc nie potrzebujemy własnego bloku. Dostają ją tylko skrzynki zarejestrowane
// jako sprzedające; zwykłe skrzynki nadal nie przyjmują energii. Kable i generatory dowolnego moda
// (Create Crafts & Additions, Create: New Age, Mekanism...) widzą ją jak każdy inny odbiornik.
public final class SellerEnergy {
    private SellerEnergy() {
    }

    // Rejestracja na magistrali moda (patrz konstruktor klasy Merchantry)
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BlockEntityType.CHEST, (chest, side) -> forChest(chest));
    }

    // null = ta skrzynka nie jest sprzedająca, więc nie ma magazynu energii
    private static IEnergyStorage forChest(ChestBlockEntity chest) {
        if (!(chest.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        SellerData data = SellerData.get(level.getServer());
        SellerData.Crate crate = data.crates.get(SellerService.key(level, chest.getBlockPos()));
        return crate == null ? null : new CrateEnergy(data, crate);
    }

    // Magazyn energii jednej skrzynki: tylko przyjmuje prąd, nie oddaje go
    private record CrateEnergy(SellerData data, SellerData.Crate crate) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            long free = Config.SELLER_ENERGY_CAPACITY.get() - crate.energy;
            int accepted = (int) Math.max(0, Math.min(Math.min(toReceive, Config.SELLER_MAX_RECEIVE.get()), free));
            if (accepted > 0 && !simulate) {
                crate.energy += accepted;
                data.setDirty();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(crate.energy, Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored() {
            return Config.SELLER_ENERGY_CAPACITY.get();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
