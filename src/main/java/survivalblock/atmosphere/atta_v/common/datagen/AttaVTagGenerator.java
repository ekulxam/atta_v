package survivalblock.atmosphere.atta_v.common.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.EntityTypeTags;
import survivalblock.atmosphere.atta_v.common.init.AttaVEntityTypes;
import survivalblock.atmosphere.atta_v.common.init.AttaVTags;

import java.util.concurrent.CompletableFuture;

public class AttaVTagGenerator {

    public static class AttaVDamageTypeTagGenerator extends FabricTagProvider<DamageType> {

        public AttaVDamageTypeTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, RegistryKeys.DAMAGE_TYPE, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
            getOrCreateTagBuilder(AttaVTags.WALL).add(DamageTypes.IN_WALL, DamageTypes.CRAMMING);
        }
    }

    public static class AttaVEntityTypeTagGenerator extends FabricTagProvider.EntityTypeTagProvider {

        public AttaVEntityTypeTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
            getOrCreateTagBuilder(EntityTypeTags.FALL_DAMAGE_IMMUNE).add(AttaVEntityTypes.WANDERER);
        }
    }
}
