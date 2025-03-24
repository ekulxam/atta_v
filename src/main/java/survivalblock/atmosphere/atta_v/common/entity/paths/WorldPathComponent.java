package survivalblock.atmosphere.atta_v.common.entity.paths;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import survivalblock.atmosphere.atta_v.common.init.AttaVWorldComponents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldPathComponent implements AutoSyncedComponent {

    private final World world;

    public WorldPathComponent(World world) {
        this.world = world;
    }

    public final Map<Identifier, EntityPath> map = new HashMap<>();

    @Override
    public void readFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup) {
        this.map.clear();
        final int size = nbt.getInt("size");
        for (int i = 0; i < size; i++) {
            this.put(EntityPath.createFromNbt(nbt.getCompound("path" + i), wrapperLookup));
        }
    }

    @Override
    public void writeToNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup) {
        List<EntityPath> list = this.getPaths();
        final int size = list.size();
        nbt.putInt("size", size);
        for (int i = 0; i < size; i++) {
            nbt.put("path" + i, list.get(i).writeToNbt(wrapperLookup));
        }
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public void put(EntityPath entityPath) {
        this.map.put(entityPath.id, entityPath);
    }

    public List<EntityPath> getPaths() {
        return this.map.values().stream().toList();
    }

    public void sync() {
        AttaVWorldComponents.WORLD_PATH.sync(this.world);
    }
}
