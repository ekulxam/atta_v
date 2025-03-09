package survivalblock.atmosphere.atta_v.common.datagen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import survivalblock.atmosphere.atta_v.common.AttaV;

public class AttaVSoundEvents {

    public static final SoundEvent WANDERER_LEG_LAND = SoundEvent.of(AttaV.id("entity.wanderer_leg_land"));

    public static void init() {
        Registry.register(Registries.SOUND_EVENT, WANDERER_LEG_LAND.getId(), WANDERER_LEG_LAND);
    }
}
