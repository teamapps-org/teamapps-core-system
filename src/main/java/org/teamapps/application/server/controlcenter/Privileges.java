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
package org.teamapps.application.server.controlcenter;

import org.teamapps.application.api.privilege.*;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.controlcenter.organization.OrganizationChartPerspective;
import org.teamapps.icons.composite.CompositeIcon;

import java.util.List;

import static org.teamapps.application.api.privilege.Privilege.*;

public class Privileges {

	private final static ApplicationPrivilegeBuilder PRIVILEGE_BUILDER = new ApplicationPrivilegeBuilder();

	private final static ApplicationRoleBuilder ROLE_BUILDER = new ApplicationRoleBuilder();

	public final static SimplePrivilege LAUNCH_APPLICATION = PRIVILEGE_BUILDER.LAUNCH_APPLICATION;
	public final static SimplePrivilege LAUNCH_PERSPECTIVE_APPLICATION_GROUPS = PRIVILEGE_BUILDER.addSimplePrivilege("launchPerspectiveApplicationGroups", ApplicationIcons.WINDOWS, "applicationGroups.launch", "applicationGroups.desc");
	public final static SimplePrivilege LAUNCH_PERSPECTIVE_APPLICATION_PROVISIONING = PRIVILEGE_BUILDER.addSimplePrivilege("launchPerspectiveApplicationProvisioning", ApplicationIcons.INSTALL, "applicationProvisioning.launch", "applicationProvisioning.desc");
	public final static SimplePrivilege LAUNCH_PERSPECTIVE_APPLICATIONS = PRIVILEGE_BUILDER.addSimplePrivilege("launchPerspectiveApplications", ApplicationIcons.BOX_SOFTWARE, "applications.launch", "applications.desc");
	public final static SimplePrivilege LAUNCH_PERSPECTIVE_APP_LOCAL_ADMINISTRATION = PRIVILEGE_BUILDER.addSimplePrivilege("launchPerspectiveAppLocalAdministration", ApplicationIcons.WINDOW_KEY, "appLocalAdministration.launch", "appLocalAdministration.desc");
	public final static SimplePrivilege LAUNCH_PERSPECTIVE_APPLICATION_CONFIGURATION = PRIVILEGE_BUILDER.addSimplePrivilege("launchPerspectiveApplicationConfiguration", ApplicationIcons.CODE_LINE, "applicationConfiguration.launch", "applicationConfiguration.desc");
	public final static SimplePrivilege LAUNCH_PERSPECTIVE_MACHINE_TRANSLATION = PRIVILEGE_BUILDER.addSimplePrivilege("launchPerspectiveMachineTranslation", ApplicationIcons.EARTH_LINK, "machineTranslation.launch", "machineTranslation.desc");
	public final static SimplePrivilege LAUNCH_PERSPECTIVE_TRANSLATIONS = PRIVILEGE_BUILDER.addSimplePrivilege("launchPerspectiveTranslations", ApplicationIcons.SPELL_CHECK, "translations.launch", "translations.desc");
	public final static SimplePrivilege LAUNCH_PERSPECTIVE_MONITORING = PRIVILEGE_BUILDER.addSimplePrivilege("launchPerspectiveMonitoring", ApplicationIcons.CHART_LINE, "monitoring.launch", "monitoring.desc");


	public final static OrganizationalPrivilegeGroup USERS_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultOrganizationalPrivilegeGroup("usersPerspective", ApplicationIcons.USERS_CROWD, "users.title", "users.desc");
	public final static OrganizationalPrivilegeGroup USER_ROLE_ASSIGNMENT_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultOrganizationalPrivilegeGroup("userRoleAssignmentPerspective", ApplicationIcons.USERS_THREE_RELATION, "userRoleAssignment.title", "userRoleAssignment.desc");
	public final static OrganizationalPrivilegeGroup ORGANIZATION_UNIT_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultOrganizationalPrivilegeGroup("organizationUnitPerspective", ApplicationIcons.ELEMENTS_HIERARCHY, "organization.title", "organization.desc");
	public final static StandardPrivilegeGroup ORGANIZATION_UNIT_TYPE_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultStandardPrivilegeGroup("organizationUnitTypePerspective", ApplicationIcons.ELEMENTS_CASCADE, "organizationUnitType.title", "organizationUnitType.desc");
	public final static StandardPrivilegeGroup ORGANIZATION_FIELD_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultStandardPrivilegeGroup("organizationFieldPerspective", ApplicationIcons.ELEMENTS_TREE, "organizationField.title", "organizationField.desc");
	public final static StandardPrivilegeGroup ROLES_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultStandardPrivilegeGroup("rolesPerspective", ApplicationIcons.WORKER, "roles.title", "roles.desc");
	public final static OrganizationalPrivilegeGroup ACCESS_CONTROL_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultOrganizationalPrivilegeGroup("accessControlPerspective", ApplicationIcons.KEYS, "accessControl.title", "accessControl.desc");
	public final static OrganizationalPrivilegeGroup ACCESS_CONTROL_APP_ROLE_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultOrganizationalPrivilegeGroup("accessControlAppRolePerspective", CompositeIcon.of(ApplicationIcons.KEY, ApplicationIcons.WORKER), "accessControlAppRole.title", "accessControlAppRole.desc");
	public final static StandardPrivilegeGroup APPLICATION_UPDATES_PERSPECTIVE = PRIVILEGE_BUILDER.addStandardPrivilegeGroup("applicationUpdatesPerspective", ApplicationIcons.BOX_SOFTWARE, "applicationUpdates.title", "applicationUpdates.desc", READ, EXECUTE, RESTORE);
	//public final static StandardPrivilegeGroup APPLICATIONS_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultStandardPrivilegeGroup("applications", ApplicationIcons.BOX_SOFTWARE, "applications.title", "applications.desc");
	public final static StandardPrivilegeGroup APPLICATION_PROVISIONING_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultStandardPrivilegeGroup("applicationProvisioning", ApplicationIcons.INSTALL, "applicationProvisioning.title", "applicationProvisioning.desc");
	//public final static StandardPrivilegeGroup APPLICATION_GROUPS_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultStandardPrivilegeGroup("applicationGroups", ApplicationIcons.WINDOWS, "applicationGroups.title", "applicationGroups.desc");
	//public final static StandardPrivilegeGroup APPLICATION_GROUPS_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultStandardPrivilegeGroup("applicationGroups", ApplicationIcons.WINDOWS, "applicationGroups.title", "applicationGroups.desc");
	//public final static StandardPrivilegeGroup APPLICATION_CONFIGURATION_PERSPECTIVE = PRIVILEGE_BUILDER.addDefaultStandardPrivilegeGroup("applicationConfigurationPerspective", ApplicationIcons.CODE_LINE, "applicationConfiguration.title", "applicationConfiguration.desc");
	public final static StandardPrivilegeGroup SYSTEM_LOG_PERSPECTIVE = PRIVILEGE_BUILDER.addStandardPrivilegeGroup("systemLogPerspective", ApplicationIcons.CONSOLE, "systemLog.title", "systemLog.desc", READ);
	public final static StandardPrivilegeGroup DATABASE_PERSPECTIVE = PRIVILEGE_BUILDER.addStandardPrivilegeGroup("databasePerspective", ApplicationIcons.DATA_TABLE, "database.launch", "database.desc", READ, SHOW_RECYCLE_BIN);

	public final static StandardPrivilegeGroup CLUSTER_PERSPECTIVE = PRIVILEGE_BUILDER.addStandardPrivilegeGroup("clusterPerspective", ApplicationIcons.RACK_SERVER_NETWORK, "cluster.launch", "cluster.desc", READ, CREATE, UPDATE, DELETE);

	public final static OrganizationalPrivilegeGroup ORGANIZATION_CHART_PERSPECTIVE = PRIVILEGE_BUILDER.addOrganizationalPrivilegeGroup("organizationChartPerspective", ApplicationIcons.PIECES, "organizationChart.title", "organizationChart.desc",
			OrganizationChartPerspective.SHOW_UPWARDS_LEADERS,
			OrganizationChartPerspective.SHOW_UPWARDS_ALL_ROLES,
			OrganizationChartPerspective.SHOW_DOWNWARDS_LEADERS,
			OrganizationChartPerspective.SHOW_DOWNWARDS_ALL_ROLES
			);

	public final static ApplicationRole APPLICATION_ADMINISTRATOR_ROLE = ROLE_BUILDER.addRole("applicationAdministratorRole", ApplicationIcons.PILOT, "application.roles.applicationAdministrator", "application.roles.applicationAdministrator.desc",
			LAUNCH_APPLICATION,
			LAUNCH_PERSPECTIVE_APP_LOCAL_ADMINISTRATION,
			LAUNCH_PERSPECTIVE_TRANSLATIONS,
			USER_ROLE_ASSIGNMENT_PERSPECTIVE.createCopyWithPrivileges(READ, CREATE, UPDATE, DELETE, SHOW_MODIFICATION_HISTORY),
			ROLES_PERSPECTIVE.createCopyWithPrivileges(READ, CREATE, UPDATE, DELETE, SHOW_MODIFICATION_HISTORY),
			ACCESS_CONTROL_PERSPECTIVE.createCopyWithPrivileges(READ, CREATE, UPDATE, DELETE, SHOW_MODIFICATION_HISTORY),
			ACCESS_CONTROL_APP_ROLE_PERSPECTIVE.createCopyWithPrivileges(READ, CREATE, UPDATE, DELETE, SHOW_MODIFICATION_HISTORY),
			APPLICATION_UPDATES_PERSPECTIVE.createCopyWithPrivileges(READ, EXECUTE, RESTORE),
			SYSTEM_LOG_PERSPECTIVE,
			LAUNCH_PERSPECTIVE_APPLICATION_CONFIGURATION,
			DATABASE_PERSPECTIVE.createCopyWithPrivileges(READ, SHOW_RECYCLE_BIN)
	);

	public final static ApplicationRole APPLICATION_DEVELOPER_ROLE = ROLE_BUILDER.addRole("applicationDeveloperRole", ApplicationIcons.TEXT_BINARY, "application.roles.applicationDeveloper", "application.roles.applicationDeveloper.desc",
			LAUNCH_PERSPECTIVE_APP_LOCAL_ADMINISTRATION,
			LAUNCH_PERSPECTIVE_TRANSLATIONS,
			USER_ROLE_ASSIGNMENT_PERSPECTIVE.createCopyWithPrivileges(READ),
			ROLES_PERSPECTIVE.createCopyWithPrivileges(READ),
			ACCESS_CONTROL_PERSPECTIVE.createCopyWithPrivileges(READ),
			ACCESS_CONTROL_APP_ROLE_PERSPECTIVE.createCopyWithPrivileges(READ),
			APPLICATION_UPDATES_PERSPECTIVE.createCopyWithPrivileges(READ, EXECUTE, RESTORE),
			SYSTEM_LOG_PERSPECTIVE,
			LAUNCH_PERSPECTIVE_APPLICATION_CONFIGURATION,
			DATABASE_PERSPECTIVE.createCopyWithPrivileges(READ, SHOW_RECYCLE_BIN)
	);

	public static List<ApplicationRole> getRoles() {
		return ROLE_BUILDER.getRoles();
	}

	public static List<PrivilegeGroup> getPrivileges() {
		return PRIVILEGE_BUILDER.getPrivileges();
	}
}
