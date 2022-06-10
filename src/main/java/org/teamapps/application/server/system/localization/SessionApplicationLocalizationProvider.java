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

import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.universaldb.index.translation.TranslatableText;

import java.util.List;

public class SessionApplicationLocalizationProvider implements ApplicationLocalizationProvider {

	private final Application application;
	private final List<String> rankedLanguages;
	private final GlobalLocalizationProvider localizationProvider;

	public SessionApplicationLocalizationProvider(Application application, List<String> rankedLanguages, GlobalLocalizationProvider localizationProvider) {
		this.application = application;
		this.rankedLanguages = rankedLanguages;
		this.localizationProvider = localizationProvider;
	}

	public String getLocalized(Application application, String key) {
		return localizationProvider.getLocalized(key, application, rankedLanguages);
	}

	@Override
	public String getLocalized(String key, Object... parameters) {
		return localizationProvider.getLocalized(key, application, rankedLanguages, parameters);
	}

	@Override
	public String getLocalized(String key, List<String> languagePriorityOrder, Object... parameters) {
		return localizationProvider.getLocalized(key, application, languagePriorityOrder, parameters);
	}

	@Override
	public String getLocalized(TranslatableText translatableText) {
		return localizationProvider.getLocalized(translatableText, rankedLanguages);
	}
}
