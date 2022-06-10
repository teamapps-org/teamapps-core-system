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
package org.teamapps.application.server.system.privilege;

import org.teamapps.application.api.application.ApplicationBuilder;
import org.teamapps.application.api.application.BaseApplicationBuilder;
import org.teamapps.application.api.privilege.ApplicationRole;
import org.teamapps.application.api.privilege.Privilege;
import org.teamapps.application.api.privilege.PrivilegeGroup;
import org.teamapps.application.api.privilege.PrivilegeObject;

import java.util.*;
import java.util.stream.Collectors;

public class ApplicationScopePrivilegeProvider {

	private final BaseApplicationBuilder applicationBuilder;
	private List<PrivilegeGroup> privilegeGroups;
	private List<ApplicationRole> applicationRoles;
	private Map<String, ApplicationRole> applicationRoleByName;
	private Map<String, PrivilegeGroup> privilegeGroupByName;
	private Map<String, Privilege> privilegeByName;

	public ApplicationScopePrivilegeProvider(BaseApplicationBuilder applicationBuilder) {
		this.applicationBuilder = applicationBuilder;
		loadPrivileges();
	}


	private void loadPrivileges() {
		privilegeGroups = applicationBuilder.getPrivilegeGroups();
		if (privilegeGroups == null) {
			return;
		}
		privilegeGroupByName = new HashMap<>();
		privilegeByName = new HashMap<>();
		privilegeGroups.forEach(privilegeGroup -> {
			privilegeGroupByName.put(privilegeGroup.getName(), privilegeGroup);
			if (privilegeGroup.getPrivileges() != null) {
				privilegeGroup.getPrivileges().forEach(privilege -> privilegeByName.put(privilege.getName(), privilege));
			}
		});
		applicationRoles = applicationBuilder.getApplicationRoles() != null ? applicationBuilder.getApplicationRoles() : Collections.emptyList();
		applicationRoleByName = applicationRoles.stream().collect(Collectors.toMap(ApplicationRole::getName, role -> role));
	}

	public PrivilegeGroup getPrivilegeGroup(String name) {
		return privilegeGroupByName.get(name);
	}

	public Privilege getPrivilege(String name) {
		return privilegeByName.get(name);
	}

	public List<Privilege> getPrivilegesByNameList(List<String> privilegeNames) {
		if (privilegeNames == null || privilegeNames.isEmpty()) {
			return null;
		} else {
			List<Privilege> privileges = new ArrayList<>();
			for (String privilegeName : privilegeNames) {
				Privilege privilege = getPrivilege(privilegeName);
				if (privilege != null) {
					privileges.add(privilege);
				}
			}
			return privileges;
		}
	}

	public List<PrivilegeObject> getPrivilegeObjects(PrivilegeGroup privilegeGroup, List<Integer> privilegeObjectIds, boolean withChildren) {
		if (privilegeObjectIds == null || privilegeObjectIds.isEmpty() || privilegeGroup.getPrivilegeObjectsSupplier() == null) {
			return null;
		} else {
			List<PrivilegeObject> result = new ArrayList<>();
			List<PrivilegeObject> privilegeObjects = privilegeGroup.getPrivilegeObjectsSupplier().get();
			if (privilegeObjects != null) {
				Map<Integer, PrivilegeObject> privilegeObjectById = privilegeObjects.stream().collect(Collectors.toMap(PrivilegeObject::getId, privilegeObject -> privilegeObject));
				privilegeObjectIds.forEach(id -> {
					PrivilegeObject privilegeObject = privilegeObjectById.get(id);
					if (privilegeObject != null) {
						result.add(privilegeObject);
						if (withChildren) {
							Set<PrivilegeObject> resultSet = new HashSet<>();
							getAllChildren(privilegeObject, resultSet);
							result.addAll(resultSet);
						}
					}
				});
			}
			return result;
		}
	}

	private void getAllChildren(PrivilegeObject privilegeObject, Set<PrivilegeObject> resultSet) {
		if (!resultSet.contains(privilegeObject) && privilegeObject.getChildren() != null) {
			for (PrivilegeObject child : privilegeObject.getChildren()) {
				resultSet.add(child);
				getAllChildren(child, resultSet);
			}
		}
	}

	public List<ApplicationRole> getApplicationRoles() {
		return applicationRoles;
	}

	public ApplicationRole getApplicationRole(String name) {
		return applicationRoleByName.get(name);
	}
}
