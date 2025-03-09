package survivalblock.atmosphere.atta_v.common.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;
import survivalblock.atmosphere.atta_v.common.init.AttaVDamageTypes;
import survivalblock.atmosphere.atta_v.common.init.AttaVEntityTypes;

import java.util.concurrent.CompletableFuture;

public class AttaVEnUsLangGenerator extends FabricLanguageProvider {

    public AttaVEnUsLangGenerator(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup wrapperLookup, TranslationBuilder translationBuilder) {
        // entity
        translationBuilder.add(AttaVEntityTypes.WANDERER, "All-Terrain Tripodal Attacker");

        // damage type
        translationBuilder.add("death.attack.atta_v.wanderer_stomp", "%1$s was stomped to death by %2$s");
        translationBuilder.add("death.attack.atta_v.wanderer_stomp.player", "%1$s was stomped to death by %2$s");
        translationBuilder.add("death.attack.atta_v.wanderer_stomp.item", "%1$s was stomped to death by %2$s using %3$s");

        translationBuilder.add("subtitles." + AttaVSoundEvents.WANDERER_LEG_LAND.getId().toTranslationKey(), "Wanderer Leg Lands");
    }
}
