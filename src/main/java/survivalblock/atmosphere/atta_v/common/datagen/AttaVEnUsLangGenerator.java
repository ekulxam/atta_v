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
        translationBuilder.add("commands.attav.find.entitynotfound", "Entity was not found!");
        translationBuilder.add("commands.attav.find.success", "A Wanderer was found at %1$s, %2$s blocks away");

        translationBuilder.add("commands.attav.path.create.alreadyexists", "An EntityPath with id %1$s already exists!");
        translationBuilder.add("commands.attav.path.create.success", "A new EntityPath with id %1$s was created");
        translationBuilder.add("commands.attav.path.remove.doesnotexist", "An EntityPath with id %1$s does not exist!");
        translationBuilder.add("commands.attav.path.remove.success", "The EntityPath with id %1$s was removed");
        translationBuilder.add("commands.attav.path.color.success", "Set the color of EntityPath %1$s to %2$s");
        translationBuilder.add("commands.attav.path.attach.unsupported", "Entity %1$s is not a Pathfinder");
        translationBuilder.add("commands.attav.path.attach.success", "Pathfinder Entity %1$s started following EntityPath %2$s");
        translationBuilder.add("commands.attav.path.detach.success", "Pathfinder Entity %1$s stopped following its EntityPath");
        translationBuilder.add("commands.attav.path.nodes.add.indexoutofbounds", "An error occurred while trying to create a new EntityPath node : %1$s");
        translationBuilder.add("commands.attav.path.nodes.add.success", "Successfully added node at %1$s with index %2$s to EntityPath %3$s");
        translationBuilder.add("commands.attav.path.nodes.remove.indexoutofbounds", "An error occurred while trying to remove an EntityPath node : %1$s");
        translationBuilder.add("commands.attav.path.nodes.remove.success", "Successfully removed node with index %1$s from EntityPath %2$s");
        translationBuilder.add("commands.attav.path.remove.toomanyentities", "While trying to remove the path from all Pathfinders, an error occurred : too many iterations (%1$s >= %2$s)");
        translationBuilder.add("commands.attav.path.nodes.get.indexoutofbounds", "An error occurred while trying to get an EntityPath node : %1$s");
        translationBuilder.add("commands.attav.path.nodes.get.success", "Node with index %1$s of EntityPath %2$s has position %3$s");

        translationBuilder.add("commands.attavclient.showentitypaths.get", "Show Entity Paths is currently set to %1$s");
        translationBuilder.add("commands.attavclient.showentitypaths.set", "Show Entity Paths has been set to %1$s");

        // gamerules
        translationBuilder.add(AttaVGameRules.WANDERER_STOMP_DOES_DAMAGE.getTranslationKey(), "Atta V - Wanderer Stomp Does Damage");
        translationBuilder.add(AttaVGameRules.WANDERER_SEEKS_OUT_PLAYERS.getTranslationKey(), "Atta V - Wanderer Seeks Out Players");
        translationBuilder.add(AttaVGameRules.WANDERER_FLINGS_PLAYERS.getTranslationKey(), "Atta V - Wanderer Flings Players");
        translationBuilder.add(AttaVGameRules.PLAYERS_CAN_RIDE_WANDERERS.getTranslationKey(), "Atta V - Players Can Ride Wanderers");
    }
}
