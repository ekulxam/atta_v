package survivalblock.atmosphere.atta_v.common.entity.paths;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import survivalblock.atmosphere.atta_v.common.init.AttaVEntityComponents;
import survivalblock.atmosphere.atta_v.common.init.AttaVWorldComponents;

public class EntityPathComponent implements AutoSyncedComponent {

    private final Entity pathfinder;

    public EntityPathComponent(Entity pathfinder) {
        this.pathfinder = pathfinder;
    }

    public @Nullable EntityPath entityPath;
    public int nodeIndex = -1;

    public void sync() {
        AttaVEntityComponents.ENTITY_PATH.sync(this.pathfinder);
    }

    @Override
    public void readFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup) {
        if (nbt.contains("pathId")) {
            this.entityPath = AttaVWorldComponents.WORLD_PATH.get(this.pathfinder.getWorld()).map.get(Identifier.of(nbt.getString("pathId")));
            this.nodeIndex = nbt.getInt("nodeIndex");
        } else {
            this.entityPath = null;
            this.nodeIndex = -1;
        }
    }

    @Override
    public void writeToNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup) {
        if (this.entityPath != null) {
            nbt.putString("pathId", this.entityPath.id.toString());
            nbt.putInt("nodeIndex", this.nodeIndex);
        }
    }
}
