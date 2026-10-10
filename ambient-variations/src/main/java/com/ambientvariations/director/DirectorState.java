package com.ambientvariations.director;

import java.util.Locale;

/** Conceptual moods of the director. These are NOT a mandatory progression. */
public enum DirectorState {
	DORMANT,
	OBSERVATION,
	FAMILIARITY,
	WITHDRAWAL,
	RETURN;

	public static DirectorState parse(String name, DirectorState fallback) {
		if (name == null) {
			return fallback;
		}
		try {
			return valueOf(name.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return fallback;
		}
	}
}
