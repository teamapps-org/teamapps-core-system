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
package org.teamapps.application.server.system.localization;

import org.teamapps.application.api.localization.LocalizationData;
import org.teamapps.application.server.system.config.LocalizationConfig;
import org.teamapps.model.controlcenter.LocalizationKey;
import org.teamapps.model.controlcenter.LocalizationKeyType;
import org.teamapps.model.controlcenter.LocalizationValue;
import org.teamapps.application.server.system.machinetranslation.TranslationService;
import org.teamapps.universaldb.index.enumeration.EnumFilterType;
import org.teamapps.universaldb.index.numeric.NumericFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictionaryLocalizationProvider implements LocalizationProvider {

	private final LocalizationConfig localizationConfig;
	private Map<String, Map<String, LocalizationValue>> localizationLanguageValueMapByKey;

	public DictionaryLocalizationProvider(LocalizationConfig localizationConfig) {
		this.localizationConfig = localizationConfig;
		synchronizeDictionaryData(localizationConfig);
		loadDictionary();
	}

	public void translateDictionary(TranslationService translationService) {
		LocalizationUtil.translateAllDictionaryValues(translationService, localizationConfig);
	}

	private void synchronizeDictionaryData(LocalizationConfig localizationConfig) {
		LocalizationUtil.synchronizeLocalizationData(LocalizationData.createDictionaryData(getClass().getClassLoader()), null, LocalizationKeyType.DICTIONARY_KEY, localizationConfig);
	}

	private void loadDictionary() {
		localizationLanguageValueMapByKey = new HashMap<>();
		LocalizationKey.filter()
				.application(NumericFilter.equalsFilter(0))
				.localizationKeyType(EnumFilterType.EQUALS, LocalizationKeyType.DICTIONARY_KEY)
				.execute()
				.stream()
				.flatMap(key -> key.getLocalizationValues().stream()).forEach(value -> {
			localizationLanguageValueMapByKey.computeIfAbsent(value.getLocalizationKey().getKey(), k -> new HashMap<>()).put(value.getLanguage(), value);
		});
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
