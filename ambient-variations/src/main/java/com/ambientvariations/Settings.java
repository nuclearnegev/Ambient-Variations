package com.ambientvariations;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

/**
 * The only switch that turns the director on. It is a per-world game rule in the
 * "Miscellaneous" category, so it lives under More > Game Rules when creating a world.
 * Default is OFF. While it is off the mod does nothing at all (it does not even
 * create a save file).
 */
public final class Settings {
	public static GameRules.Key<GameRules.BooleanRule> AMBIENT_WORLD_BEHAVIOR;

	private Settings() {}

	public static void register() {
		AMBIENT_WORLD_BEHAVIOR = GameRuleRegistry.register(
				"ambientWorldBehavior",
				GameRules.Category.MISC,
				GameRuleFactory.createBooleanRule(false));
	}
}
