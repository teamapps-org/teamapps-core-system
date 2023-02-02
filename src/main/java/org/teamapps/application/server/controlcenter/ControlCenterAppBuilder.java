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
package org.teamapps.application.server.controlcenter;

import org.teamapps.application.api.annotation.TeamAppsBootableClass;
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
import org.teamapps.application.server.controlcenter.accesscontrol.AccessControlAppRolePerspectiveBuilder;
import org.teamapps.application.server.controlcenter.accesscontrol.AccessControlPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.applications.ApplicationGroupsPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.applications.ApplicationProvisioningPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.applications.ApplicationUpdatesPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.applications.ApplicationsPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.applocal.AppLocalAdministrationPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.cluster.ClusterPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.core.SystemCoreUpdatePerspectiveBuilder;
import org.teamapps.application.server.controlcenter.database.DataBasePerspectiveBuilder;
import org.teamapps.application.server.controlcenter.monitoring.MonitoringPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.organization.OrganizationChartPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.organization.OrganizationFieldPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.organization.OrganizationPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.organization.OrganizationUnitTypePerspectiveBuilder;
import org.teamapps.application.server.controlcenter.roles.RolesPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.roles.UserRoleAssignmentPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.sessions.SessionsOverviewPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.systemlog.SystemLogPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.systenconfig.ApplicationConfigurationPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.translations.TranslationsPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.users.UsersPerspectiveBuilder;
import org.teamapps.application.server.messaging.newsboard.NewsBoardPerspectiveBuilder;
import org.teamapps.application.server.settings.UserLanguageSettingsPerspectiveBuilder;
import org.teamapps.application.server.system.config.SystemConfig;
import org.teamapps.model.ControlCenterSchema;
import org.teamapps.universaldb.schema.SchemaInfoProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@TeamAppsBootableClass(priority = 3)
public class ControlCenterAppBuilder extends AbstractApplicationBuilder {

	private ApplicationConfig<SystemConfig> applicationConfig;

	public ControlCenterAppBuilder() {
		super("controlCenter", ApplicationIcons.WINDOW_KEY, "application.title", "application.desc");
		SystemConfig config = new SystemConfig();
		applicationConfig = new ApplicationConfig<>();
		applicationConfig.setConfig(config);
	}

	@Override
	public List<PerspectiveBuilder> getPerspectiveBuilders() {
		return Arrays.asList(
				new UsersPerspectiveBuilder(),
				new UserRoleAssignmentPerspectiveBuilder(),
				new SessionsOverviewPerspectiveBuilder(),
				new OrganizationPerspectiveBuilder(),
				new RolesPerspectiveBuilder(),
				new AccessControlPerspectiveBuilder(),
				new AccessControlAppRolePerspectiveBuilder(),
				new ApplicationsPerspectiveBuilder(),
				new TranslationsPerspectiveBuilder(),
				new ApplicationConfigurationPerspectiveBuilder(),
				new ClusterPerspectiveBuilder(),
				new SystemCoreUpdatePerspectiveBuilder(),
				new SystemLogPerspectiveBuilder(),
				new MonitoringPerspectiveBuilder(),
				new DataBasePerspectiveBuilder(),

				new UserLanguageSettingsPerspectiveBuilder(),

				new AppLocalAdministrationPerspectiveBuilder(),
				new ApplicationGroupsPerspectiveBuilder(),
				new ApplicationProvisioningPerspectiveBuilder(),
				new ApplicationUpdatesPerspectiveBuilder(),
				new OrganizationChartPerspectiveBuilder(),
				new OrganizationFieldPerspectiveBuilder(),
				new OrganizationUnitTypePerspectiveBuilder()
		);
	}

	@Override
	public ApplicationVersion getApplicationVersion() {
		return ApplicationVersion.create(0, 63);
	}

	@Override
	public List<ApplicationRole> getApplicationRoles() {
		return Privileges.getRoles();
	}

	@Override
	public List<PrivilegeGroup> getPrivilegeGroups() {
		return Privileges.getPrivileges();
	}

	@Override
	public LocalizationData getLocalizationData() {
		return LocalizationData.createFromPropertyFiles("org.teamapps.application.server.i18n.controlCenter", getClass().getClassLoader(),
				new LocalizationLanguages(
						Locale.ENGLISH
				).setMachineTranslatedLanguages(
						Locale.GERMAN
				)
		);
	}

	@Override
	public SchemaInfoProvider getDatabaseModel() {
		return new ControlCenterSchema();
		//return null;
	}

	@Override
	public ApplicationConfig<SystemConfig> getApplicationConfig() {
		return applicationConfig;
	}

	@Override
	public boolean isApplicationAccessible(ApplicationPrivilegeProvider privilegeProvider) {
		return privilegeProvider.isAllowed(Privileges.LAUNCH_APPLICATION);
	}

}
