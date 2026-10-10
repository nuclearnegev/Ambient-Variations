package com.ambientvariations.history;

import com.ambientvariations.director.DirectorState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The director's private memory, saved with the world (data/ambientvariations_history.dat).
 * Every mutation goes through a method here so the save is always flagged dirty.
 */
public class WorldHistory extends PersistentState {
	private static final String DATA_ID = "ambientvariations_history";
	private static final int MAX_RECORDS = 2000;

	private DirectorState state = DirectorState.DORMANT;
	private long clockMinutes = 0;
	private long stateEnteredMinutes = 0;
	private long silenceUntilMinutes = 0;
	private int eventsInState = 0;
	private boolean stopped = false;
	private final List<EventRecord> records = new ArrayList<>();

	public static WorldHistory get(MinecraftServer server) {
		PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
		return manager.getOrCreate(WorldHistory::fromNbt, WorldHistory::new, DATA_ID);
	}

	// ---------- reads ----------
	public DirectorState getState() { return state; }
	public long getClockMinutes() { return clockMinutes; }
	public long getStateEnteredMinutes() { return stateEnteredMinutes; }
	public long getSilenceUntilMinutes() { return silenceUntilMinutes; }
	public int getEventsInState() { return eventsInState; }
	public boolean isStopped() { return stopped; }
	public List<EventRecord> records() { return Collections.unmodifiableList(records); }

	public int naturalEventCount() {
		int n = 0;
		for (EventRecord r : records) {
			if (!r.forced) n++;
		}
		return n;
	}

	/** Director minutes since this event last happened naturally, or Long.MAX_VALUE if never. */
	public long minutesSinceLast(String eventId) {
		for (int i = records.size() - 1; i >= 0; i--) {
			EventRecord r = records.get(i);
			if (!r.forced && r.eventId.equals(eventId)) {
				return clockMinutes - r.clockMinutes;
			}
		}
		return Long.MAX_VALUE;
	}

	// ---------- writes ----------
	public void advanceClock(long minutes) {
		clockMinutes += minutes;
		markDirty();
	}

	public void setState(DirectorState newState) {
		state = newState;
		stateEnteredMinutes = clockMinutes;
		eventsInState = 0;
		markDirty();
	}

	public void silenceFor(long minutes) {
		silenceUntilMinutes = clockMinutes + Math.max(0, minutes);
		markDirty();
	}

	public void incrementEventsInState() {
		eventsInState++;
		markDirty();
	}

	public void setStopped(boolean value) {
		stopped = value;
		markDirty();
	}

	public void addRecord(EventRecord record) {
		records.add(record);
		while (records.size() > MAX_RECORDS) {
			records.remove(0);
		}
		markDirty();
	}

	public void reset() {
		state = DirectorState.DORMANT;
		clockMinutes = 0;
		stateEnteredMinutes = 0;
		silenceUntilMinutes = 0;
		eventsInState = 0;
		stopped = false;
		records.clear();
		markDirty();
	}

	// ---------- saving ----------
	public static WorldHistory fromNbt(NbtCompound nbt) {
		WorldHistory h = new WorldHistory();
		h.state = DirectorState.parse(nbt.getString("state"), DirectorState.DORMANT);
		h.clockMinutes = nbt.getLong("clock");
		h.stateEnteredMinutes = nbt.getLong("stateEntered");
		h.silenceUntilMinutes = nbt.getLong("silenceUntil");
		h.eventsInState = nbt.getInt("eventsInState");
		h.stopped = nbt.getBoolean("stopped");

		NbtList list = nbt.getList("records", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			h.records.add(EventRecord.fromNbt(list.getCompound(i)));
		}
		return h;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putString("state", state.name());
		nbt.putLong("clock", clockMinutes);
		nbt.putLong("stateEntered", stateEnteredMinutes);
		nbt.putLong("silenceUntil", silenceUntilMinutes);
		nbt.putInt("eventsInState", eventsInState);
		nbt.putBoolean("stopped", stopped);

		NbtList list = new NbtList();
		for (EventRecord r : records) {
			list.add(r.toNbt());
		}
		nbt.put("records", list);
		return nbt;
	}
}
