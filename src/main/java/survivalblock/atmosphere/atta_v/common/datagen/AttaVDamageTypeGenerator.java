package survivalblock.atmosphere.atta_v.common.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import survivalblock.atmosphere.atta_v.common.init.AttaVDamageTypes;

import java.util.concurrent.CompletableFuture;

public class AttaVDamageTypeGenerator extends FabricDynamicRegistryProvider {

    public AttaVDamageTypeGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup, Entries entries) {
        AttaVDamageTypes.asDamageTypes().forEach(entries::add);
    }

    @Override
    public String getName() {
        return "Dynamic Registry for " + RegistryKeys.DAMAGE_TYPE.getValue();
    }
}
