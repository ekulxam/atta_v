package survivalblock.atmosphere.atta_v.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;
import survivalblock.atmosphere.atta_v.common.init.AttaVTags;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyReturnValue(method = "isInvulnerableTo", at = @At("RETURN"))
    private boolean stopSuffocation(boolean original, DamageSource source) {
        return original || (this.getVehicle() instanceof WalkingCubeEntity && source.isIn(AttaVTags.WALL));
    }
}
