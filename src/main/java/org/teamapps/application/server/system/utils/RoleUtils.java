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
package org.teamapps.application.server.system.utils;

import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.application.api.privilege.ApplicationRole;
import org.teamapps.application.server.system.privilege.MergedApplicationPrivileges;
import org.teamapps.application.server.system.privilege.MergedPrivilegeGroup;
import org.teamapps.application.api.privilege.Privilege;
import org.teamapps.application.api.privilege.PrivilegeGroup;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.model.controlcenter.*;

import java.util.*;
import java.util.stream.Collectors;

public class RoleUtils {

	public static Set<Role> getAllRoleInstances(Role role) {
		Set<Role> roleSet = new HashSet<>();
		calculateRoleInstances(role, roleSet);
		return roleSet;
	}

	private static void calculateRoleInstances(Role role, Set<Role> roleSet) {
		if (!roleSet.contains(role)) {
			roleSet.add(role);
			for (Role specializationRole : role.getSpecializationRoles()) {
				calculateRoleInstances(specializationRole, roleSet);
			}
		}
	}

	public static Set<Role> getAllPrivilegeRoles(Role role) {
		Set<Role> roleSet = new HashSet<>();
		calculatePrivilegeRoles(role, roleSet);
		return roleSet;
	}

	private static void calculatePrivilegeRoles(Role role, Set<Role> roleSet) {
		if (!roleSet.contains(role)) {
			roleSet.add(role);
			for (Role generalizationRole : role.getGeneralizationRoles()) {
				calculatePrivilegeRoles(generalizationRole, roleSet);
			}
			for (Role privilegesSendingRole : role.getPrivilegesSendingRoles()) {
				calculatePrivilegeRoles(privilegesSendingRole, roleSet);
			}
		}
	}

	public static List<UserRoleAssignment> getUserRoleAssignments(Role role, boolean withSpecializationRoles, OrganizationUnit organizationUnit, List<OrganizationUnitType> organizationUnitTypesFilter) {
		if (role == null || organizationUnit == null) {
			return Collections.emptyList();
		}
		Set<Role> roleSet = withSpecializationRoles ? getAllRoleInstances(role) : new HashSet<>(Collections.singletonList(role));
		Set<OrganizationUnit> allUnits = OrganizationUtils.getAllUnits(organizationUnit, organizationUnitTypesFilter);
		return getUserRoleAssignments(roleSet, allUnits);
	}

	public static List<UserRoleAssignment> getUserRoleAssignments(Set<Role> roleSet, Set<OrganizationUnit> organizationUnits) {
		return UserRoleAssignment.getAll().stream()
				.filter(assignment -> roleSet.contains(assignment.getRole()))
				.filter(assignment -> organizationUnits.contains(assignment.getOrganizationUnit()))
				.collect(Collectors.toList());
	}

	public static int getMemberCount(Role role, boolean withSpecializationRoles) {
		Set<Role> roleSet = withSpecializationRoles ? getAllRoleInstances(role) : new HashSet<>(Collections.singletonList(role));
		return (int) UserRoleAssignment.getAll().stream()
				.filter(assignment -> roleSet.contains(assignment.getRole()))
				.count();
	}

	public static List<UserRoleAssignment> getMembers(Role role, boolean withSpecializationRoles) {
		Set<Role> roleSet = withSpecializationRoles ? getAllRoleInstances(role) : new HashSet<>(Collections.singletonList(role));
		return UserRoleAssignment.getAll().stream()
				.filter(assignment -> roleSet.contains(assignment.getRole()))
				.collect(Collectors.toList());
	}

	public static Comparator<UserRoleAssignment> createRoleTypeAndMainResponsibleComparator() {
		return (assignment1, assignment2) -> {
			RoleType type1 = assignment1.getRole().getRoleType();
			RoleType type2 = assignment2.getRole().getRoleType();
			int ordinal1 = type1 != null ? type1.ordinal() : RoleType.OTHER.ordinal();
			int ordinal2 = type2 != null ? type2.ordinal() : RoleType.OTHER.ordinal();
			if (type1 == type2) {
				if (assignment1.isMainResponsible()) {
					ordinal1 = -1;
				}
				if (assignment2.isMainResponsible()) {
					ordinal2 = -1;
				}
				if (ordinal1 == ordinal2) {
					if (assignment1.getUser().getProfilePictureLength() == 0) {
						ordinal1++;
					}
				}
			}
			return Integer.compare(ordinal1, ordinal2);
		};
	}

	public static List<MergedApplicationPrivileges> calcPrivileges(Role role, UserSessionData userSessionData) {
		SystemRegistry systemRegistry = userSessionData.getRegistry();
		Map<String, MergedApplicationPrivileges> applicationPrivilegesMap = new HashMap<>();
		Set<Role> privilegeRoles = getAllPrivilegeRoles(role);
		for (Role privilegeRole : privilegeRoles) {
			for (RoleApplicationRoleAssignment applicationRoleAssignment : privilegeRole.getApplicationRoleAssignments()) {
				Application application = applicationRoleAssignment.getApplication();
				ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(application);
				MergedApplicationPrivileges mergedApplicationPrivileges = applicationPrivilegesMap.computeIfAbsent(application.getName(), s -> new MergedApplicationPrivileges(s, IconUtils.decodeIcon(application.getIcon()), localizationProvider.getLocalized(application.getTitleKey()), localizationProvider.getLocalized(application.getDescriptionKey())));
				ApplicationRole applicationRole = getApplicationRole(application.getName(), applicationRoleAssignment.getApplicationRoleName(), systemRegistry);
				if (applicationRole != null) {
					for (PrivilegeGroup privilegeGroup : applicationRole.getPrivilegeGroups()) {
						mergedApplicationPrivileges.addPrivilegeGroup(privilegeGroup, localizationProvider);
					}
				}
			}

			for (RolePrivilegeAssignment privilegeAssignment : privilegeRole.getPrivilegeAssignments()) {
				Application application = privilegeAssignment.getApplication();
				ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(application);
				ApplicationPrivilegeGroup privilegeGroup = privilegeAssignment.getPrivilegeGroup();
				MergedApplicationPrivileges mergedApplicationPrivileges = applicationPrivilegesMap.computeIfAbsent(application.getName(), s -> new MergedApplicationPrivileges(s, IconUtils.decodeIcon(application.getIcon()), localizationProvider.getLocalized(application.getTitleKey()), localizationProvider.getLocalized(application.getDescriptionKey())));
				mergedApplicationPrivileges.addPrivilegeGroup(privilegeGroup, localizationProvider);
			}
		}
		return applicationPrivilegesMap.values().stream()
				.sorted(Comparator.comparing(MergedApplicationPrivileges::getTitle))
				.collect(Collectors.toList());
	}

	public static ApplicationRole getApplicationRole(String applicationName, String applicationRoleName, SystemRegistry systemRegistry) {
		return systemRegistry.getLoadedApplications().stream()
				.filter(app -> app.getApplication().getName().equals(applicationName))
				.filter(app -> app.getBaseApplicationBuilder() != null)
				.flatMap(app -> app.getBaseApplicationBuilder().getApplicationRoles().stream())
				.filter(role -> role.getName().equals(applicationRoleName))
				.findFirst().orElse(null);
	}

}
