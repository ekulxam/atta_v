package survivalblock.atmosphere.atta_v.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

public class AttaVClientCommands {

    @SuppressWarnings("unused")
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {

        LiteralCommandNode<FabricClientCommandSource> attaVNode = ClientCommandManager.literal("attavclient").build();

        LiteralCommandNode<FabricClientCommandSource> showPathsNode = ClientCommandManager.literal("showentitypaths")
                .executes(context -> {
                    context.getSource().sendFeedback(Text.stringifiedTranslatable("commands.attavclient.showentitypaths.get", AttaVClient.showEntityPaths));
                    return 1;
                })
                .then(ClientCommandManager.argument("value", BoolArgumentType.bool()).executes(context -> {
                    boolean bool = BoolArgumentType.getBool(context, "value");
                    AttaVClient.showEntityPaths = bool;
                    context.getSource().sendFeedback(Text.stringifiedTranslatable("commands.attavclient.showentitypaths.set", bool));
                    return 1;
                }).build())
                .build();

        dispatcher.getRoot().addChild(attaVNode);

        attaVNode.addChild(showPathsNode);
    }
}
