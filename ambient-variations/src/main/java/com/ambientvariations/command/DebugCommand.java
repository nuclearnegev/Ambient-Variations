package com.ambientvariations.command;

import com.ambientvariations.director.AnomalyEvent;
import com.ambientvariations.director.Director;
import com.ambientvariations.director.DirectorState;
import com.ambientvariations.director.EventContext;
import com.ambientvariations.director.EventRegistry;
import com.ambientvariations.history.EventRecord;
import com.ambientvariations.history.WorldHistory;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Host/developer tool. Operators only (permission level 4). Replies go only to the person
 * who typed the command, never to other operators.
 *
 *   /avdebug status                  show director state
 *   /avdebug decide                  run one decision right now and explain it
 *   /avdebug advance <minutes>       fast-forward the director clock
 *   /avdebug state <name>            force a state (dormant, observation, ...)
 *   /avdebug silence <minutes>       silence from now (0 clears it)
 *   /avdebug force <event>           run an event on yourself
 *   /avdebug history                 last 10 remembered incidents
 *   /avdebug stop | resume           permanently stop / un-stop the director
 *   /avdebug reset                   wipe the director's memory for this world
 */
public final class DebugCommand {
	private static final List<String> STATE_NAMES = new ArrayList<>();

	static {
		for (DirectorState s : DirectorState.values()) {
			STATE_NAMES.add(s.name().toLowerCase(Locale.ROOT));
		}
	}

	private DebugCommand() {}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("avdebug")
				.requires(source -> source.hasPermissionLevel(4))
				.then(CommandManager.literal("status").executes(DebugCommand::status))
				.then(CommandManager.literal("decide").executes(DebugCommand::decide))
				.then(CommandManager.literal("history").executes(DebugCommand::history))
				.then(CommandManager.literal("stop").executes(c -> setStopped(c, true)))
				.then(CommandManager.literal("resume").executes(c -> setStopped(c, false)))
				.then(CommandManager.literal("reset").executes(DebugCommand::reset))
				.then(CommandManager.literal("advance")
						.then(CommandManager.argument("minutes", IntegerArgumentType.integer(1, 1000000))
								.executes(DebugCommand::advance)))
				.then(CommandManager.literal("silence")
						.then(CommandManager.argument("minutes", IntegerArgumentType.integer(0, 1000000))
								.executes(DebugCommand::silence)))
				.then(CommandManager.literal("state")
						.then(CommandManager.argument("name", StringArgumentType.word())
								.suggests((c, b) -> CommandSource.suggestMatching(STATE_NAMES, b))
								.executes(DebugCommand::setState)))
				.then(CommandManager.literal("force")
						.then(CommandManager.argument("event", StringArgumentType.word())
								.suggests((c, b) -> CommandSource.suggestMatching(EventRegistry.ids(), b))
								.executes(DebugCommand::force))));
	}

	private static void reply(ServerCommandSource source, String message) {
		source.sendFeedback(() -> Text.literal(message), false);
	}

	private static int status(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource src = ctx.getSource();
		MinecraftServer server = src.getServer();
		WorldHistory h = WorldHistory.get(server);
		reply(src, "Setting (ambientWorldBehavior): " + (Director.isEnabled(server) ? "ON" : "OFF"));
		reply(src, "State: " + h.getState() + " (for " + (h.getClockMinutes() - h.getStateEnteredMinutes()) + " min)");
		reply(src, "Director clock: " + h.getClockMinutes() + " min (" + (h.getClockMinutes() / 60) + " h)");
		long silenceLeft = Math.max(0, h.getSilenceUntilMinutes() - h.getClockMinutes());
		reply(src, "Silent for another: " + silenceLeft + " min");
		reply(src, "Stopped: " + h.isStopped() + " | Events remembered: " + h.records().size()
				+ " (natural: " + h.naturalEventCount() + ")");
		return 1;
	}

	private static int decide(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource src = ctx.getSource();
		MinecraftServer server = src.getServer();
		WorldHistory h = WorldHistory.get(server);
		List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
		if (players.isEmpty()) {
			src.sendError(Text.literal("No players online."));
			return 0;
		}
		String outcome = Director.decide(server, h, players);
		reply(src, "Decision: " + outcome + " | now " + h.getState());
		return 1;
	}

	private static int advance(CommandContext<ServerCommandSource> ctx) {
		int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
		WorldHistory h = WorldHistory.get(ctx.getSource().getServer());
		h.advanceClock(minutes);
		reply(ctx.getSource(), "Director clock advanced " + minutes + " min. Now " + h.getClockMinutes() + " min.");
		return 1;
	}

	private static int silence(CommandContext<ServerCommandSource> ctx) {
		int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
		WorldHistory h = WorldHistory.get(ctx.getSource().getServer());
		h.silenceFor(minutes);
		reply(ctx.getSource(), "Silent for " + minutes + " min from now.");
		return 1;
	}

	private static int setState(CommandContext<ServerCommandSource> ctx) {
		String name = StringArgumentType.getString(ctx, "name");
		DirectorState parsed = DirectorState.parse(name, null);
		if (parsed == null) {
			ctx.getSource().sendError(Text.literal("Unknown state: " + name));
			return 0;
		}
		WorldHistory h = WorldHistory.get(ctx.getSource().getServer());
		h.setState(parsed);
		h.silenceFor(0);
		reply(ctx.getSource(), "State set to " + parsed + " (silence cleared).");
		return 1;
	}

	private static int setStopped(CommandContext<ServerCommandSource> ctx, boolean value) {
		WorldHistory h = WorldHistory.get(ctx.getSource().getServer());
		h.setStopped(value);
		reply(ctx.getSource(), value ? "Director stopped." : "Director resumed.");
		return 1;
	}

	private static int reset(CommandContext<ServerCommandSource> ctx) {
		WorldHistory h = WorldHistory.get(ctx.getSource().getServer());
		h.reset();
		reply(ctx.getSource(), "Director memory wiped for this world.");
		return 1;
	}

	private static int force(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerCommandSource src = ctx.getSource();
		ServerPlayerEntity player = src.getPlayerOrThrow();
		String id = StringArgumentType.getString(ctx, "event");

		AnomalyEvent event = EventRegistry.get(id);
		if (event == null) {
			src.sendError(Text.literal("Unknown event: " + id + " (known: " + EventRegistry.ids() + ")"));
			return 0;
		}
		WorldHistory h = WorldHistory.get(src.getServer());
		EventContext ec = new EventContext(src.getServer(), player, h, h.getState(), new Random(), true);
		if (!event.canRun(ec)) {
			src.sendError(Text.literal(id + " cannot run for you right now."));
			return 0;
		}
		EventRecord record = event.run(ec);
		if (record == null) {
			src.sendError(Text.literal(id + " ran but did nothing."));
			return 0;
		}
		h.addRecord(record);
		reply(src, "Forced " + id + " (recorded as forced).");
		return 1;
	}

	private static int history(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource src = ctx.getSource();
		WorldHistory h = WorldHistory.get(src.getServer());
		List<EventRecord> records = h.records();
		reply(src, records.size() + " record(s). Showing the latest 10:");
		for (int i = Math.max(0, records.size() - 10); i < records.size(); i++) {
			EventRecord r = records.get(i);
			String where = r.hasPos ? " | " + r.x + " " + r.y + " " + r.z : "";
			reply(src, "  " + r.eventId + (r.forced ? " [forced]" : "") + " | " + r.stateAtTime
					+ " | " + (h.getClockMinutes() - r.clockMinutes) + " min ago" + where);
		}
		return 1;
	}
}
