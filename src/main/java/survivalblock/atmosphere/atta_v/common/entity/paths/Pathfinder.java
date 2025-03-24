package survivalblock.atmosphere.atta_v.common.entity.paths;

import org.jetbrains.annotations.Nullable;

/**
 * Classes that implement this should extend {@link net.minecraft.entity.Entity}
 * These entities should be able to follow paths.
 */
public interface Pathfinder {

    void followPath(@Nullable EntityPath path);
}
