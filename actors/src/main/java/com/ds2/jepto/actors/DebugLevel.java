package com.ds2.jepto.actors;

import java.util.logging.Level;

/**
 * DEBUG levels are those in the range between
 * levels CONFIG and INFO (from value 700 up to 800).
 *
 */
public class DebugLevel extends Level {
	public static final DebugLevel DEBUG =
			DebugLevel.makeDebugLevel("DEBUG", Level.INFO.intValue() - 1);

	private DebugLevel(String name, int value) {
		super(name, value);
	}

	protected static DebugLevel makeDebugLevel(String name, int value) throws IllegalArgumentException {
		if (value >= Level.INFO.intValue() ||
			value <= Level.CONFIG.intValue())
			throw new IllegalArgumentException();
		return new DebugLevel(name, value);
	}

	public static Level parse(String name) {
		try {
			Level level = Level.parse(name);
			return level;
		} catch (IllegalArgumentException e) {
		}

		if (name.toUpperCase().equals("DEBUG"))
			return DEBUG;
		try {
			int value = Integer.parseUnsignedInt(name);
			return makeDebugLevel("DEBUG", value);
		} catch (NumberFormatException e) {
		} catch (IllegalArgumentException e) {
		}
		return DEBUG;
	}
}