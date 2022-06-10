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

import com.ibm.icu.text.MessageFormat;
import org.teamapps.application.api.localization.LocalizationData;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.LocalizationKey;
import org.teamapps.model.controlcenter.LocalizationValue;
import org.teamapps.universaldb.index.translation.TranslatableText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalLocalizationProvider {

	private final DictionaryLocalizationProvider dictionary;
	private final SystemLocalizationProvider systemDictionary;
	private Map<String, Map<String, LocalizationValue>> allKeysMap;
	private Map<Application, Map<String, Map<String, LocalizationValue>>> applicationLocalizationMap;
	
	public GlobalLocalizationProvider(SystemRegistry registry) {
		this.dictionary = registry.getDictionary();
		this.systemDictionary = registry.getSystemDictionary();
		updateLocalizationData();
	}

	public void updateLocalizationData() {
		try {
			Map<String, Map<String, LocalizationValue>> allKeysMap = new HashMap<>();
			Map<Application, Map<String, Map<String, LocalizationValue>>> applicationLocalizationMap = new HashMap<>();
			LocalizationKey.filter()
					.execute()
					.stream()
					.flatMap(key -> key.getLocalizationValues().stream()).forEach(value -> {
				allKeysMap.computeIfAbsent(value.getLocalizationKey().getKey(), k -> new HashMap<>()).put(value.getLanguage(), value);
				applicationLocalizationMap
						.computeIfAbsent(value.getLocalizationKey().getApplication(), k -> new HashMap<>())
						.computeIfAbsent(value.getLocalizationKey().getKey(), k -> new HashMap<>()).put(value.getLanguage(), value);
			});
			this.allKeysMap = allKeysMap;
			this.applicationLocalizationMap = applicationLocalizationMap;
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public String getLocalized(String key, Application application, List<String> rankedLanguages) {
		if (key == null || key.isEmpty()) {
			return key;
		}
		if (key.startsWith(LocalizationData.DICTIONARY_PREFIX)) {
			return dictionary.getLocalizationValue(key, rankedLanguages);
		} else if (key.startsWith(SystemLocalizationProvider.SYSTEM_KEY_PREFIX)) {
			return systemDictionary.getLocalizationValue(key, rankedLanguages);
		} else {
			if (application != null) {
				return getLocalizationValue(key, applicationLocalizationMap.get(application), rankedLanguages);
			} else {
				return getLocalizationValue(key, allKeysMap, rankedLanguages);
			}
		}
	}

	private String getLocalizationValue(String key, Map<String, Map<String, LocalizationValue>> localizationMap, List<String> rankedLanguages) {
		if (localizationMap == null) {
			return key;
		}
		Map<String, LocalizationValue> languageValueMap = localizationMap.get(key);
		if (languageValueMap != null) {
			for (String language : rankedLanguages) {
				LocalizationValue value = languageValueMap.get(language);
				if (value != null && value.getCurrentDisplayValue() != null) {
					return value.getCurrentDisplayValue();
				}
			}
		}
		return key;
	}

	public String getLocalized(String key, Application application, List<String> rankedLanguage, Object... parameters) {
		String localizationValue = getLocalized(key, application, rankedLanguage);
		if (parameters != null && parameters.length > 0) {
			try {
				return MessageFormat.format(localizationValue, parameters);
			} catch (Exception e) {
				e.printStackTrace();
				return localizationValue;
			}
		} else {
			return localizationValue;
		}
	}

	public String getLocalized(TranslatableText translatableText, List<String> rankedLanguages) {
		if (translatableText == null) {
			return null;
		} else {
			Map<String, String> translationMap = translatableText.getTranslationMap();
			for (String language : rankedLanguages) {
				String value = translationMap.get(language);
				if (value != null) {
					return value;
				}
			}
			return translatableText.getText();
		}
	}
}
