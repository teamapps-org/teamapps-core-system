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
package org.teamapps.application.server.system.privilege;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.privilege.*;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.utils.RoleUtils;
import org.teamapps.application.server.system.utils.ValueConverterUtils;
import org.teamapps.model.controlcenter.*;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

public class UserPrivileges {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final User user;
	private final SystemRegistry systemRegistry;

	private final Map<PrivilegeApplicationKey, Set<SimplePrivilege>> simplePrivilegesMap = new HashMap<>();
	private final Map<PrivilegeApplicationKey, Map<SimpleOrganizationalPrivilege, Set<OrganizationUnitView>>> simpleOrganizationPrivilegeMap = new HashMap<>();
	private final Map<PrivilegeApplicationKey, Map<SimpleCustomObjectPrivilege, Set<PrivilegeObject>>> simpleCustomObjectPrivilegeMap = new HashMap<>();
	private final Map<PrivilegeApplicationKey, Map<StandardPrivilegeGroup, Set<Privilege>>> standardPrivilegeMap = new HashMap<>();
	private final Map<PrivilegeApplicationKey, Map<OrganizationalPrivilegeGroup, Map<Privilege, Set<OrganizationUnitView>>>> organizationPrivilegeGroupMap = new HashMap<>();
	private final Map<PrivilegeApplicationKey, Map<CustomObjectPrivilegeGroup, Map<Privilege, Set<PrivilegeObject>>>> customObjectPrivilegeGroupMap = new HashMap<>();
	private final Map<PrivilegeApplicationKey, Map<RoleAssignmentDelegatedCustomPrivilegeGroup, Map<Privilege, Set<PrivilegeObject>>>> roleAssignmentDelegatedCustomPrivilegeMap = new HashMap<>();
	private final Map<PrivilegeApplicationKey, UserApplicationPrivilege> userApplicationPrivilegeByApplication = new HashMap<>();

	private boolean skippedMultiFactorRequiringPrivileges;

	public UserPrivileges(User user, SystemRegistry systemRegistry, Role authenticatedUserRole) {
		this(user, systemRegistry, authenticatedUserRole, false);
	}

	public UserPrivileges(User user, SystemRegistry systemRegistry, Role authenticatedUserRole, boolean multiFactorAuthenticationProvided) {
		this.user = user;
		this.systemRegistry = systemRegistry;
		calculatePrivileges(authenticatedUserRole, multiFactorAuthenticationProvided);
	}

	public Set<PrivilegeApplicationKey> getKeys() {
		Set<PrivilegeApplicationKey> keySet = new HashSet<>();
		keySet.addAll(simplePrivilegesMap.keySet());
		keySet.addAll(simpleOrganizationPrivilegeMap.keySet());
		keySet.addAll(simpleCustomObjectPrivilegeMap.keySet());
		keySet.addAll(standardPrivilegeMap.keySet());
		keySet.addAll(organizationPrivilegeGroupMap.keySet());
		keySet.addAll(customObjectPrivilegeGroupMap.keySet());
		keySet.addAll(roleAssignmentDelegatedCustomPrivilegeMap.keySet());
		return keySet;
	}

	public List<Application> getApplications() {
		return getKeys()
				.stream()
				.map(PrivilegeApplicationKey::getApplication)
				.collect(Collectors.toList());
	}

	public Map<Application, List<PrivilegeApplicationKey>> getApplicationKeyMap() {
		return getKeys()
				.stream()
				.collect(Collectors.groupingBy(PrivilegeApplicationKey::getApplication));
	}

	public List<PrivilegeGroup> getPrivilegeGroups(PrivilegeApplicationKey applicationKey) {
		List<PrivilegeGroup> groups = new ArrayList<>();
		if (simplePrivilegesMap.containsKey(applicationKey)) groups.addAll(simplePrivilegesMap.get(applicationKey));
		if (simpleOrganizationPrivilegeMap.containsKey(applicationKey))
			groups.addAll(simpleOrganizationPrivilegeMap.get(applicationKey).keySet());
		if (simpleCustomObjectPrivilegeMap.containsKey(applicationKey))
			groups.addAll(simpleCustomObjectPrivilegeMap.get(applicationKey).keySet());
		if (standardPrivilegeMap.containsKey(applicationKey))
			groups.addAll(standardPrivilegeMap.get(applicationKey).keySet());
		if (organizationPrivilegeGroupMap.containsKey(applicationKey))
			groups.addAll(organizationPrivilegeGroupMap.get(applicationKey).keySet());
		if (customObjectPrivilegeGroupMap.containsKey(applicationKey))
			groups.addAll(customObjectPrivilegeGroupMap.get(applicationKey).keySet());
		if (roleAssignmentDelegatedCustomPrivilegeMap.containsKey(applicationKey))
			groups.addAll(roleAssignmentDelegatedCustomPrivilegeMap.get(applicationKey).keySet());
		return groups;
	}

	private void calculatePrivileges(Role authenticatedUserRole, boolean multiFactorAuthenticationProvided) {
		List<UserRoleAssignment> roleAssignments = new ArrayList<>(user.getRoleAssignments());
		if (authenticatedUserRole != null) {
			roleAssignments.add(UserRoleAssignment.create()
					.setRole(authenticatedUserRole)
					.setUser(user)
					.setOrganizationUnit(user.getOrganizationUnit())
			);
		}

		for (UserRoleAssignment roleAssignment : roleAssignments) {
			Role role = roleAssignment.getRole();
			int delegatedCustomPrivilegeObjectId = roleAssignment.getDelegatedCustomPrivilegeObjectId();
			OrganizationUnit organizationUnit = roleAssignment.getOrganizationUnit();
			Set<Role> privilegeRoles = RoleUtils.getAllPrivilegeRoles(role);
			for (Role privilegeRole : privilegeRoles) {
				for (RoleApplicationRoleAssignment roleApplicationRoleAssignment : privilegeRole.getApplicationRoleAssignments()) {
					if (roleApplicationRoleAssignment.isInheritOrgFieldFromRole()) {
						OrganizationField organizationField = roleAssignment.getRole().getOrganizationField();
						calculatePrivilegesFromApplicationRoleAssignment(organizationUnit, roleApplicationRoleAssignment.getApplication(), organizationField, roleApplicationRoleAssignment, delegatedCustomPrivilegeObjectId, multiFactorAuthenticationProvided, privilegeRole.equals(role));
					} else {
						calculatePrivilegesFromApplicationRoleAssignment(organizationUnit, roleApplicationRoleAssignment.getApplication(), roleApplicationRoleAssignment.getOrganizationFieldFilter(), roleApplicationRoleAssignment, delegatedCustomPrivilegeObjectId, multiFactorAuthenticationProvided, privilegeRole.equals(role));
						for (OrganizationField additionalOrgField : roleApplicationRoleAssignment.getAdditionalOrgFields()) {
							calculatePrivilegesFromApplicationRoleAssignment(organizationUnit, roleApplicationRoleAssignment.getApplication(), additionalOrgField, roleApplicationRoleAssignment, delegatedCustomPrivilegeObjectId, multiFactorAuthenticationProvided, privilegeRole.equals(role));
						}
					}
				}
				for (RolePrivilegeAssignment privilegeAssignment : privilegeRole.getPrivilegeAssignments()) {
					calculatePrivilegesFromRolePrivilegeAssignment(organizationUnit, privilegeAssignment, delegatedCustomPrivilegeObjectId, multiFactorAuthenticationProvided, privilegeRole.equals(role));
				}
			}
		}
	}

	private void calculatePrivilegesFromApplicationRoleAssignment(OrganizationUnit organizationUnit, Application application, OrganizationField organizationField, RoleApplicationRoleAssignment roleApplicationRoleAssignment, int delegatedCustomPrivilegeObjectId, boolean multiFactorAuthenticationProvided, boolean isDirectRoleOwner) {
		try {
			String applicationRoleName = roleApplicationRoleAssignment.getApplicationRoleName();
			PrivilegeApplicationKey privilegeApplicationKey = PrivilegeApplicationKey.create(application, organizationField);
			OrganizationUnit fixedOrganizationRoot = roleApplicationRoleAssignment.getFixedOrganizationRoot();
			List<OrganizationUnitType> organizationUnitTypeFilter = roleApplicationRoleAssignment.getOrganizationUnitTypeFilter();
			boolean noInheritanceOfOrganizationalUnits = roleApplicationRoleAssignment.isNoInheritanceOfOrganizationalUnits();
			LoadedApplication loadedApplication = systemRegistry.getLoadedApplication(application);
			if (loadedApplication != null) {
				ApplicationRole applicationRole = loadedApplication.getAppPrivilegeProvider().getApplicationRole(applicationRoleName);
				if (applicationRole != null && applicationRole.getPrivilegeGroups() != null) {
					Set<OrganizationUnit> allUnits = OrganizationUtils.getAllUnits(fixedOrganizationRoot != null ? fixedOrganizationRoot : organizationUnit, organizationUnitTypeFilter, noInheritanceOfOrganizationalUnits);
					List<OrganizationUnitView> organizationUnitViews = OrganizationUtils.convertList(allUnits);
					List<PrivilegeGroup> privilegeGroups = applicationRole.getPrivilegeGroups();
					for (PrivilegeGroup privilegeGroup : privilegeGroups) {
						if (privilegeGroup.isInheritanceForbidden() && !isDirectRoleOwner) {
							continue;
						}
						if (user.getBlockedPrivilegesCount() > 0 && user.getBlockedPrivileges().stream().anyMatch(apg -> apg.getApplication() != null && apg.getApplication().getName().equals(application.getName()) && privilegeGroup.getName().equals(apg.getName()))) {
							continue;
						}
						if (privilegeGroup.isMultiFactorAuthenticationRequired() && !multiFactorAuthenticationProvided) {
							skippedMultiFactorRequiringPrivileges = true;
							continue;
						}
						switch (privilegeGroup.getType()) {
							case SIMPLE_PRIVILEGE:
								SimplePrivilege simplePrivilege = (SimplePrivilege) privilegeGroup;
								simplePrivilegesMap
										.computeIfAbsent(privilegeApplicationKey, app -> new HashSet<>())
										.add(simplePrivilege);
								break;
							case SIMPLE_ORGANIZATIONAL_PRIVILEGE:
								SimpleOrganizationalPrivilege simpleOrganizationalPrivilege = (SimpleOrganizationalPrivilege) privilegeGroup;
								simpleOrganizationPrivilegeMap
										.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
										.computeIfAbsent(simpleOrganizationalPrivilege, s -> new HashSet<>())
										.addAll(organizationUnitViews);
								break;
							case SIMPLE_CUSTOM_OBJECT_PRIVILEGE:
								SimpleCustomObjectPrivilege simpleCustomObjectPrivilege = (SimpleCustomObjectPrivilege) privilegeGroup;
								List<PrivilegeObject> privilegeObjects = simpleCustomObjectPrivilege.getPrivilegeObjectsSupplier().get();
								simpleCustomObjectPrivilegeMap
										.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
										.computeIfAbsent(simpleCustomObjectPrivilege, s -> new HashSet<>())
										.addAll(privilegeObjects);
								break;
							case STANDARD_PRIVILEGE_GROUP:
								StandardPrivilegeGroup standardPrivilegeGroup = (StandardPrivilegeGroup) privilegeGroup;
								List<Privilege> privileges = privilegeGroup.getPrivileges();
								standardPrivilegeMap
										.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
										.computeIfAbsent(standardPrivilegeGroup, s -> new HashSet<>())
										.addAll(privileges);
								break;
							case ORGANIZATIONAL_PRIVILEGE_GROUP:
								OrganizationalPrivilegeGroup organizationalPrivilegeGroup = (OrganizationalPrivilegeGroup) privilegeGroup;
								List<Privilege> groupPrivileges = privilegeGroup.getPrivileges();
								Map<Privilege, Set<OrganizationUnitView>> organizationUnitViewsByPrivilege = organizationPrivilegeGroupMap
										.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
										.computeIfAbsent(organizationalPrivilegeGroup, s -> new HashMap<>());
								for (Privilege privilege : groupPrivileges) {
									organizationUnitViewsByPrivilege
											.computeIfAbsent(privilege, p -> new HashSet<>())
											.addAll(organizationUnitViews);
								}
								break;
							case CUSTOM_OBJECT_PRIVILEGE_GROUP:
								CustomObjectPrivilegeGroup customObjectPrivilegeGroup = (CustomObjectPrivilegeGroup) privilegeGroup;
								List<PrivilegeObject> customPrivileges = customObjectPrivilegeGroup.getPrivilegeObjectsSupplier().get();
								List<Privilege> customObjectPrivileges = customObjectPrivilegeGroup.getPrivileges();
								Map<Privilege, Set<PrivilegeObject>> customObjectsByPrivilege = customObjectPrivilegeGroupMap
										.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
										.computeIfAbsent(customObjectPrivilegeGroup, c -> new HashMap<>());
								for (Privilege privilege : customObjectPrivileges) {
									customObjectsByPrivilege
											.computeIfAbsent(privilege, p -> new HashSet<>())
											.addAll(customPrivileges);
								}
								break;
							case ROLE_ASSIGNMENT_DELEGATED_CUSTOM_PRIVILEGE_GROUP:
								if (delegatedCustomPrivilegeObjectId > 0) {
									RoleAssignmentDelegatedCustomPrivilegeGroup delegatedCustomPrivilegeGroup = (RoleAssignmentDelegatedCustomPrivilegeGroup) privilegeGroup;
									List<Privilege> delegatedCustomPrivileges = delegatedCustomPrivilegeGroup.getPrivileges();
									Map<Privilege, Set<PrivilegeObject>> privilegeObjectByPrivilege = roleAssignmentDelegatedCustomPrivilegeMap
											.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
											.computeIfAbsent(delegatedCustomPrivilegeGroup, s -> new HashMap<>());
									PrivilegeObject privilegeObject = delegatedCustomPrivilegeGroup.getPrivilegeObjectById(delegatedCustomPrivilegeObjectId);
									if (privilegeObject != null) {
										for (Privilege privilege : delegatedCustomPrivileges) {
											privilegeObjectByPrivilege
													.computeIfAbsent(privilege, p -> new HashSet<>())
													.add(privilegeObject);
										}
									}
								}
								break;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void calculatePrivilegesFromRolePrivilegeAssignment(OrganizationUnit organizationUnit, RolePrivilegeAssignment privilegeAssignment, int delegatedCustomPrivilegeObjectId, boolean multiFactorAuthenticationProvided, boolean isDirectRoleOwner) {
		Application application = privilegeAssignment.getApplication();
		LoadedApplication loadedApplication = systemRegistry.getLoadedApplication(application);
		if (loadedApplication != null) {
			ApplicationScopePrivilegeProvider privilegeProvider = loadedApplication.getAppPrivilegeProvider();
			PrivilegeApplicationKey privilegeApplicationKey = PrivilegeApplicationKey.create(privilegeAssignment);
			OrganizationUnit fixedOrganizationRoot = privilegeAssignment.getFixedOrganizationRoot();
			List<OrganizationUnitType> organizationUnitTypeFilter = privilegeAssignment.getOrganizationUnitTypeFilter();
			boolean noInheritanceOfOrganizationalUnits = privilegeAssignment.isNoInheritanceOfOrganizationalUnits();
			PrivilegeGroup privilegeGroup = privilegeProvider.getPrivilegeGroup(privilegeAssignment.getPrivilegeGroup().getName());
			List<Privilege> privileges = privilegeProvider.getPrivilegesByNameList(privilegeAssignment.getPrivileges().stream().map(ApplicationPrivilege::getName).collect(Collectors.toList()));
			boolean privilegeObjectInheritance = privilegeAssignment.getPrivilegeObjectInheritance();
			List<Integer> privilegeObjectIdList = ValueConverterUtils.decompressIds(privilegeAssignment.getPrivilegeObjects());
			List<PrivilegeObject> privilegeObjects = privilegeProvider.getPrivilegeObjects(privilegeGroup, privilegeObjectIdList, privilegeObjectInheritance);
			Set<OrganizationUnit> allUnits = OrganizationUtils.getAllUnits(fixedOrganizationRoot != null ? fixedOrganizationRoot : organizationUnit, organizationUnitTypeFilter, noInheritanceOfOrganizationalUnits);
			List<OrganizationUnitView> organizationUnitViews = OrganizationUtils.convertList(allUnits);
			if (privilegeGroup == null) {
				LOGGER.error("ERROR: missing privilege group from assignment: {}", privilegeAssignment.getPrivilegeGroup().getName());
				return;
			}
			if (privilegeGroup.isInheritanceForbidden() && !isDirectRoleOwner) {
				return;
			}
			if (user.getBlockedPrivilegesCount() > 0 && user.getBlockedPrivileges().stream().anyMatch(apg -> apg.getApplication() != null && apg.getApplication().getName().equals(application.getName()) && privilegeGroup.getName().equals(apg.getName()))) {
				return;
			}
			if (privilegeGroup.isMultiFactorAuthenticationRequired() && !multiFactorAuthenticationProvided) {
				skippedMultiFactorRequiringPrivileges = true;
				return;
			}
			try {
				switch (privilegeGroup.getType()) {
					case SIMPLE_PRIVILEGE:
						SimplePrivilege simplePrivilege = (SimplePrivilege) privilegeGroup;
						simplePrivilegesMap
								.computeIfAbsent(privilegeApplicationKey, app -> new HashSet<>())
								.add(simplePrivilege);
						break;
					case SIMPLE_ORGANIZATIONAL_PRIVILEGE:
						SimpleOrganizationalPrivilege simpleOrganizationalPrivilege = (SimpleOrganizationalPrivilege) privilegeGroup;
						simpleOrganizationPrivilegeMap
								.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
								.computeIfAbsent(simpleOrganizationalPrivilege, s -> new HashSet<>())
								.addAll(organizationUnitViews);
						break;
					case SIMPLE_CUSTOM_OBJECT_PRIVILEGE:
						SimpleCustomObjectPrivilege simpleCustomObjectPrivilege = (SimpleCustomObjectPrivilege) privilegeGroup;
						simpleCustomObjectPrivilegeMap
								.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
								.computeIfAbsent(simpleCustomObjectPrivilege, s -> new HashSet<>())
								.addAll(privilegeObjects);
						break;
					case STANDARD_PRIVILEGE_GROUP:
						StandardPrivilegeGroup standardPrivilegeGroup = (StandardPrivilegeGroup) privilegeGroup;
						standardPrivilegeMap
								.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
								.computeIfAbsent(standardPrivilegeGroup, s -> new HashSet<>())
								.addAll(privileges);
						break;
					case ORGANIZATIONAL_PRIVILEGE_GROUP:
						OrganizationalPrivilegeGroup organizationalPrivilegeGroup = (OrganizationalPrivilegeGroup) privilegeGroup;
						Map<Privilege, Set<OrganizationUnitView>> organizationUnitViewsByPrivilege = organizationPrivilegeGroupMap
								.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
								.computeIfAbsent(organizationalPrivilegeGroup, s -> new HashMap<>());
						for (Privilege privilege : privileges) {
							organizationUnitViewsByPrivilege
									.computeIfAbsent(privilege, p -> new HashSet<>())
									.addAll(organizationUnitViews);
						}
						break;
					case CUSTOM_OBJECT_PRIVILEGE_GROUP:
						CustomObjectPrivilegeGroup customObjectPrivilegeGroup = (CustomObjectPrivilegeGroup) privilegeGroup;
						Map<Privilege, Set<PrivilegeObject>> customObjectsByPrivilege = customObjectPrivilegeGroupMap
								.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
								.computeIfAbsent(customObjectPrivilegeGroup, c -> new HashMap<>());
						for (Privilege privilege : privileges) {
							customObjectsByPrivilege
									.computeIfAbsent(privilege, p -> new HashSet<>())
									.addAll(privilegeObjects);
						}
						break;
					case ROLE_ASSIGNMENT_DELEGATED_CUSTOM_PRIVILEGE_GROUP:
						if (delegatedCustomPrivilegeObjectId > 0) {
							RoleAssignmentDelegatedCustomPrivilegeGroup delegatedCustomPrivilegeGroup = (RoleAssignmentDelegatedCustomPrivilegeGroup) privilegeGroup;
							Map<Privilege, Set<PrivilegeObject>> privilegeObjectByPrivilege = roleAssignmentDelegatedCustomPrivilegeMap
									.computeIfAbsent(privilegeApplicationKey, app -> new HashMap<>())
									.computeIfAbsent(delegatedCustomPrivilegeGroup, s -> new HashMap<>());
							PrivilegeObject privilegeObject = delegatedCustomPrivilegeGroup.getPrivilegeObjectById(delegatedCustomPrivilegeObjectId);
							if (privilegeObject != null) {
								for (Privilege privilege : privileges) {
									privilegeObjectByPrivilege
											.computeIfAbsent(privilege, p -> new HashSet<>())
											.add(privilegeObject);
								}
							}
						}
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public ApplicationPrivilegeProvider getApplicationPrivilegeProvider(PrivilegeApplicationKey privilegeApplicationKey) {
		UserApplicationPrivilege userApplicationPrivilege = userApplicationPrivilegeByApplication.get(privilegeApplicationKey);
		if (userApplicationPrivilege == null) {
			if (user.getUserAccountStatus() == UserAccountStatus.SUPER_ADMIN) {
				return new AllowAllPrivilegeProvider();
			}
			userApplicationPrivilege = new UserApplicationPrivilege(this, privilegeApplicationKey);
			userApplicationPrivilegeByApplication.put(privilegeApplicationKey, userApplicationPrivilege);
		}
		return userApplicationPrivilege;
	}

	public Map<PrivilegeApplicationKey, Set<SimplePrivilege>> getSimplePrivilegesMap() {
		return simplePrivilegesMap;
	}

	public Map<PrivilegeApplicationKey, Map<SimpleOrganizationalPrivilege, Set<OrganizationUnitView>>> getSimpleOrganizationPrivilegeMap() {
		return simpleOrganizationPrivilegeMap;
	}

	public Map<PrivilegeApplicationKey, Map<SimpleCustomObjectPrivilege, Set<PrivilegeObject>>> getSimpleCustomObjectPrivilegeMap() {
		return simpleCustomObjectPrivilegeMap;
	}

	public Map<PrivilegeApplicationKey, Map<StandardPrivilegeGroup, Set<Privilege>>> getStandardPrivilegeMap() {
		return standardPrivilegeMap;
	}

	public Map<PrivilegeApplicationKey, Map<OrganizationalPrivilegeGroup, Map<Privilege, Set<OrganizationUnitView>>>> getOrganizationPrivilegeGroupMap() {
		return organizationPrivilegeGroupMap;
	}

	public Map<PrivilegeApplicationKey, Map<CustomObjectPrivilegeGroup, Map<Privilege, Set<PrivilegeObject>>>> getCustomObjectPrivilegeGroupMap() {
		return customObjectPrivilegeGroupMap;
	}

	public Map<PrivilegeApplicationKey, Map<RoleAssignmentDelegatedCustomPrivilegeGroup, Map<Privilege, Set<PrivilegeObject>>>> getRoleAssignmentDelegatedCustomPrivilegeMap() {
		return roleAssignmentDelegatedCustomPrivilegeMap;
	}

	public User getUser() {
		return user;
	}

	public boolean hasSkippedMultiFactorRequiringPrivileges() {
		return skippedMultiFactorRequiringPrivileges;
	}
}
