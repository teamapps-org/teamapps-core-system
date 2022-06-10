package org.teamapps.application.server.system.utils;

import org.slf4j.event.Level;
import org.teamapps.model.controlcenter.LogLevel;

public class LogUtils {

	public static LogLevel convert(Level level) {
		switch (level) {
			case ERROR -> {
				return LogLevel.ERROR;
			}
			case WARN -> {
				return LogLevel.WARNING;
			}
			case INFO -> {
				return LogLevel.INFO;
			}
			case DEBUG, TRACE -> {
				return null;
			}
		}
		return null;
	}
}
