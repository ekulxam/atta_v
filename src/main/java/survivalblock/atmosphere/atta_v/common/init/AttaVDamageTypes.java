package survivalblock.atmosphere.atta_v.common.init;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import survivalblock.atmosphere.atta_v.common.AttaV;

import java.util.HashMap;
import java.util.Map;

public class AttaVDamageTypes {

    public static final RegistryKey<DamageType> WANDERER_STOMP = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, AttaV.id("wanderer_stomp"));

    /**
     * Creates a map with the {@link RegistryKey<DamageType>}s as keys and {@link DamageType}s as values
     * @return an {@link ImmutableMap}
     */
    public static ImmutableMap<RegistryKey<DamageType>, DamageType> asDamageTypes() {
        Map<RegistryKey<DamageType>, DamageType> damageTypes = new HashMap<>();
        damageTypes.put(WANDERER_STOMP, new DamageType("atta_v.wanderer_stomp", 0.1F));
        return ImmutableMap.copyOf(damageTypes);
    }

    public static void bootstrap(Registerable<DamageType> damageTypeRegisterable) {
        asDamageTypes().forEach(damageTypeRegisterable::register);
    }
}
