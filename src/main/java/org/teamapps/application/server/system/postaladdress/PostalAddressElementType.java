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

public enum PostalAddressElementType {

	FIRST_NAME("FN", "Firstname"),
	LAST_NAME("N", "Lastname"),
	ORGANIZATION("O", "Organisation"),
	ADDRESS("A", "Street"),
	DEPENDENT_LOCALITY("D", "Dependent locality"),
	CITY("C", "City"),
	STATE("S", "State"),
	ZIP("Z", "Zip"),
	SORTING_CODE("X", "Sorting code"),
	;

	private final String key;
	private final String name;

	PostalAddressElementType(String key, String name) {
		this.key = key;
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public static PostalAddressElementType findType(String s) {
		for (PostalAddressElementType value : values()) {
			if (value.getKey().equals(s)) {
				return value;
			}
		}
		return null;
	}

}
