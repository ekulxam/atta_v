package survivalblock.atmosphere.atta_v.common.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class AttaVDataGenerator implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(AttaVEnUsLangGenerator::new);
		pack.addProvider(AttaVTagGenerator.AttaVDamageTypeTagGenerator::new);
		pack.addProvider(AttaVTagGenerator.AttaVEntityTypeTagGenerator::new);
	}
}
