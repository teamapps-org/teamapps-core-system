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
package org.teamapps.application.server.messaging;

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
import org.teamapps.application.server.messaging.newsboard.NewsBoardPerspectiveBuilder;
import org.teamapps.universaldb.schema.SchemaInfoProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MessagingApplication extends AbstractApplicationBuilder {


	public MessagingApplication() {
		super("messagingApp", ApplicationIcons.MAIL, "messaging.title", "messaging.desc");
	}

	@Override
	public List<PerspectiveBuilder> getPerspectiveBuilders() {
		return Arrays.asList(
				new NewsBoardPerspectiveBuilder()
		);
	}

	@Override
	public ApplicationVersion getApplicationVersion() {
		return ApplicationVersion.create(0,6);
	}

	@Override
	public List<ApplicationRole> getApplicationRoles() {
		return MessagingPrivileges.getRoles();
	}

	@Override
	public List<PrivilegeGroup> getPrivilegeGroups() {
		return MessagingPrivileges.getPrivileges();
	}

	@Override
	public LocalizationData getLocalizationData() {
		return LocalizationData.createFromPropertyFiles("org.teamapps.application.server.i18n.messagingApp", getClass().getClassLoader(),
				new LocalizationLanguages(
						Locale.ENGLISH
				).setMachineTranslatedLanguages(
						Locale.GERMAN,
						Locale.FRENCH,
						Locale.ITALIAN,
						Locale.JAPANESE,
						Locale.CHINESE,
						Locale.forLanguageTag("bg"),
						Locale.forLanguageTag("cs"),
						Locale.forLanguageTag("da"),
						Locale.forLanguageTag("el"),
						Locale.forLanguageTag("es"),
						Locale.forLanguageTag("et"),
						Locale.forLanguageTag("fi"),
						Locale.forLanguageTag("hu"),
						Locale.forLanguageTag("lt"),
						Locale.forLanguageTag("lv"),
						Locale.forLanguageTag("nl"),
						Locale.forLanguageTag("pl"),
						Locale.forLanguageTag("pt"),
						Locale.forLanguageTag("ro"),
						Locale.forLanguageTag("ru"),
						Locale.forLanguageTag("sk"),
						Locale.forLanguageTag("sl"),
						Locale.forLanguageTag("sv"),
						Locale.forLanguageTag("fa"),
						Locale.forLanguageTag("iw"), //he
						Locale.forLanguageTag("hi"),
						Locale.forLanguageTag("hr"),
						Locale.forLanguageTag("in"), //id
						Locale.forLanguageTag("ko"),
						Locale.forLanguageTag("mk"),
						Locale.forLanguageTag("mn"),
						Locale.forLanguageTag("sr"),
						Locale.forLanguageTag("tr"),
						Locale.forLanguageTag("vi")
				)
		);
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
		return privilegeProvider.isAllowed(MessagingPrivileges.LAUNCH_APPLICATION);
	}
}
