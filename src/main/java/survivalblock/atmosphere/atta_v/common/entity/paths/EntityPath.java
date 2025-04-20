package survivalblock.atmosphere.atta_v.common.entity.paths;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import survivalblock.atmosphere.atta_v.common.AttaV;

import java.util.ArrayList;
import java.util.List;

public class EntityPath {

    public final Identifier id;
    public final List<Vec3d> nodes;
    public int color = 0xFFFFFFFF;

    public EntityPath(String string) {
        this(Identifier.of(string));
    }

    public EntityPath(Identifier id) {
        this(id, new ArrayList<>());
    }

    protected EntityPath(Identifier id, List<Vec3d> nodes) {
        this.id = id;
        this.nodes = nodes;
    }

    public NbtCompound writeToNbt(RegistryWrapper.WrapperLookup wrapperLookup) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", this.id.toString());
        nbt.putInt("color", this.color);
        final int size = this.nodes.size();
        nbt.putInt("size", size);
        for (int i = 0; i < size; i++) {
            nbt.put("node" + i, Vec3d.CODEC.encodeStart(wrapperLookup.getOps(NbtOps.INSTANCE), this.nodes.get(i)).getOrThrow());
        }
        return nbt;
    }

    public static EntityPath createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup) {
        EntityPath entityPath = new EntityPath(nbt.getString("id"));
        entityPath.color = nbt.getInt("color");
        final int size = nbt.getInt("size");
        entityPath.nodes.clear();
        for (int i = 0; i < size; i++) {
            if (nbt.contains("node" + i)) {
                entityPath.nodes.add(Vec3d.CODEC.parse(wrapperLookup.getOps(NbtOps.INSTANCE), nbt.get("node" + i))
                        .resultOrPartial(error -> AttaV.LOGGER.error("Tried to load invalid Vec3d for node pos: '{}'", error))
                        .orElse(Vec3d.ZERO));
            }
        }
        return entityPath;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EntityPath entityPath)) {
            return false;
        }
        return this.id == entityPath.id;
    }
}
