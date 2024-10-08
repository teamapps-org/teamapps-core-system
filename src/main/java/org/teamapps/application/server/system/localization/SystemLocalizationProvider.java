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
package org.teamapps.application.server.system.localization;

import org.teamapps.model.controlcenter.LocalizationKey;
import org.teamapps.model.controlcenter.LocalizationKeyType;
import org.teamapps.model.controlcenter.LocalizationValue;
import org.teamapps.universaldb.index.enumeration.EnumFilterType;
import org.teamapps.universaldb.index.numeric.NumericFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemLocalizationProvider implements LocalizationProvider {

	public final static String SYSTEM_KEY_PREFIX = "org.teamapps.systemKey.";

	private Map<String, Map<String, LocalizationValue>> localizationLanguageValueMapByKey;

	public SystemLocalizationProvider() {
		update();
	}

	public void update() {
		Map<String, Map<String, LocalizationValue>> localizationLanguageValueMapByKey = new HashMap<>();
		LocalizationKey.filter()
				.application(NumericFilter.equalsFilter(0))
				.localizationKeyType(EnumFilterType.EQUALS, LocalizationKeyType.SYSTEM_KEY)
				.execute()
				.stream()
				.flatMap(key -> key.getLocalizationValues().stream()).forEach(value -> {
			localizationLanguageValueMapByKey.computeIfAbsent(value.getLocalizationKey().getKey(), k -> new HashMap<>()).put(value.getLanguage(), value);
		});
		this.localizationLanguageValueMapByKey = localizationLanguageValueMapByKey;
	}

	public void reload() {
		update();
	}

	@Deprecated
	public void createKey(String key, String language, String value) {
		if (!key.startsWith(SYSTEM_KEY_PREFIX)) {
			key = SYSTEM_KEY_PREFIX + key;
		}
		LocalizationKey localizationKey = LocalizationKey.create()
				.setKey(key)
				.setLocalizationKeyType(LocalizationKeyType.SYSTEM_KEY)
				.setUsed(true)
				.save();
		LocalizationValue localizationValue = LocalizationValue.create()
				.setLocalizationKey(localizationKey)
				.setLanguage(language)
				.setOriginal(value)
				.save();
		localizationLanguageValueMapByKey.computeIfAbsent(localizationKey.getKey(), k -> new HashMap<>()).put(localizationValue.getLanguage(), localizationValue);
		//todo create translation values
	}

	public String getLocalizationValue(String key, List<String> languagePriorityOrder) {
		Map<String, LocalizationValue> languageValueMap = localizationLanguageValueMapByKey.get(key);
		if (languageValueMap != null) {
			for (String language : languagePriorityOrder) {
				LocalizationValue value = languageValueMap.get(language);
				if (value != null && value.getCurrentDisplayValue() != null) {
					return value.getCurrentDisplayValue();
				}
			}
		}
		return key;
	}


}
