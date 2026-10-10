package com.ambientvariations.director;

import com.ambientvariations.director.events.TestPingEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EventRegistry {
	private static final Map<String, AnomalyEvent> EVENTS = new LinkedHashMap<>();

	private EventRegistry() {}

	public static void register(AnomalyEvent event) {
		EVENTS.put(event.id(), event);
	}

	public static AnomalyEvent get(String id) {
		return EVENTS.get(id);
	}

	public static Collection<String> ids() {
		return new ArrayList<>(EVENTS.keySet());
	}

	public static void registerDefaults() {
		register(new TestPingEvent());
		// Future anomalies get registered here.
	}

	/** Weighted random choice among events that are allowed, possible, and off cooldown. */
	public static AnomalyEvent pick(EventContext ctx) {
		List<AnomalyEvent> candidates = new ArrayList<>();
		int total = 0;
		for (AnomalyEvent e : EVENTS.values()) {
			if (e.weight() <= 0) continue;
			if (!e.allowedIn(ctx.state())) continue;
			if (ctx.history().minutesSinceLast(e.id()) < e.cooldownMinutes()) continue;
			if (!e.canRun(ctx)) continue;
			candidates.add(e);
			total += e.weight();
		}
		if (candidates.isEmpty()) {
			return null;
		}
		int roll = ctx.random().nextInt(total);
		for (AnomalyEvent e : candidates) {
			roll -= e.weight();
			if (roll < 0) {
				return e;
			}
		}
		return null;
	}
}
