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
package org.teamapps.application.server.system.session;

import org.slf4j.event.Level;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.entity.EntityUpdate;
import org.teamapps.application.api.application.perspective.ApplicationPerspective;
import org.teamapps.application.api.config.ApplicationConfig;
import org.teamapps.application.api.desktop.ApplicationDesktop;
import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.application.api.notification.ApplicationNotificationHandler;
import org.teamapps.application.api.organization.UserRoleType;
import org.teamapps.application.api.privilege.*;
import org.teamapps.application.api.search.UserSearch;
import org.teamapps.application.api.state.ReplicatedStateMachine;
import org.teamapps.application.api.ui.UiComponentFactory;
import org.teamapps.application.api.user.SessionUser;
import org.teamapps.application.server.PublicLinkResourceProvider;
import org.teamapps.application.server.ServerMode;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.event.Event;
import org.teamapps.message.protocol.message.Message;
import org.teamapps.model.controlcenter.OrganizationFieldView;
import org.teamapps.model.controlcenter.OrganizationUnitView;
import org.teamapps.model.controlcenter.User;
import org.teamapps.reporting.convert.DocumentConverter;
import org.teamapps.universaldb.index.translation.TranslatableText;
import org.teamapps.universaldb.message.MessageStore;
import org.teamapps.universaldb.record.EntityBuilder;
import org.teamapps.ux.application.perspective.Perspective;
import org.teamapps.ux.component.progress.MultiProgressDisplay;
import org.teamapps.ux.resource.Resource;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UserSessionApplicationInstanceData implements ApplicationInstanceData {

	private final SystemRegistry registry;
	private final UserSessionData userSessionData;
	private final ApplicationPrivilegeProvider privilegeProvider;
	private final ApplicationLocalizationProvider localizationProvider;
	private final Supplier<DocumentConverter> documentConverterSupplier;
	private final SessionUiComponentFactory componentFactory;

	public UserSessionApplicationInstanceData(UserSessionData userSessionData) {
		this.registry = userSessionData.getRegistry();
		this.userSessionData = userSessionData;
		this.privilegeProvider = null;
		this.localizationProvider = userSessionData.getLocalizationProvider();
		this.documentConverterSupplier = registry.getDocumentConverterSupplier();
		this.componentFactory = userSessionData.getRegistry().getSessionUiComponentFactoryBuilder().build(this, userSessionData.getRegistry(), null);
	}

	@Override
	public SessionUser getUser() {
		return userSessionData.getSessionUser();
	}

	@Override
	public OrganizationFieldView getOrganizationField() {
		return null;
	}

	@Override
	public int getManagedApplicationId() {
		return 0;
	}

	@Override
	public DocumentConverter getDocumentConverter() {
		return documentConverterSupplier != null ? documentConverterSupplier.get() : null;
	}

	@Override
	public MultiProgressDisplay getMultiProgressDisplay() {
		return null;
	}

	@Override
	public File createTempFile() {
		return userSessionData.getRegistry().createTempFile();
	}

	@Override
	public File createTempFile(String prefix, String suffix) {
		return userSessionData.getRegistry().createTempFile(prefix, suffix);
	}

	@Override
	public void showPerspective(Perspective perspective) {

	}

	@Override
	public ApplicationPerspective showApplicationPerspective(String perspectiveName) {
		return null;
	}

	@Override
	public ApplicationDesktop createApplicationDesktop() {
		return userSessionData.getApplicationDesktopSupplier().get();
	}

	@Override
	public boolean isDarkTheme() {
		return false;
	}

	@Override
	public UiComponentFactory getComponentFactory() {
		return componentFactory;
	}

	public LoadedApplication getMainApplication() {
		return null;
	}

	@Override
	public ApplicationConfig<?> getApplicationConfig() {
		return getMainApplication().getBaseApplicationBuilder().getApplicationConfig();
	}

	@Override
	public void writeActivityLog(Level level, String title, String data) {

	}

	@Override
	public void writeExceptionLog(Level level, String title, Throwable throwable) {

	}

	@Override
	public Integer getOrganizationUserWithRole(OrganizationUnitView orgUnit, UserRoleType userRoleType) {
		List<Integer> organizationUsersWithRole = PerspectiveSessionData.getOrganizationUsersWithRole(orgUnit, userRoleType, getOrganizationField());
		if (organizationUsersWithRole != null && !organizationUsersWithRole.isEmpty()) {
			return organizationUsersWithRole.get(0);
		} else {
			return null;
		}
	}

	@Override
	public Integer getOrganizationUserWithDelegatedObjectId(OrganizationUnitView orgUnit, int objectId) {
		List<Integer> organizationUsersWithRole = PerspectiveSessionData.getOrganizationUsersWithDelegatedObjectId(orgUnit, objectId, getOrganizationField());
		if (organizationUsersWithRole != null && !organizationUsersWithRole.isEmpty()) {
			return organizationUsersWithRole.getFirst();
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
	public List<Integer> getOrganizationUsersWithDelegatedObjectId(OrganizationUnitView orgUnit, int objectId) {
		return PerspectiveSessionData.getOrganizationUsersWithDelegatedObjectId(orgUnit, objectId, getOrganizationField());
	}

	@Override
	public List<Integer> getOrganizationUsersWithRole(OrganizationUnitView orgUnit, UserRoleType userRoleType) {
		return PerspectiveSessionData.getOrganizationUsersWithRole(orgUnit, userRoleType, getOrganizationField());
	}

	@Override
	public <ENTITY> void registerEntityUpdateListener(EntityBuilder<ENTITY> entityBuilder, Consumer<EntityUpdate<ENTITY>> listener) {
		userSessionData.getRegistry().registerEntity(entityBuilder, userSessionData.getUser().getId(), listener);
	}

	@Override
	public <TYPE> Event<TYPE> getUserSessionEvent(String name) {
		return userSessionData.getUserSessionEvent(name);
	}

	@Override
	public <TYPE> TwoWayBindableValue<TYPE> getBindableValue(String name) {
		return userSessionData.getBindableValue(name);
	}

	public <TYPE> TwoWayBindableValue<TYPE> getBindableValue(String name, boolean fireAlways) {
		return userSessionData.getBindableValue(name, fireAlways);
	}

	@Override
	public ReplicatedStateMachine getReplicatedStateMachine(String name) {
		return userSessionData.getReplicatedStateMachine(name);
	}

	@Override
	public String createPublicLinkForResource(Resource resource, Duration availabilityDuration) {
		return PublicLinkResourceProvider.getInstance().createLinkForResource(resource, availabilityDuration);
	}

	@Override
	public <MESSAGE extends Message> MessageStore<MESSAGE> getMessageStore(String name) {
		return null;
	}

	@Override
	public UserSearch createUserSearch(String authCode) {
		return userSessionData.createUserSearch(authCode, this);
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

	@Override
	public Map<OrganizationFieldView, ApplicationPrivilegeProvider> getInheritedOrganizationFieldPrivilegeProviderMap() {
		return privilegeProvider.getInheritedOrganizationFieldPrivilegeProviderMap();
	}

	@Override
	public ApplicationNotificationHandler getNotificationHandler() {
		return userSessionData.getRegistry().getSystemAppNotificationHandler().getNotificationHandler(getOrganizationField());
	}

	@Override
	public ServerMode getServerMode() {
		return userSessionData.getServerMode();
	}

	@Override
	public Set<Integer> getGroupMemberships() {
		return userSessionData.getGroupIds();
	}

	@Override
	public boolean isGroupModerator(int groupId) {
		return userSessionData.getGroupModerationIds().contains(groupId);
	}
}
