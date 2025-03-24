package survivalblock.atmosphere.atta_v.common.init;

import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;
import survivalblock.atmosphere.atta_v.common.AttaV;
import survivalblock.atmosphere.atta_v.common.entity.paths.EntityPathComponent;
import survivalblock.atmosphere.atta_v.common.entity.paths.WorldPathComponent;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;

public class AttaVEntityComponents implements EntityComponentInitializer {

    public static final ComponentKey<EntityPathComponent> ENTITY_PATH = ComponentRegistry.getOrCreate(AttaV.id("entity_path"), EntityPathComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry entityComponentFactoryRegistry) {
        entityComponentFactoryRegistry.registerFor(WalkingCubeEntity.class, ENTITY_PATH, EntityPathComponent::new);
    }
}
