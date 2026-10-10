package com.ambientvariations.history;

import com.ambientvariations.director.DirectorState;
import com.ambientvariations.director.EventContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** One remembered incident. Stored privately in the world save. */
public final class EventRecord {
	public final String eventId;
	public final long clockMinutes;
	public final long worldTime;
	public final String dimension;
	public final boolean hasPos;
	public final int x;
	public final int y;
	public final int z;
	public final String playerUuid;
	public final DirectorState stateAtTime;
	public final boolean forced;
	public final String note;

	public EventRecord(String eventId, long clockMinutes, long worldTime, String dimension,
					   boolean hasPos, int x, int y, int z, String playerUuid,
					   DirectorState stateAtTime, boolean forced, String note) {
		this.eventId = eventId;
		this.clockMinutes = clockMinutes;
		this.worldTime = worldTime;
		this.dimension = dimension;
		this.hasPos = hasPos;
		this.x = x;
		this.y = y;
		this.z = z;
		this.playerUuid = playerUuid;
		this.stateAtTime = stateAtTime;
		this.forced = forced;
		this.note = note;
	}

	/** Convenience: build a record from an event context. pos may be null. */
	public static EventRecord of(EventContext ctx, String eventId, BlockPos pos, String note) {
		ServerWorld world = ctx.world();
		return new EventRecord(
				eventId,
				ctx.history().getClockMinutes(),
				world.getTime(),
				world.getRegistryKey().getValue().toString(),
				pos != null,
				pos == null ? 0 : pos.getX(),
				pos == null ? 0 : pos.getY(),
				pos == null ? 0 : pos.getZ(),
				ctx.player().getUuid().toString(),
				ctx.state(),
				ctx.forced(),
				note == null ? "" : note);
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("id", eventId);
		nbt.putLong("clock", clockMinutes);
		nbt.putLong("time", worldTime);
		nbt.putString("dim", dimension);
		nbt.putBoolean("hasPos", hasPos);
		nbt.putInt("x", x);
		nbt.putInt("y", y);
		nbt.putInt("z", z);
		nbt.putString("player", playerUuid);
		nbt.putString("state", stateAtTime.name());
		nbt.putBoolean("forced", forced);
		nbt.putString("note", note);
		return nbt;
	}

	public static EventRecord fromNbt(NbtCompound nbt) {
		return new EventRecord(
				nbt.getString("id"),
				nbt.getLong("clock"),
				nbt.getLong("time"),
				nbt.getString("dim"),
				nbt.getBoolean("hasPos"),
				nbt.getInt("x"),
				nbt.getInt("y"),
				nbt.getInt("z"),
				nbt.getString("player"),
				DirectorState.parse(nbt.getString("state"), DirectorState.DORMANT),
				nbt.getBoolean("forced"),
				nbt.getString("note"));
	}
}
