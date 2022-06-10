/*-
 * ========================LICENSE_START=================================
 * TeamApps Application Server
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
package org.teamapps.application.server.controlcenter.database;

public class DbExplorerUtils {

	public static String getFirstUpper(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	public static String createTitleFromCamelCase(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (i < 3) {
				sb.append(c);
			} else {
				if (Character.isUpperCase(c)) {
					sb.append(" ");
				}
				sb.append(c);
			}
		}
		return getFirstUpper(sb.toString());
	}
}
