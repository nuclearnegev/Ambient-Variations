package com.ambientvariations.director.events;

import com.ambientvariations.director.AnomalyEvent;
import com.ambientvariations.director.DirectorState;
import com.ambientvariations.director.EventContext;
import com.ambientvariations.history.EventRecord;
import net.minecraft.text.Text;

/**
 * Debug-only event (weight 0, so the director never picks it). Use
 * "/avdebug force test_ping" to confirm the event pipeline and history saving work.
 */
public final class TestPingEvent implements AnomalyEvent {
	@Override
	public String id() {
		return "test_ping";
	}

	@Override
	public boolean allowedIn(DirectorState state) {
		return false;
	}

	@Override
	public int weight() {
		return 0;
	}

	@Override
	public boolean canRun(EventContext ctx) {
		return true;
	}

	@Override
	public EventRecord run(EventContext ctx) {
		ctx.player().sendMessage(Text.literal("[debug] test_ping ran in state " + ctx.state()), false);
		return EventRecord.of(ctx, id(), ctx.player().getBlockPos(), "debug test");
	}
}
