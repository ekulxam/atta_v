package survivalblock.atmosphere.atta_v.common.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;
import survivalblock.atmosphere.atta_v.common.init.AttaVDamageTypes;
import survivalblock.atmosphere.atta_v.common.init.AttaVEntityTypes;
import survivalblock.atmosphere.atta_v.common.init.AttaVGameRules;

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

        // sounds
        translationBuilder.add("subtitles." + AttaVSoundEvents.WANDERER_LEG_LAND.getId().toTranslationKey(), "Wanderer Leg Lands");

        // commands
        translationBuilder.add("commands.attav.recalibratelegs.invalidvehicle", "Vehicle is null or not of type WalkingCubeEntity");
        translationBuilder.add("commands.attav.recalibratelegs.invalidentity", "Entity is null or not of type WalkingCubeEntity");
        translationBuilder.add("commands.attav.recalibratelegs.notplayersource", "The source of the command did not originate from a player");
        translationBuilder.add("commands.attav.recalibratelegs.success", "Successfully recalibrated legs");

        // gamerules
        translationBuilder.add(AttaVGameRules.WANDERER_STOMP_DOES_DAMAGE.getTranslationKey(), "Atta V - Wanderer Stomp Does Damage");
        translationBuilder.add(AttaVGameRules.WANDERER_SEEKS_OUT_PLAYERS.getTranslationKey(), "Atta V - Wanderer Seeks Out Players");
    }
}
