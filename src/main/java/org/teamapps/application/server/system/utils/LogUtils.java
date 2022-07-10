/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
 * ---
 * Copyright (C) 2020 - 2022 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
