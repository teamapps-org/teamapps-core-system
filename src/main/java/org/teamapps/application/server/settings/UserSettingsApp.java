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
package org.teamapps.application.server.settings;

import org.teamapps.application.api.application.AbstractApplicationBuilder;
import org.teamapps.application.api.application.perspective.PerspectiveBuilder;
import org.teamapps.application.api.config.ApplicationConfig;
import org.teamapps.application.api.localization.LocalizationData;
import org.teamapps.application.api.localization.LocalizationLanguages;
import org.teamapps.application.api.privilege.ApplicationPrivilegeProvider;
import org.teamapps.application.api.privilege.ApplicationRole;
import org.teamapps.application.api.privilege.PrivilegeGroup;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.api.versioning.ApplicationVersion;
import org.teamapps.universaldb.schema.SchemaInfoProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class UserSettingsApp extends AbstractApplicationBuilder {

	public UserSettingsApp() {
		super("userSettings", ApplicationIcons.WINDOW_GEAR, "userSettings.title", "userSettings.desc");
	}

	@Override
	public List<PerspectiveBuilder> getPerspectiveBuilders() {
		return Arrays.asList(
				new UserLanguageSettingsPerspectiveBuilder()
		);
	}

	@Override
	public ApplicationVersion getApplicationVersion() {
		return ApplicationVersion.create(0, 1);
	}

	@Override
	public List<ApplicationRole> getApplicationRoles() {
		return null;
	}

	@Override
	public List<PrivilegeGroup> getPrivilegeGroups() {
		return Collections.emptyList();
	}

	@Override
	public LocalizationData getLocalizationData() {
		return LocalizationData.createFromPropertyFiles("org.teamapps.application.server.i18n.userSettings", getClass().getClassLoader(),
				new LocalizationLanguages(
						Locale.ENGLISH
				).setMachineTranslatedLanguages(
						Locale.GERMAN
				));
	}

	@Override
	public SchemaInfoProvider getDatabaseModel() {
		return null;
	}

	@Override
	public ApplicationConfig getApplicationConfig() {
		return null;
	}

	@Override
	public boolean isApplicationAccessible(ApplicationPrivilegeProvider privilegeProvider) {
		return true;
	}
}
