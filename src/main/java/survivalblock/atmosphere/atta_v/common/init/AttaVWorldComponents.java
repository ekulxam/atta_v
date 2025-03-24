package survivalblock.atmosphere.atta_v.common.init;

import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;
import survivalblock.atmosphere.atta_v.common.AttaV;
import survivalblock.atmosphere.atta_v.common.entity.paths.WorldPathComponent;

public class AttaVWorldComponents implements WorldComponentInitializer {

    public static final ComponentKey<WorldPathComponent> WORLD_PATH = ComponentRegistry.getOrCreate(AttaV.id("world_path"), WorldPathComponent.class);

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry worldComponentFactoryRegistry) {
        worldComponentFactoryRegistry.register(WORLD_PATH, WorldPathComponent::new);
    }
}
