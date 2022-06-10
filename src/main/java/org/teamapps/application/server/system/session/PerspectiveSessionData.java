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
package org.teamapps.application.server.system.session;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.event.Level;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.entity.EntityUpdate;
import org.teamapps.application.api.application.perspective.ApplicationPerspective;
import org.teamapps.application.api.application.perspective.PerspectiveBuilder;
import org.teamapps.application.api.config.ApplicationConfig;
import org.teamapps.application.api.desktop.ApplicationDesktop;
import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.application.api.organization.UserRoleType;
import org.teamapps.application.api.privilege.*;
import org.teamapps.application.api.ui.UiComponentFactory;
import org.teamapps.application.api.user.SessionUser;
import org.teamapps.application.server.system.launcher.PerspectiveByNameLauncher;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.utils.LogUtils;
import org.teamapps.application.server.system.utils.RoleUtils;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.reporting.convert.DocumentConverter;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.universaldb.index.translation.TranslatableText;
import org.teamapps.universaldb.record.EntityBuilder;
import org.teamapps.ux.application.perspective.Perspective;
import org.teamapps.ux.component.progress.MultiProgressDisplay;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PerspectiveSessionData implements ApplicationInstanceData {

	private final ManagedApplicationSessionData managedApplicationSessionData;
	private final ManagedApplication managedApplication;
	private final ManagedApplicationPerspective managedApplicationPerspective;
	private final PerspectiveBuilder perspectiveBuilder;
	private PerspectiveByNameLauncher perspectiveByNameLauncher;
	private final ApplicationPrivilegeProvider privilegeProvider;
	private final ApplicationLocalizationProvider localizationProvider;
	private final Supplier<DocumentConverter> documentConverterSupplier;
	private final UserSessionData userSessionData;
	private final SessionUiComponentFactory componentFactory;

	public PerspectiveSessionData(ManagedApplicationSessionData managedApplicationSessionData, ManagedApplication managedApplication, ManagedApplicationPerspective managedApplicationPerspective, PerspectiveBuilder perspectiveBuilder, PerspectiveByNameLauncher perspectiveByNameLauncher, ApplicationPrivilegeProvider privilegeProvider, ApplicationLocalizationProvider localizationProvider, Supplier<DocumentConverter> documentConverterSupplier) {
		this.managedApplicationSessionData = managedApplicationSessionData;
		this.managedApplication = managedApplication;
		this.managedApplicationPerspective = managedApplicationPerspective;
		this.perspectiveBuilder = perspectiveBuilder;
		this.perspectiveByNameLauncher = perspectiveByNameLauncher;
		this.privilegeProvider = privilegeProvider;
		this.localizationProvider = localizationProvider;
		this.documentConverterSupplier = documentConverterSupplier;
		this.userSessionData = managedApplicationSessionData.getUserSessionData();
		this.componentFactory = new SessionUiComponentFactory(this, userSessionData.getRegistry(), managedApplication.getMainApplication());
	}

	public Icon getIcon() {
		if (managedApplicationPerspective.getIconOverride() != null) {
			return IconUtils.decodeIcon(managedApplicationPerspective.getIconOverride());
		} else {
			return perspectiveBuilder.getIcon();
		}
	}

	public String getTitle() {
		if (managedApplicationPerspective.getTitleKeyOverride() != null) {
			return localizationProvider.getLocalized(managedApplicationPerspective.getTitleKeyOverride());
		} else {
			return localizationProvider.getLocalized(perspectiveBuilder.getTitleKey());
		}
	}

	public String getDescription() {
		if (managedApplicationPerspective.getDescriptionKeyOverride() != null) {
			return localizationProvider.getLocalized(managedApplicationPerspective.getDescriptionKeyOverride());
		} else {
			return localizationProvider.getLocalized(perspectiveBuilder.getDescriptionKey());
		}
	}

	public ManagedApplicationPerspective getManagedApplicationPerspective() {
		return managedApplicationPerspective;
	}

	public ManagedApplicationSessionData getManagedApplicationSessionData() {
		return managedApplicationSessionData;
	}

	public PerspectiveBuilder getPerspectiveBuilder() {
		return perspectiveBuilder;
	}

	@Override
	public SessionUser getUser() {
		return managedApplicationSessionData.getUserSessionData().getSessionUser();
	}

	@Override
	public OrganizationFieldView getOrganizationField() {
		return managedApplicationSessionData.getOrganizationFieldView();
	}

	@Override
	public int getManagedApplicationId() {
		return managedApplication.getId();
	}

	@Override
	public DocumentConverter getDocumentConverter() {
		return documentConverterSupplier != null ? documentConverterSupplier.get() : null;
	}

	@Override
	public MultiProgressDisplay getMultiProgressDisplay() {
		return managedApplicationSessionData.getResponsiveApplication().getMultiProgressDisplay();
	}

	@Override
	public void showPerspective(Perspective perspective) {
		managedApplicationSessionData.getResponsiveApplication().showPerspective(perspective);
	}

	@Override
	public ApplicationPerspective showApplicationPerspective(String perspectiveName) {
		return perspectiveByNameLauncher.showApplicationPerspective(perspectiveName);
	}

	@Override
	public ApplicationDesktop createApplicationDesktop() {
		return userSessionData.getApplicationDesktopSupplier().get();
	}

	@Override
	public UiComponentFactory getComponentFactory() {
		return componentFactory;
	}

	@Override
	public boolean isDarkTheme() {
		return managedApplication.getDarkTheme();
	}


	@Override
	public ApplicationConfig<?> getApplicationConfig() {
		return managedApplicationSessionData.getMainApplication().getBaseApplicationBuilder().getApplicationConfig();
	}

	@Override
	public void writeActivityLog(Level level, String title, String data) {
		LogLevel logLevel = LogUtils.convert(level);
		if (logLevel == null) {
			return;
		}
		SystemLog.create()
				.setManagedApplication(managedApplication)
				.setManagedPerspective(managedApplicationPerspective)
				.setApplication(managedApplicationPerspective.getApplicationPerspective().getApplication())
				.setLogLevel(logLevel)
				.setMessage(title)
				.setDetails(data)
				.save();
	}

	@Override
	public void writeExceptionLog(Level level, String title, Throwable throwable) {
		LogLevel logLevel = LogUtils.convert(level);
		if (logLevel == null) {
			return;
		}
		SystemLog.create()
				.setManagedApplication(managedApplication)
				.setManagedPerspective(managedApplicationPerspective)
				.setApplication(managedApplicationPerspective.getApplicationPerspective().getApplication())
				.setLogLevel(logLevel)
				.setMessage(title)
				.setDetails(ExceptionUtils.getStackTrace(throwable))
				.save();
	}

	@Override
	public Integer getOrganizationUserWithRole(OrganizationUnitView orgUnit, UserRoleType userRoleType) {
		List<Integer> organizationUsersWithRole = getOrganizationUsersWithRole(orgUnit, userRoleType, getOrganizationField());
		if (organizationUsersWithRole != null && !organizationUsersWithRole.isEmpty()) {
			return organizationUsersWithRole.get(0);
		} else {
			return null;
		}
	}

	@Override
	public String getOrganizationUserNameWithRole(OrganizationUnitView orgUnit, UserRoleType userRoleType, boolean lastNameFirst) {
		Integer userId = getOrganizationUserWithRole(orgUnit, userRoleType);
		if (userId != null) {
			User user = User.getById(userId);
			return lastNameFirst ? user.getLastName() + ", " + user.getFirstName() : user.getFirstName() + " " + user.getLastName();
		} else {
			return null;
		}
	}

	@Override
	public List<Integer> getOrganizationUsersWithRole(OrganizationUnitView orgUnit, UserRoleType userRoleType) {
		return getOrganizationUsersWithRole(orgUnit, userRoleType, getOrganizationField());
	}

	@Override
	public <ENTITY> void registerEntity(EntityBuilder<ENTITY> entityBuilder, Consumer<EntityUpdate<ENTITY>> listener) {
		userSessionData.getRegistry().registerEntity(entityBuilder, userSessionData.getUser().getId(), listener);
	}

	public static List<Integer> getOrganizationUsersWithRole(OrganizationUnitView orgUnit, UserRoleType userRoleType, OrganizationFieldView organizationFieldView) {
		if (userRoleType == null) {
			return null;
		}
		RoleType roleType = switch (userRoleType) {
			case LEADER -> RoleType.LEADER;
			case ASSISTANT -> RoleType.ASSISTANT;
			case MENTOR -> RoleType.MENTOR;
			case ADMINISTRATOR -> RoleType.ADMINISTRATOR;
			case OTHER -> RoleType.OTHER;
		};
		OrganizationField organizationField = OrganizationUtils.convert(organizationFieldView);
		return UserRoleAssignment.filter()
				.organizationUnit(NumericFilter.equalsFilter(orgUnit.getId()))
				.execute()
				.stream()
				.filter(userRoleAssignment -> organizationField == null || userRoleAssignment.getRole().getOrganizationField().equals(organizationField))
				.filter(userRoleAssignment -> userRoleAssignment.getRole().getRoleType() == roleType)
				.filter(userRoleAssignment -> userRoleAssignment.getUser() != null)
				.sorted(RoleUtils.createRoleTypeAndMainResponsibleComparator())
				.map(assignment -> assignment.getUser().getId())
				.collect(Collectors.toList());
	}

	@Override
	public String getLocalized(String s, Object... objects) {
		return localizationProvider.getLocalized(s, objects);
	}

	@Override
	public String getLocalized(String key, List<String> languagePriorityOrder, Object... parameters) {
		return localizationProvider.getLocalized(key, languagePriorityOrder, parameters);
	}

	@Override
	public String getLocalized(TranslatableText translatableText) {
		return localizationProvider.getLocalized(translatableText);
	}

	@Override
	public boolean isAllowed(SimplePrivilege simplePrivilege) {
		return privilegeProvider.isAllowed(simplePrivilege);
	}

	@Override
	public boolean isAllowed(SimpleOrganizationalPrivilege simpleOrganizationalPrivilege, OrganizationUnitView organizationUnitView) {
		return privilegeProvider.isAllowed(simpleOrganizationalPrivilege, organizationUnitView);
	}

	@Override
	public boolean isAllowed(SimpleCustomObjectPrivilege simpleCustomObjectPrivilege, PrivilegeObject privilegeObject) {
		return privilegeProvider.isAllowed(simpleCustomObjectPrivilege, privilegeObject);
	}

	@Override
	public boolean isAllowed(StandardPrivilegeGroup standardPrivilegeGroup, Privilege privilege) {
		return privilegeProvider.isAllowed(standardPrivilegeGroup, privilege);
	}

	@Override
	public boolean isAllowed(OrganizationalPrivilegeGroup organizationalPrivilegeGroup, Privilege privilege, OrganizationUnitView organizationUnitView) {
		return privilegeProvider.isAllowed(organizationalPrivilegeGroup, privilege, organizationUnitView);
	}

	@Override
	public boolean isAllowed(CustomObjectPrivilegeGroup customObjectPrivilegeGroup, Privilege privilege, PrivilegeObject privilegeObject) {
		return privilegeProvider.isAllowed(customObjectPrivilegeGroup, privilege, privilegeObject);
	}

	@Override
	public boolean isAllowed(RoleAssignmentDelegatedCustomPrivilegeGroup group, Privilege privilege, PrivilegeObject privilegeObject) {
		return privilegeProvider.isAllowed(group, privilege, privilegeObject);
	}

	@Override
	public List<OrganizationUnitView> getAllowedUnits(SimpleOrganizationalPrivilege simpleOrganizationalPrivilege) {
		return privilegeProvider.getAllowedUnits(simpleOrganizationalPrivilege);
	}

	@Override
	public List<OrganizationUnitView> getAllowedUnits(OrganizationalPrivilegeGroup organizationalPrivilegeGroup, Privilege privilege) {
		return privilegeProvider.getAllowedUnits(organizationalPrivilegeGroup, privilege);
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(SimpleCustomObjectPrivilege simpleCustomObjectPrivilege) {
		return privilegeProvider.getAllowedPrivilegeObjects(simpleCustomObjectPrivilege);
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(CustomObjectPrivilegeGroup customObjectPrivilegeGroup, Privilege privilege) {
		return privilegeProvider.getAllowedPrivilegeObjects(customObjectPrivilegeGroup, privilege);
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(RoleAssignmentDelegatedCustomPrivilegeGroup group, Privilege privilege) {
		return privilegeProvider.getAllowedPrivilegeObjects(group, privilege);
	}
}
