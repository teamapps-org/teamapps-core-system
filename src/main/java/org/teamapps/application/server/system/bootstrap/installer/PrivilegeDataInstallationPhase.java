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
import org.teamapps.application.api.privilege.ApplicationRole;
import org.teamapps.application.api.privilege.Privilege;
import org.teamapps.application.api.privilege.PrivilegeGroup;
import org.teamapps.application.api.privilege.PrivilegeGroupType;
import org.teamapps.application.server.system.bootstrap.ApplicationInfo;
import org.teamapps.application.server.system.bootstrap.ApplicationInfoDataElement;
import org.teamapps.application.server.system.privilege.ApplicationScopePrivilegeProvider;
import org.teamapps.application.tools.KeyCompare;
import org.teamapps.application.server.system.utils.ValueCompare;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.ApplicationPrivilege;
import org.teamapps.model.controlcenter.ApplicationPrivilegeGroup;
import org.teamapps.model.controlcenter.ApplicationPrivilegeGroupType;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.universaldb.pojo.Entity;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.teamapps.model.controlcenter.ApplicationPrivilegeGroupType.*;
import static org.teamapps.model.controlcenter.ApplicationPrivilegeGroupType.ROLE_ASSIGNMENT_DELEGATED_CUSTOM_PRIVILEGE_GROUP;

public class PrivilegeDataInstallationPhase implements ApplicationInstallationPhase {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void checkApplication(ApplicationInfo applicationInfo) {
		try {
			if (!applicationInfo.getErrors().isEmpty()) {
				return;
			}
			BaseApplicationBuilder baseApplicationBuilder = applicationInfo.getBaseApplicationBuilder();
			List<PrivilegeGroup> privilegeGroups = baseApplicationBuilder.getPrivilegeGroups();
			if (privilegeGroups == null) {
				applicationInfo.addError("Missing privileges");
				return;
			}

			if (!privilegeGroups.stream().map(PrivilegeGroup::getName).allMatch(new HashSet<>()::add)) {
				applicationInfo.addError("Privilege groups with same name");
				return;
			}

			if (baseApplicationBuilder.getApplicationRoles() != null && !baseApplicationBuilder.getApplicationRoles().stream().map(ApplicationRole::getName).allMatch(new HashSet<>()::add)) {
				applicationInfo.addError("Application roles with same name");
				return;
			}

			ApplicationInfoDataElement dataInfo = new ApplicationInfoDataElement();
			Application application = applicationInfo.getApplication();
			List<String> privilegeGroupInfoData = getPrivilegeGroupInfoData(privilegeGroups);
			dataInfo.setData(String.join("\n", privilegeGroupInfoData));

			if (application == null) {
				dataInfo.setDataAdded(privilegeGroupInfoData);
			} else {
				List<ApplicationPrivilegeGroup> applicationPrivilegeGroups = getApplicationPrivilegeGroups(application);
				KeyCompare<PrivilegeGroup, ApplicationPrivilegeGroup> keyCompare = new KeyCompare<>(privilegeGroups, applicationPrivilegeGroups, PrivilegeGroup::getName, ApplicationPrivilegeGroup::getName);
				List<PrivilegeGroup> newPrivilegeGroups = keyCompare.getAEntriesNotInB();
				dataInfo.setDataAdded(getPrivilegeGroupInfoData(newPrivilegeGroups));

				List<ApplicationPrivilegeGroup> removedPrivilegeGroups = keyCompare.getBEntriesNotInA();
				dataInfo.setDataRemoved(getApplicationPrivilegeGroupInfoData(removedPrivilegeGroups));

				List<PrivilegeGroup> existingGroups = keyCompare.getAEntriesInB();
				for (PrivilegeGroup privilegeGroup : existingGroups) {
					KeyCompare<Privilege, ApplicationPrivilege> privilegeCompare = new KeyCompare<>(privilegeGroup.getPrivileges(), keyCompare.getB(privilegeGroup).getPrivileges(), Privilege::getName, ApplicationPrivilege::getName);
					//privilege diff
				}
			}
			applicationInfo.setPrivilegeData(dataInfo);
		} catch (Exception e) {
			applicationInfo.addError("Error checking privileges:" + e.getMessage());
			LOGGER.error("Error checking privileges:", e);
		}
	}

	@Override
	public void installApplication(ApplicationInfo applicationInfo) {
		BaseApplicationBuilder baseApplicationBuilder = applicationInfo.getBaseApplicationBuilder();
		List<PrivilegeGroup> privilegeGroups = baseApplicationBuilder.getPrivilegeGroups();
		Application application = applicationInfo.getApplication();
		List<ApplicationPrivilegeGroup> applicationPrivilegeGroups = getApplicationPrivilegeGroups(application);

		KeyCompare<PrivilegeGroup, ApplicationPrivilegeGroup> keyCompare = new KeyCompare<>(privilegeGroups, applicationPrivilegeGroups, PrivilegeGroup::getName, ApplicationPrivilegeGroup::getName);
		List<PrivilegeGroup> newPrivilegeGroups = keyCompare.getAEntriesNotInB();
		newPrivilegeGroups.forEach(group -> createApplicationPrivilegeGroup(group, application));

		List<ApplicationPrivilegeGroup> removedPrivilegeGroups = keyCompare.getBEntriesNotInA();
		removedPrivilegeGroups.forEach(Entity::delete);

		List<PrivilegeGroup> existingGroups = keyCompare.getAEntriesInB();
		for (PrivilegeGroup group : existingGroups) {
			ApplicationPrivilegeGroup applicationPrivilegeGroup = keyCompare.getB(group);
			ValueCompare valueCompare = new ValueCompare()
					.check(IconUtils.encodeNoStyle(group.getIcon()), applicationPrivilegeGroup.getIcon())
					.check(group.getName(), applicationPrivilegeGroup.getName())
					.check(group.getTitleKey(), applicationPrivilegeGroup.getTitleKey())
					.check(group.getDescriptionKey(), applicationPrivilegeGroup.getDescriptionKey());

			if (valueCompare.isDifferent()) {
				applicationPrivilegeGroup
						.setIcon(IconUtils.encodeNoStyle(group.getIcon()))
						.setName(group.getName())
						.setTitleKey(group.getTitleKey())
						.setDescriptionKey(group.getDescriptionKey())
						.save();
			}
			KeyCompare<Privilege, ApplicationPrivilege> privilegeCompare = new KeyCompare<>(group.getPrivileges(), applicationPrivilegeGroup.getPrivileges(), Privilege::getName, ApplicationPrivilege::getName);
			List<Privilege> newPrivileges = privilegeCompare.getAEntriesNotInB();
			newPrivileges.forEach(privilege -> createApplicationPrivilege(applicationPrivilegeGroup, privilege));

			List<ApplicationPrivilege> removedPrivileges = privilegeCompare.getBEntriesNotInA();
			removedPrivileges.forEach(Entity::delete);

			List<Privilege> existingPrivileges = privilegeCompare.getAEntriesInB();
			for (Privilege existingPrivilege : existingPrivileges) {
				ApplicationPrivilege applicationPrivilege = privilegeCompare.getB(existingPrivilege);
				if (new ValueCompare()
						.check(IconUtils.encodeNoStyle(existingPrivilege.getIcon()), applicationPrivilege.getIcon())
						.check(existingPrivilege.getTitleKey(), applicationPrivilege.getTitleKey())
						.isDifferent()) {
					applicationPrivilege
							.setIcon(IconUtils.encodeNoStyle(existingPrivilege.getIcon()))
							.setTitleKey(existingPrivilege.getTitleKey())
							.save();
				}
			}
		}
	}

	@Override
	public void loadApplication(ApplicationInfo applicationInfo) {
		applicationInfo.getLoadedApplication().setAppPrivilegeProvider(new ApplicationScopePrivilegeProvider(applicationInfo.getBaseApplicationBuilder()));
	}

	private List<ApplicationPrivilegeGroup> getApplicationPrivilegeGroups(Application application) {
		return ApplicationPrivilegeGroup.filter()
				.application(NumericFilter.equalsFilter(application.getId()))
				.execute();
	}

	private void createApplicationPrivilegeGroup(PrivilegeGroup group, Application application) {
		ApplicationPrivilegeGroup applicationPrivilegeGroup = ApplicationPrivilegeGroup.create()
				.setApplication(application)
				.setIcon(IconUtils.encodeNoStyle(group.getIcon()))
				.setName(group.getName())
				.setTitleKey(group.getTitleKey())
				.setDescriptionKey(group.getDescriptionKey())
				.setApplicationPrivilegeGroupType(getType(group.getType()))
				.save();
		for (Privilege privilege : group.getPrivileges()) {
			createApplicationPrivilege(applicationPrivilegeGroup, privilege);
		}
	}

	private void createApplicationPrivilege(ApplicationPrivilegeGroup applicationPrivilegeGroup, Privilege privilege) {
		ApplicationPrivilege.create()
				.setPrivilegeGroup(applicationPrivilegeGroup)
				.setIcon(IconUtils.encodeNoStyle(privilege.getIcon()))
				.setName(privilege.getName())
				.setTitleKey(privilege.getTitleKey())
				.save();
	}

	private ApplicationPrivilegeGroupType getType(PrivilegeGroupType groupType) {
		switch (groupType) {
			case SIMPLE_PRIVILEGE:
				return SIMPLE_PRIVILEGE;
			case SIMPLE_ORGANIZATIONAL_PRIVILEGE:
				return SIMPLE_ORGANIZATIONAL_PRIVILEGE;
			case SIMPLE_CUSTOM_OBJECT_PRIVILEGE:
				return SIMPLE_CUSTOM_OBJECT_PRIVILEGE;
			case STANDARD_PRIVILEGE_GROUP:
				return STANDARD_PRIVILEGE_GROUP;
			case ORGANIZATIONAL_PRIVILEGE_GROUP:
				return ORGANIZATIONAL_PRIVILEGE_GROUP;
			case CUSTOM_OBJECT_PRIVILEGE_GROUP:
				return CUSTOM_OBJECT_PRIVILEGE_GROUP;
			case ROLE_ASSIGNMENT_DELEGATED_CUSTOM_PRIVILEGE_GROUP:
				return ROLE_ASSIGNMENT_DELEGATED_CUSTOM_PRIVILEGE_GROUP;
			default:
				return null;
		}
	}

	private List<String> getPrivilegeGroupInfoData(List<PrivilegeGroup> privilegeGroups) {
		List<String> infoData = new ArrayList<>();
		for (PrivilegeGroup privilegeGroup : privilegeGroups) {
			infoData.add(privilegeGroup.getName() + ": " + privilegeGroup.getType().name());
			for (Privilege privilege : privilegeGroup.getPrivileges()) {
				infoData.add(" -> " + privilege.getName());
			}
		}
		return infoData;
	}

	private List<String> getApplicationPrivilegeGroupInfoData(List<ApplicationPrivilegeGroup> privilegeGroups) {
		List<String> infoData = new ArrayList<>();
		for (ApplicationPrivilegeGroup privilegeGroup : privilegeGroups) {
			infoData.add(privilegeGroup.getName() + ": " + (privilegeGroup.getApplicationPrivilegeGroupType() != null ? privilegeGroup.getApplicationPrivilegeGroupType().name() : null));
			for (ApplicationPrivilege privilege : privilegeGroup.getPrivileges()) {
				infoData.add(" -> " + privilege.getName());
			}
		}
		return infoData;
	}
}
