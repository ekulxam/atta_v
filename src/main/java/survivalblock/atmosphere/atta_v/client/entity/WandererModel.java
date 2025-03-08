package survivalblock.atmosphere.atta_v.client.entity;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

public class WandererModel extends EntityModel<Entity> {

	public final ModelPart cubes;
	public final ModelPart body;

	public WandererModel(ModelPart root) {
		this.cubes = root.getChild("cubes");
		this.body = root.getChild("body");
	}

	@SuppressWarnings("unused")
    public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();

		ModelPartData cubes = modelPartData.addChild("cubes", ModelPartBuilder.create().uv(0, 32).cuboid(-6.5F, 1.5F, 7.5F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F))
				.uv(0, 32).cuboid(-6.5F, -7.5F, 7.5F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F))
				.uv(0, 32).cuboid(2.5F, 1.5F, 7.5F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F))
				.uv(0, 32).cuboid(2.5F, -7.5F, 7.5F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F))
				.uv(0, 32).cuboid(2.5F, 1.5F, -1.5F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F))
				.uv(0, 32).cuboid(2.5F, -7.5F, -1.5F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F))
				.uv(0, 32).cuboid(-6.5F, 1.5F, -1.5F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F))
				.uv(0, 32).cuboid(-6.5F, -7.5F, -1.5F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(-2.0F, 23.0F, -7.0F));

		ModelPartData body = modelPartData.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-6.0F, -16.0F, -10.0F, 16.0F, 16.0F, 16.0F, new Dilation(0.0F))
				.uv(32, 37).cuboid(8.0F, -9.0F, -3.0F, 3.0F, 2.0F, 2.0F, new Dilation(0.0F))
				.uv(32, 37).cuboid(-7.0F, -9.0F, -3.0F, 3.0F, 2.0F, 2.0F, new Dilation(0.0F))
				.uv(32, 32).cuboid(1.0F, -9.0F, 4.0F, 2.0F, 2.0F, 3.0F, new Dilation(0.0F))
				.uv(32, 32).cuboid(1.0F, -9.0F, -11.0F, 2.0F, 2.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-2.0F, 32.0F, 2.0F));
		return TexturedModelData.of(modelData, 128, 128);
	}

	@Override
	public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
		this.cubes.render(matrices, vertexConsumer, light, overlay, color);
		this.body.render(matrices, vertexConsumer, light, overlay, color);
	}
}