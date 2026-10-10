package com.ambientvariations.director;

import com.ambientvariations.history.EventRecord;

/**
 * One kind of anomaly. To add a new one: implement this interface and register it in
 * EventRegistry.registerDefaults(). The director never needs to change.
 */
public interface AnomalyEvent {
	/** Unique short id, e.g. "leafless_tree". Used in history and debug commands. */
	String id();

	/** Which director states may pick this event on their own. */
	boolean allowedIn(DirectorState state);

	/** Relative likelihood. 0 means the director never picks it (debug-only). */
	int weight();

	/** Minimum director minutes before this same event may happen again. */
	default int cooldownMinutes() {
		return 360;
	}

	/** Cheap check: is this event possible for this player right now? No side effects. */
	boolean canRun(EventContext ctx);

	/**
	 * Do the thing. Return a record describing it, or null if nothing ended up happening.
	 * Events must be harmless: never destroy builds, kill pets, or take valuable items.
	 */
	EventRecord run(EventContext ctx);
}
