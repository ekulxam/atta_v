package survivalblock.atmosphere.atta_v.common.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;

import java.util.function.Consumer;

public class AttaVCommands {

    public static final SimpleCommandExceptionType NOT_PLAYER_SOURCE = new SimpleCommandExceptionType(Text.translatable("commands.attav.recalibratelegs.notplayersource"));
    public static final SimpleCommandExceptionType INVALID_VEHICLE = new SimpleCommandExceptionType(Text.translatable("commands.attav.recalibratelegs.invalidvehicle"));
    public static final SimpleCommandExceptionType INVALID_ENTITY = new SimpleCommandExceptionType(Text.translatable("commands.attav.recalibratelegs.invalidentity"));

    public static final Consumer<ServerCommandSource> SUCCESS = (source) -> source.sendFeedback(() -> Text.translatable("commands.attav.recalibratelegs.success"), false);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                @SuppressWarnings("unused") CommandRegistryAccess registryAccess,
                                @SuppressWarnings("unused") CommandManager.RegistrationEnvironment environment) {

        LiteralCommandNode<ServerCommandSource> attaVNode = CommandManager.literal("attav").build();

        LiteralCommandNode<ServerCommandSource> recalibrateLegsNode = CommandManager.literal("recalibratelegs")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity player) {
                        Entity entity = player.getVehicle();
                        if (!(entity instanceof WalkingCubeEntity walkingCube)) {
                            throw INVALID_VEHICLE.create();
                        }
                        walkingCube.recalibrateLegs(true);
                        SUCCESS.accept(source);
                        return 1;
                    }
                    throw NOT_PLAYER_SOURCE.create();
                }).then(CommandManager.argument("target", EntityArgumentType.entity())
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            Entity entity = EntityArgumentType.getEntity(context, "target");
                            if (!(entity instanceof WalkingCubeEntity walkingCube)) {
                                throw INVALID_ENTITY.create();
                            }
                            walkingCube.recalibrateLegs(true);
                            SUCCESS.accept(context.getSource());
                            return 1;
                        })
                        .build())
                .build();

        dispatcher.getRoot().addChild(attaVNode);

        attaVNode.addChild(recalibrateLegsNode);
    }
}
