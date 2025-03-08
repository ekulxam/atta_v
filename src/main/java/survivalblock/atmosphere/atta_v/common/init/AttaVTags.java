package survivalblock.atmosphere.atta_v.common.init;

import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import survivalblock.atmosphere.atta_v.common.AttaV;

public class AttaVTags {

    // damage type
    public static final TagKey<DamageType> WALL = TagKey.of(RegistryKeys.DAMAGE_TYPE, AttaV.id("wall"));
}
