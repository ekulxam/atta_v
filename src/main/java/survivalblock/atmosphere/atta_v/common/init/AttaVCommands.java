package survivalblock.atmosphere.atta_v.common.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import survivalblock.atmosphere.atta_v.common.entity.paths.EntityPath;
import survivalblock.atmosphere.atta_v.common.entity.paths.EntityPathComponent;
import survivalblock.atmosphere.atta_v.common.entity.paths.Pathfinder;
import survivalblock.atmosphere.atta_v.common.entity.paths.WorldPathComponent;
import survivalblock.atmosphere.atta_v.common.entity.wanderer.WalkingCubeEntity;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

public final class AttaVCommands {

    public static final SimpleCommandExceptionType NOT_PLAYER_SOURCE = new SimpleCommandExceptionType(Text.translatable("commands.attav.recalibratelegs.notplayersource"));
    public static final SimpleCommandExceptionType INVALID_VEHICLE = new SimpleCommandExceptionType(Text.translatable("commands.attav.recalibratelegs.invalidvehicle"));
    public static final SimpleCommandExceptionType INVALID_ENTITY = new SimpleCommandExceptionType(Text.translatable("commands.attav.recalibratelegs.invalidentity"));

    public static final SimpleCommandExceptionType ENTITY_NOT_FOUND = new SimpleCommandExceptionType(Text.translatable("commands.attav.find.entitynotfound"));

    public static final Consumer<ServerCommandSource> RECALIBRATION_SUCCESS = (source) -> source.sendFeedback(() -> Text.translatable("commands.attav.recalibratelegs.success"), false);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                @SuppressWarnings("unused") CommandRegistryAccess registryAccess,
                                @SuppressWarnings("unused") CommandManager.RegistrationEnvironment environment) {

        LiteralCommandNode<ServerCommandSource> attaVNode = CommandManager.literal("attav").build();

        LiteralCommandNode<ServerCommandSource> recalibrateLegsNode = CommandManager.literal("recalibratelegs")
                .executes(AttaVCommands::recalibrateAsPassenger)
                .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(AttaVCommands::recalibrateAsOperator)
                        .build())
                .build();

        LiteralCommandNode<ServerCommandSource> dataNode = CommandManager.literal("data")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("target", EntityArgumentType.entity())
                        .then(CommandManager.argument("type", StringArgumentType.string())
                                .then(CommandManager.argument("args", StringArgumentType.string())
                                        .executes(AttaVCommands::executeData))
                                .build())
                        .build())
                .build();

        LiteralCommandNode<ServerCommandSource> findNode = CommandManager.literal("find")
                .requires(source -> source.hasPermissionLevel(1) || (source.getEntity() instanceof PlayerEntity player && player.isCreative()))
                .executes(AttaVCommands::executeFind)
                .build();

        dispatcher.getRoot().addChild(attaVNode);

        attaVNode.addChild(recalibrateLegsNode);
        attaVNode.addChild(dataNode);
        attaVNode.addChild(findNode);

        registerPathCommands(attaVNode);
    }

    private static int recalibrateAsOperator(Entity entity, SimpleCommandExceptionType exceptionType, ServerCommandSource context) throws CommandSyntaxException {
        if (!(entity instanceof WalkingCubeEntity walkingCube)) {
            throw exceptionType.create();
        }
        walkingCube.recalibrateLegs(true);
        RECALIBRATION_SUCCESS.accept(context);
        return 1;
    }

    private static int recalibrateAsOperator(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return recalibrateAsOperator(EntityArgumentType.getEntity(context, "target"), INVALID_ENTITY, context.getSource());
    }

    private static int recalibrateAsPassenger(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        if (source.getEntity() instanceof PlayerEntity player) {
            return recalibrateAsOperator(player.getVehicle(), INVALID_VEHICLE, source);
        }
        throw NOT_PLAYER_SOURCE.create();
    }

    private static int executeData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        if (!(source.getEntity() instanceof PlayerEntity)) {
            throw NOT_PLAYER_SOURCE.create();
        }
        if (!(EntityArgumentType.getEntity(context, "target") instanceof WalkingCubeEntity walkingCube)) {
            throw INVALID_ENTITY.create();
        }
        // basically invoke data command (I think this is safe, but my only worry is that "type" will somehow be more than one word and still make a valid command, basically lowering /data perms
        runCommand(source.getWorld(), walkingCube, "/data " + StringArgumentType.getString(context, "type") + " entity @s " + StringArgumentType.getString(context, "args"));
        return 1;
    }

    private static void runCommand(ServerWorld serverWorld, Entity entity, String command) {
        serverWorld.getServer().getCommandManager().executeWithPrefix(
                new ServerCommandSource(entity, entity.getPos(),
                        entity.getRotationClient(), serverWorld,
                        4, entity.getName().getString(),
                        entity.getDisplayName(), serverWorld.getServer(),
                        entity),
                command);
    }

    private static int executeFind(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        if (!(source.getEntity() instanceof PlayerEntity player)) {
            throw NOT_PLAYER_SOURCE.create();
        }
        Vec3d playerPos = player.getPos();
        ServerWorld serverWorld = source.getWorld();
        BlockPos blockPos = null;
        double distance = Double.MAX_VALUE;
        int iterations = 0;
        for (Entity entity : serverWorld.iterateEntities()) {
            if (!(entity instanceof WalkingCubeEntity walkingCube)) {
                continue;
            }
            double d = walkingCube.getPos().distanceTo(playerPos);
            if (d < distance) {
                distance = d;
                blockPos = walkingCube.getBlockPos();
            }
            iterations++;
            if (iterations >= Integer.MAX_VALUE - 10) {
                break;
            }
        }
        if (blockPos == null) {
            throw ENTITY_NOT_FOUND.create();
        }
        sendFindSuccess(source, blockPos, Math.round(distance));
        return 1;
    }

    private static void sendFindSuccess(ServerCommandSource source, BlockPos pos, long distance) {
        source.sendFeedback(() -> Text.stringifiedTranslatable("commands.attav.find.success", pos, distance), false);
    }


    private static final SuggestionProvider<ServerCommandSource> PATH_SUGGESTION_PROVIDER = (context, builder) -> {
        Collection<EntityPath> collection = AttaVWorldComponents.WORLD_PATH.get(context.getSource().getWorld()).getPaths();
        return CommandSource.suggestIdentifiers(collection.stream().map(entityPath -> entityPath.id), builder);
    };

    private static final DynamicCommandExceptionType PATH_ALREADY_EXISTS = new DynamicCommandExceptionType(obj ->
            Text.stringifiedTranslatable("commands.attav.path.create.alreadyexists", obj));

    private static final DynamicCommandExceptionType PATH_DOES_NOT_EXIST = new DynamicCommandExceptionType(obj ->
            Text.stringifiedTranslatable("commands.attav.path.remove.doesnotexist", obj));

    private static final DynamicCommandExceptionType UNSUPPORTED_ENTITY = new DynamicCommandExceptionType((obj) ->
            Text.stringifiedTranslatable("commands.attav.path.attach.unsupported", obj));

    private static final DynamicCommandExceptionType ADD_INDEX_OUT_OF_BOUNDS = new DynamicCommandExceptionType((obj) ->
            Text.stringifiedTranslatable("commands.attav.path.nodes.add.indexoutofbounds", obj));

    private static final DynamicCommandExceptionType REMOVE_INDEX_OUT_OF_BOUNDS = new DynamicCommandExceptionType((obj) ->
            Text.stringifiedTranslatable("commands.attav.path.nodes.remove.indexoutofbounds", obj));
    private static final DynamicCommandExceptionType GET_INDEX_OUT_OF_BOUNDS = new DynamicCommandExceptionType((obj) ->
            Text.stringifiedTranslatable("commands.attav.path.nodes.get.indexoutofbounds", obj));

    public static void registerPathCommands(LiteralCommandNode<ServerCommandSource> attaVNode) {
        LiteralCommandNode<ServerCommandSource> pathNode = CommandManager.literal("path")
                .requires(source -> source.hasPermissionLevel(2)).build();

        LiteralCommandNode<ServerCommandSource> createNode = CommandManager.literal("create").then(CommandManager.argument("id", IdentifierArgumentType.identifier()).suggests(PATH_SUGGESTION_PROVIDER)
                .executes(context -> {
                    Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
                    WorldPathComponent worldPathComponent = AttaVWorldComponents.WORLD_PATH.get(context.getSource().getWorld());
                    if (!worldPathComponent.isEmpty() && worldPathComponent.map.containsKey(id)) {
                        throw PATH_ALREADY_EXISTS.create(id);
                    }
                    worldPathComponent.put(new EntityPath(id));
                    worldPathComponent.sync();
                    context.getSource().sendFeedback(() -> Text.stringifiedTranslatable("commands.attav.path.create.success", id), true);
                    return 1;
                }).build())
                .build();

        LiteralCommandNode<ServerCommandSource> deleteNode = CommandManager.literal("delete").then(CommandManager.argument("id", IdentifierArgumentType.identifier()).suggests(PATH_SUGGESTION_PROVIDER)
                .executes(context -> {
                    IdToWorldPathComponent idToWorldPathComponent = getWorldPathComponent(context);
                    Identifier id = idToWorldPathComponent.id;
                    idToWorldPathComponent.worldPathComponent.map.remove(id);
                    idToWorldPathComponent.sync();
                    context.getSource().sendFeedback(() -> Text.stringifiedTranslatable("commands.attav.path.remove.success", id), true);
                    return 1;
                })).build();

        LiteralCommandNode<ServerCommandSource> colorNode = CommandManager.literal("color").then(CommandManager.argument("id", IdentifierArgumentType.identifier()).suggests(PATH_SUGGESTION_PROVIDER)
                        .then(CommandManager.argument("value", IntegerArgumentType.integer()).executes(context -> {
                            IdToWorldPathComponent idToWorldPathComponent = getWorldPathComponent(context);
                            final int color = IntegerArgumentType.getInteger(context, "value");
                            idToWorldPathComponent.getEntityPath().color = color;
                            idToWorldPathComponent.sync();
                            context.getSource().sendFeedback(() -> Text.stringifiedTranslatable("commands.attav.path.color.success", idToWorldPathComponent.id, color), true);
                            return 1;
                        }).build()))
                .build();

        LiteralCommandNode<ServerCommandSource> attachNode = CommandManager.literal("attach").then(CommandManager.argument("id", IdentifierArgumentType.identifier()).suggests(PATH_SUGGESTION_PROVIDER)
                        .then(CommandManager.argument("target", EntityArgumentType.entity()).executes(context -> {
                            IdToWorldPathComponent idToWorldPathComponent = getWorldPathComponent(context);
                            Entity entity = EntityArgumentType.getEntity(context, "target");
                            Text name = entity.getName();
                            if (!(entity instanceof Pathfinder pathfinder)) {
                                throw UNSUPPORTED_ENTITY.create(name);
                            }
                            pathfinder.followPath(idToWorldPathComponent.getEntityPath());
                            context.getSource().sendFeedback(() -> Text.stringifiedTranslatable("commands.attav.path.attach.success", entity, idToWorldPathComponent.id), true);
                            return 1;
                        }).build()))
                .build();

        LiteralCommandNode<ServerCommandSource> detachNode = CommandManager.literal("detach").then(CommandManager.argument("target", EntityArgumentType.entity())
                        .executes(context -> {
                            Entity entity = EntityArgumentType.getEntity(context, "target");
                            Text name = entity.getName();
                            if (!(entity instanceof Pathfinder pathfinder)) {
                                throw UNSUPPORTED_ENTITY.create(name);
                            }
                            pathfinder.followPath(null);
                            context.getSource().sendFeedback(() -> Text.stringifiedTranslatable("commands.attav.path.detach.success", entity), true);
                            return 1;
                        }).build())
                .build();

        LiteralCommandNode<ServerCommandSource> nodesNode = CommandManager.literal("nodes").build();

        LiteralCommandNode<ServerCommandSource> addNodeNode = CommandManager.literal("add").then(CommandManager.argument("id", IdentifierArgumentType.identifier()).suggests(PATH_SUGGESTION_PROVIDER)
                .then(CommandManager.argument("position", Vec3ArgumentType.vec3())
                        .executes(AttaVCommands::createNode)
                        .then(CommandManager.argument("index", IntegerArgumentType.integer())
                                .executes(AttaVCommands::createNodeWithIndex)).build())
                .build()).build();

        LiteralCommandNode<ServerCommandSource> removeNodeNode = CommandManager.literal("remove").then(CommandManager.argument("id", IdentifierArgumentType.identifier()).suggests(PATH_SUGGESTION_PROVIDER)
                .then(CommandManager.argument("index", IntegerArgumentType.integer())
                        .executes(context -> {
                            IdToWorldPathComponent idToWorldPathComponent = getWorldPathComponent(context);
                            EntityPath entityPath = idToWorldPathComponent.getEntityPath();
                            try {
                                final int index = IntegerArgumentType.getInteger(context, "index");
                                entityPath.nodes.remove(index);
                                idToWorldPathComponent.sync();
                                context.getSource().sendFeedback(() -> Text.stringifiedTranslatable("commands.attav.path.nodes.remove.success", index, entityPath.id), true);
                            } catch (IndexOutOfBoundsException e) {
                                throw REMOVE_INDEX_OUT_OF_BOUNDS.create(e);
                            }
                            return 1;
                        }).build())
                .build()).build();

        LiteralCommandNode<ServerCommandSource> getNodeNode = CommandManager.literal("get").then(CommandManager.argument("id", IdentifierArgumentType.identifier()).suggests(PATH_SUGGESTION_PROVIDER)
                .then(CommandManager.argument("index", IntegerArgumentType.integer())
                        .executes(context -> {
                            IdToWorldPathComponent idToWorldPathComponent = getWorldPathComponent(context);
                            EntityPath entityPath = idToWorldPathComponent.getEntityPath();
                            try {
                                final int index = IntegerArgumentType.getInteger(context, "index");
                                Vec3d vec3d = entityPath.nodes.get(index);
                                idToWorldPathComponent.sync();
                                context.getSource().sendFeedback(() -> Text.stringifiedTranslatable("commands.attav.path.nodes.get.success", index, entityPath.id, vec3d), false);
                            } catch (IndexOutOfBoundsException e) {
                                throw GET_INDEX_OUT_OF_BOUNDS.create(e);
                            }
                            return 1;
                        }).build())
                .build()).build();

        attaVNode.addChild(pathNode);

        pathNode.addChild(createNode);
        pathNode.addChild(deleteNode);
        pathNode.addChild(colorNode);
        pathNode.addChild(attachNode);
        pathNode.addChild(detachNode);

        pathNode.addChild(nodesNode);
        nodesNode.addChild(addNodeNode);
        nodesNode.addChild(removeNodeNode);
        nodesNode.addChild(getNodeNode);
    }

    private static IdToWorldPathComponent getWorldPathComponent(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
        WorldPathComponent worldPathComponent = AttaVWorldComponents.WORLD_PATH.get(context.getSource().getWorld());
        if (worldPathComponent.isEmpty() || !worldPathComponent.map.containsKey(id)) {
            throw PATH_DOES_NOT_EXIST.create(id);
        }
        return new IdToWorldPathComponent(id, worldPathComponent);
    }

    private static int createNode(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return createNode(context, -1);
    }

    private static int createNodeWithIndex(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return createNode(context, IntegerArgumentType.getInteger(context, "index"));
    }

    private static int createNode(CommandContext<ServerCommandSource> context, int index) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
        WorldPathComponent worldPathComponent = AttaVWorldComponents.WORLD_PATH.get(context.getSource().getWorld());
        if (!worldPathComponent.map.containsKey(id)) {
            throw PATH_DOES_NOT_EXIST.create(id);
        }
        EntityPath path = worldPathComponent.map.get(id);
        Vec3d vec3d = Vec3ArgumentType.getVec3(context, "position");
        if (index < 0) {
            path.nodes.add(vec3d);
        } else {
            try {
                path.nodes.add(index, vec3d);
            } catch (IndexOutOfBoundsException e) {
                throw ADD_INDEX_OUT_OF_BOUNDS.create(e);
            }
        }
        worldPathComponent.sync();
        context.getSource().sendFeedback(() -> Text.stringifiedTranslatable("commands.attav.path.nodes.add.success", vec3d, path.nodes.indexOf(vec3d), path.id), true);
        return 1;
    }

    private record IdToWorldPathComponent(Identifier id, WorldPathComponent worldPathComponent) {

        public EntityPath getEntityPath() {
            return worldPathComponent.map.get(id);
        }

        public void sync() {
            worldPathComponent.sync();
        }
    }
}
