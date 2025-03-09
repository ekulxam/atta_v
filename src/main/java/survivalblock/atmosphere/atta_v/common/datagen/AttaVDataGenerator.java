package survivalblock.atmosphere.atta_v.common.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;
import survivalblock.atmosphere.atta_v.common.init.AttaVDamageTypes;

public class AttaVDataGenerator implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(AttaVEnUsLangGenerator::new);
		pack.addProvider(AttaVTagGenerator.AttaVDamageTypeTagGenerator::new);
		pack.addProvider(AttaVTagGenerator.AttaVEntityTypeTagGenerator::new);
		pack.addProvider(AttaVDamageTypeGenerator::new);
	}

	@Override
	public void buildRegistry(RegistryBuilder registryBuilder) {
		registryBuilder.addRegistry(RegistryKeys.DAMAGE_TYPE, AttaVDamageTypes::bootstrap);
	}
}
