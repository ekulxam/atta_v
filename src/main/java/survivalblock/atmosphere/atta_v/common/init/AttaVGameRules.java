package survivalblock.atmosphere.atta_v.common.init;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

public class AttaVGameRules {

    public static final GameRules.Key<GameRules.BooleanRule> WANDERER_STOMP_DOES_DAMAGE = GameRuleRegistry.register("atta_v:wandererStompDoesDamage", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> WANDERER_SEEKS_OUT_PLAYERS = GameRuleRegistry.register("atta_v:wandererSeeksOutPlayers", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    public static void init() {

    }
}
