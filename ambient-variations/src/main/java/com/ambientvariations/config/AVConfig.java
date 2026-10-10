package com.ambientvariations.config;

import com.ambientvariations.AmbientVariations;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Host-facing tuning. Lives in config/ambientvariations.json (created on first launch).
 * All times are in "director minutes": one minute of real play time with at least
 * one player online while the game rule is on.
 */
public final class AVConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static AVConfig instance = new AVConfig();

	/** Writes director decisions to the server log. Off by default. */
	public boolean debugLogging = false;

	/** Minimum quiet time after entering Dormant before the director may wake. */
	public int dormantMinMinutes = 180;
	/** Chance per minute (after the quiet period) that Dormant ends. Low on purpose. */
	public double dormantWakeChancePerMinute = 0.002;

	/** Chance per minute, while Observation/Familiarity, that the director considers an event. */
	public double eventChancePerMinute = 0.008;
	/** Same, but for the single incident allowed in the Return state. */
	public double returnEventChancePerMinute = 0.01;

	/** After any event, the director stays silent for a random time between these. */
	public int silenceMinMinutes = 30;
	public int silenceMaxMinutes = 240;

	/** Natural events needed in Observation before moving to Familiarity. */
	public int eventsBeforeFamiliarity = 4;

	/** Minimum time spent in Withdrawal before the director may leave it. */
	public int withdrawalMinMinutes = 600;

	public static AVConfig get() {
		return instance;
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("ambientvariations.json");
		AVConfig loaded = null;

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				loaded = GSON.fromJson(reader, AVConfig.class);
			} catch (IOException | RuntimeException e) {
				AmbientVariations.LOGGER.warn("Could not read config, using defaults: {}", e.toString());
			}
		}
		if (loaded == null) {
			loaded = new AVConfig();
		}
		loaded.sanitize();
		instance = loaded;

		// Write back so new options appear in the file after updates.
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(loaded, writer);
		} catch (IOException e) {
			AmbientVariations.LOGGER.warn("Could not write config: {}", e.toString());
		}
	}

	private void sanitize() {
		dormantMinMinutes = Math.max(0, dormantMinMinutes);
		dormantWakeChancePerMinute = clamp01(dormantWakeChancePerMinute);
		eventChancePerMinute = clamp01(eventChancePerMinute);
		returnEventChancePerMinute = clamp01(returnEventChancePerMinute);
		silenceMinMinutes = Math.max(0, silenceMinMinutes);
		silenceMaxMinutes = Math.max(silenceMinMinutes, silenceMaxMinutes);
		eventsBeforeFamiliarity = Math.max(1, eventsBeforeFamiliarity);
		withdrawalMinMinutes = Math.max(0, withdrawalMinMinutes);
	}

	private static double clamp01(double v) {
		return Math.max(0.0, Math.min(1.0, v));
	}
}
