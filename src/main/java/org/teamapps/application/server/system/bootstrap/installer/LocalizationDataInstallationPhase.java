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
package org.teamapps.application.server.system.bootstrap.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.application.BaseApplicationBuilder;
import org.teamapps.application.api.localization.LocalizationData;
import org.teamapps.application.api.localization.LocalizationEntry;
import org.teamapps.application.api.localization.LocalizationEntrySet;
import org.teamapps.application.server.system.bootstrap.ApplicationInfo;
import org.teamapps.application.server.system.bootstrap.ApplicationInfoDataElement;
import org.teamapps.application.server.system.config.LocalizationConfig;
import org.teamapps.application.server.system.localization.LocalizationUtil;
import org.teamapps.application.tools.KeyCompare;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.LocalizationKey;
import org.teamapps.model.controlcenter.LocalizationKeyType;
import org.teamapps.model.controlcenter.LocalizationValue;
import org.teamapps.universaldb.index.numeric.NumericFilter;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalizationDataInstallationPhase implements ApplicationInstallationPhase {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final LocalizationConfig localizationConfig;

	public LocalizationDataInstallationPhase(LocalizationConfig localizationConfig) {
		this.localizationConfig = localizationConfig;
	}

	@Override
	public void checkApplication(ApplicationInfo applicationInfo) {
		try {
			if (!applicationInfo.getErrors().isEmpty()) {
				return;
			}
			BaseApplicationBuilder baseApplicationBuilder = applicationInfo.getBaseApplicationBuilder();
			LocalizationData localizationData = baseApplicationBuilder.getLocalizationData();
			if (localizationData == null) {
				applicationInfo.addError("Missing localization data");
				return;
			}
			List<LocalizationEntrySet> localizationEntrySets = localizationData.getLocalizationEntrySets();
			if (!localizationData.containsAnyLanguage(localizationConfig.getAllowedSourceLanguages())) {
				applicationInfo.addError("Error: no supported language!:" + localizationEntrySets
						.stream()
						.map(LocalizationEntrySet::getLanguage)
						.collect(Collectors.joining(", ")));
				return;
			}
			ApplicationInfoDataElement dataInfo = new ApplicationInfoDataElement();
			dataInfo.setData(String.join("\n", getAllEntries(localizationEntrySets)));
			Application application = applicationInfo.getApplication();

			Map<String, Map<String, String>> localizationMapByKey = localizationData.createLocalizationMapByKey();
			List<LocalizationKey> localizationKeys = application == null ? Collections.emptyList() : LocalizationKey.filter().application(NumericFilter.equalsFilter(application.getId())).execute();
			KeyCompare<String, LocalizationKey> keyCompare = new KeyCompare<>(localizationMapByKey.keySet(), localizationKeys, s -> s, LocalizationKey::getKey);
			List<String> newKeys = keyCompare.getAEntriesNotInB();
			for (String key : newKeys) {
				Map<String, String> translations = localizationMapByKey.get(key);
				for (Map.Entry<String, String> entry : translations.entrySet()) {
					String language = entry.getKey();
					String original = entry.getValue();
					dataInfo.added(key + " -> " + language + ":" + original);
				}
			}

			List<LocalizationKey> removedKeys = keyCompare.getBEntriesNotInA();
			removedKeys.stream().flatMap(key -> key.getLocalizationValues().stream()).forEach(value -> {
				dataInfo.removed(value.getLocalizationKey().getKey() + " -> " + value.getLanguage() + ":" + value.getOriginal());
			});
			List<String> existingKeys = keyCompare.getAEntriesInB();
			for (String key : existingKeys) {
				Map<String, String> translations = localizationMapByKey.get(key);
				LocalizationKey localizationKey = keyCompare.getB(key);
				KeyCompare<String, LocalizationValue> languageCompare = new KeyCompare<>(translations.keySet(), localizationKey.getLocalizationValues(), s -> s, LocalizationValue::getLanguage);
				if (languageCompare.isDifferent()) {
					List<String> newLanguages = languageCompare.getAEntriesNotInB();
					newLanguages.forEach(language -> dataInfo.added(key + " -> " + language + ":" + translations.get(language)));
					List<LocalizationValue> removedLanguages = languageCompare.getBEntriesNotInA();
					removedLanguages.forEach(value -> dataInfo.removed(key + " -> " + value.getLanguage() + ":" + value.getOriginal()));
					//todo changed originals
				}
			}
			applicationInfo.setLocalizationData(dataInfo);
		} catch (Exception e) {
			applicationInfo.addError("Error checking localization data:" + e.getMessage());
			LOGGER.error("Error checking localization data:", e);
		}
	}

	@Override
	public void installApplication(ApplicationInfo applicationInfo) {
		LocalizationData localizationData = applicationInfo.getBaseApplicationBuilder().getLocalizationData();
		Application application = applicationInfo.getApplication();
		LocalizationKeyType localizationKeyType = LocalizationKeyType.APPLICATION_RESOURCE_KEY;
		LocalizationUtil.synchronizeLocalizationData(localizationData, application, localizationKeyType, localizationConfig);
	}

	@Override
	public void loadApplication(ApplicationInfo applicationInfo) {

	}


	private static List<String> getAllEntries(List<LocalizationEntrySet> localizationEntrySets) {
		List<String> rows = new ArrayList<>();
		for (LocalizationEntrySet localizationEntrySet : localizationEntrySets) {
			String language = localizationEntrySet.getLanguage();
			for (LocalizationEntry entry : localizationEntrySet.getEntries()) {
				rows.add(language + " -> " + entry.getKey() + ":" + entry.getValue());
			}
		}
		return rows;
	}
}
