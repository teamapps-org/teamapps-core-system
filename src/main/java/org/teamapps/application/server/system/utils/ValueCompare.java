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
package org.teamapps.application.server.system.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ValueCompare {

	private boolean different;
	private Map<String, Object> valueByKey = new HashMap<>();

	public static ValueCompare create(Object a, Object b) {
		return new ValueCompare().check(a, b);
	}

	public static ValueCompare create(String key, Object a, Object b) {
		return new ValueCompare().check(key, a, b);
	}

	public ValueCompare check(Object a, Object b) {
		if (!Objects.equals(a, b)) {
			different = true;
		}
		return this;
	}

	public ValueCompare check(String key, Object value, Object existingValue) {
		valueByKey.put(key, value);
		return check(value, existingValue);
	}

	public boolean isDifferent() {
		return different;
	}

	public String getString(String key) {
		return (String) valueByKey.get(key);
	}

	public Integer getInt(String key) {
		return (Integer) valueByKey.get(key);
	}

	public Object get(String key) {
		return valueByKey.get(key);
	}
}
