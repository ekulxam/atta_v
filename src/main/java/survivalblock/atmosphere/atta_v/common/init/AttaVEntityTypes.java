package survivalblock.atmosphere.atta_v.common.init;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import survivalblock.atmosphere.atta_v.common.AttaV;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;

public class AttaVEntityTypes {

    @SuppressWarnings("RedundantTypeArguments")
    public static final EntityType<WalkingCubeEntity> WANDERER = registerEntity(
            "wanderer",
            EntityType.Builder.<WalkingCubeEntity>create(WalkingCubeEntity::new, SpawnGroup.MISC)
                    .dimensions(3, 3)
                    .eyeHeight(1.5f)
                    .passengerAttachments(0.8f)
                    .maxTrackingRange(16)
    );

    @SuppressWarnings("SameParameterValue")
    private static <T extends Entity> EntityType<T> registerEntity(String name, EntityType.Builder<T> builder) {
        return Registry.register(Registries.ENTITY_TYPE, AttaV.id(name), builder.build());
    }

    public static void init(){

    }
}
