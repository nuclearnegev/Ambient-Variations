package com.ambientvariations.director;

import com.ambientvariations.AmbientVariations;
import com.ambientvariations.Settings;
import com.ambientvariations.config.AVConfig;
import com.ambientvariations.history.EventRecord;
import com.ambientvariations.history.WorldHistory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Random;

/**
 * The adaptive director. Once per minute of play it makes ONE decision, and the most
 * common decision is to do nothing. It keeps its own clock ("director minutes") that only
 * advances while the game rule is on and at least one player is online.
 */
public final class Director {
	public static final int TICKS_PER_DECISION = 1200; // 1 minute at 20 tps
	private static final Random RANDOM = new Random();
	private static int tickCounter = 0;

	private Director() {}

	public static void reset() {
		tickCounter = 0;
	}

	public static boolean isEnabled(MinecraftServer server) {
		return server.getGameRules().getBoolean(Settings.AMBIENT_WORLD_BEHAVIOR);
	}

	public static void onServerTick(MinecraftServer server) {
		if (++tickCounter < TICKS_PER_DECISION) {
			return;
		}
		tickCounter = 0;

		// Rule off = total inertness. No file is created, nothing is read.
		if (!isEnabled(server)) {
			return;
		}
		List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
		if (players.isEmpty()) {
			return;
		}
		WorldHistory history = WorldHistory.get(server);
		if (history.isStopped()) {
			return;
		}

		history.advanceClock(1);
		try {
			String outcome = decide(server, history, players);
			if (AVConfig.get().debugLogging) {
				AmbientVariations.LOGGER.info("[director] {} @{}min | {}",
						history.getState(), history.getClockMinutes(), outcome);
			}
		} catch (RuntimeException e) {
			// A buggy event must never be able to crash the server tick.
			AmbientVariations.LOGGER.error("Director decision failed", e);
		}
	}

	/** One decision. Returns a short human-readable reason (used by /avdebug decide). */
	public static String decide(MinecraftServer server, WorldHistory h, List<ServerPlayerEntity> players) {
		AVConfig cfg = AVConfig.get();

		if (h.isStopped()) {
			return "stopped permanently";
		}
		long silenceLeft = h.getSilenceUntilMinutes() - h.getClockMinutes();
		if (silenceLeft > 0) {
			return "silent for " + silenceLeft + " more min";
		}
		long inState = h.getClockMinutes() - h.getStateEnteredMinutes();

		switch (h.getState()) {
			case DORMANT -> {
				if (inState < cfg.dormantMinMinutes) {
					return "dormant: quiet period not over (" + inState + "/" + cfg.dormantMinMinutes + " min)";
				}
				if (RANDOM.nextDouble() >= cfg.dormantWakeChancePerMinute) {
					return "dormant: stayed quiet";
				}
				// First awakening -> Observation. Later awakenings -> a single Return incident.
				DirectorState next = h.naturalEventCount() == 0 ? DirectorState.OBSERVATION : DirectorState.RETURN;
				h.setState(next);
				return "dormant -> " + next;
			}
			case OBSERVATION, FAMILIARITY -> {
				if (RANDOM.nextDouble() >= cfg.eventChancePerMinute) {
					return "chose silence";
				}
				return tryEvent(server, h, players);
			}
			case RETURN -> {
				if (RANDOM.nextDouble() >= cfg.returnEventChancePerMinute) {
					return "return: waiting";
				}
				return tryEvent(server, h, players);
			}
			case WITHDRAWAL -> {
				if (inState < cfg.withdrawalMinMinutes) {
					return "withdrawal: still quiet (" + inState + "/" + cfg.withdrawalMinMinutes + " min)";
				}
				DirectorState next = RANDOM.nextDouble() < 0.7 ? DirectorState.DORMANT : DirectorState.RETURN;
				h.setState(next);
				return "withdrawal -> " + next;
			}
		}
		return "no decision";
	}

	private static String tryEvent(MinecraftServer server, WorldHistory h, List<ServerPlayerEntity> players) {
		AVConfig cfg = AVConfig.get();

		// Target selection will become smarter in the multiplayer stage.
		ServerPlayerEntity target = players.get(RANDOM.nextInt(players.size()));
		EventContext ctx = new EventContext(server, target, h, h.getState(), RANDOM, false);

		AnomalyEvent event = EventRegistry.pick(ctx);
		if (event == null) {
			return "no eligible event, stayed quiet";
		}
		EventRecord record = event.run(ctx);
		if (record == null) {
			return "event " + event.id() + " declined to run";
		}

		h.addRecord(record);
		h.incrementEventsInState();
		h.silenceFor(cfg.silenceMinMinutes + RANDOM.nextInt(cfg.silenceMaxMinutes - cfg.silenceMinMinutes + 1));

		if (h.getState() == DirectorState.RETURN) {
			h.setState(DirectorState.DORMANT);
		} else if (h.getState() == DirectorState.OBSERVATION
				&& h.getEventsInState() >= cfg.eventsBeforeFamiliarity) {
			h.setState(DirectorState.FAMILIARITY);
		}
		return "ran " + event.id();
	}
}
