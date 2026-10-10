package com.ambientvariations.director;

import com.ambientvariations.history.WorldHistory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Random;

/** Everything an anomaly event needs to know when it is asked to run. */
public record EventContext(
		MinecraftServer server,
		ServerPlayerEntity player,
		WorldHistory history,
		DirectorState state,
		Random random,
		boolean forced) {

	public ServerWorld world() {
		return player.getServerWorld();
	}
}
