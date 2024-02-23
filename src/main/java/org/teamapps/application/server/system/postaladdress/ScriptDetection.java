/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
 * ---
 * Copyright (C) 2020 - 2024 TeamApps.org
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
package org.teamapps.application.server.system.postaladdress;

import com.ibm.icu.text.Transliterator;

import java.util.HashMap;
import java.util.Map;

public class ScriptDetection {

	private static Character.UnicodeScript getScript(String s) {
		int[] counts = new int[Character.UnicodeScript.values().length];

		Character.UnicodeScript mostFrequentScript = null;
		int maxCount = 0;

		int n = s.codePointCount(0, s.length());
		for (int i = 0; i < n; i = s.offsetByCodePoints(i, 1)) {
			int codePoint = s.codePointAt(i);
			Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);

			int count = ++counts[script.ordinal()];
			if (mostFrequentScript == null || count > maxCount) {
				maxCount = count;
				mostFrequentScript = script;
			}
		}
		return mostFrequentScript;
	}

	private static Map<Character.UnicodeScript, Integer> analyseScripts(String s) {
		Map<Character.UnicodeScript, Integer> resultMap = new HashMap<>();
		int n = s.codePointCount(0, s.length());
		for (int i = 0; i < n; i = s.offsetByCodePoints(i, 1)) {
			Character.UnicodeScript script = Character.UnicodeScript.of(s.codePointAt(i));
			if (script != Character.UnicodeScript.COMMON) {
				resultMap.compute(script, (sc, v) -> v == null ? 1 : v + 1);
			}
		}
		return resultMap;
	}

}
