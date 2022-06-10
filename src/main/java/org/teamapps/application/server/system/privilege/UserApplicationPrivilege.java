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


import org.teamapps.application.api.privilege.*;
import org.teamapps.model.controlcenter.OrganizationUnitView;

import java.util.*;

public class UserApplicationPrivilege implements ApplicationPrivilegeProvider {

	private final UserPrivileges userPrivileges;
	private final PrivilegeApplicationKey privilegeApplicationKey;
	private Set<SimplePrivilege> simplePrivileges;
	private Map<SimpleOrganizationalPrivilege, Set<OrganizationUnitView>> simpleOrganizationalPrivilegeSetMap;
	private Map<SimpleCustomObjectPrivilege, Set<PrivilegeObject>> simpleCustomObjectPrivilegeSetMap;
	private Map<StandardPrivilegeGroup, Set<Privilege>> standardPrivilegeGroupSetMap;
	private Map<OrganizationalPrivilegeGroup, Map<Privilege, Set<OrganizationUnitView>>> organizationalPrivilegeGroupMap;
	private Map<CustomObjectPrivilegeGroup, Map<Privilege, Set<PrivilegeObject>>> customObjectPrivilegeGroupMap;
	private Map<RoleAssignmentDelegatedCustomPrivilegeGroup, Map<Privilege, Set<PrivilegeObject>>> delegatedCustomPrivilegeGroupMap;

	public UserApplicationPrivilege(UserPrivileges userPrivileges, PrivilegeApplicationKey privilegeApplicationKey) {
		this.userPrivileges = userPrivileges;
		this.privilegeApplicationKey = privilegeApplicationKey;
		init();
	}

	private void init() {
		simplePrivileges = userPrivileges.getSimplePrivilegesMap().get(privilegeApplicationKey);
		simpleOrganizationalPrivilegeSetMap = userPrivileges.getSimpleOrganizationPrivilegeMap().get(privilegeApplicationKey);
		simpleCustomObjectPrivilegeSetMap = userPrivileges.getSimpleCustomObjectPrivilegeMap().get(privilegeApplicationKey);
		standardPrivilegeGroupSetMap = userPrivileges.getStandardPrivilegeMap().get(privilegeApplicationKey);
		organizationalPrivilegeGroupMap = userPrivileges.getOrganizationPrivilegeGroupMap().get(privilegeApplicationKey);
		customObjectPrivilegeGroupMap = userPrivileges.getCustomObjectPrivilegeGroupMap().get(privilegeApplicationKey);
		delegatedCustomPrivilegeGroupMap = userPrivileges.getRoleAssignmentDelegatedCustomPrivilegeMap().get(privilegeApplicationKey);
	}

	@Override
	public boolean isAllowed(SimplePrivilege simplePrivilege) {
		if (simplePrivileges == null) {
			return false;
		} else {
			return simplePrivileges.contains(simplePrivilege);
		}
	}

	@Override
	public boolean isAllowed(SimpleOrganizationalPrivilege simpleOrganizationalPrivilege, OrganizationUnitView organizationUnitView) {
		if (simpleOrganizationalPrivilegeSetMap == null || !simpleOrganizationalPrivilegeSetMap.containsKey(simpleOrganizationalPrivilege)) {
			return false;
		} else {
			return simpleOrganizationalPrivilegeSetMap.get(simpleOrganizationalPrivilege).contains(organizationUnitView);
		}
	}

	@Override
	public boolean isAllowed(SimpleCustomObjectPrivilege simpleCustomObjectPrivilege, PrivilegeObject privilegeObject) {
		if (simpleCustomObjectPrivilegeSetMap == null || !simpleCustomObjectPrivilegeSetMap.containsKey(simpleCustomObjectPrivilege)) {
			return false;
		} else {
			return simpleCustomObjectPrivilegeSetMap.get(simpleCustomObjectPrivilege).contains(privilegeObject);
		}
	}

	@Override
	public boolean isAllowed(StandardPrivilegeGroup standardPrivilegeGroup, Privilege privilege) {
		if (standardPrivilegeGroupSetMap == null || !standardPrivilegeGroupSetMap.containsKey(standardPrivilegeGroup)) {
			return false;
		} else {
			return standardPrivilegeGroupSetMap.get(standardPrivilegeGroup).contains(privilege);
		}
	}

	@Override
	public boolean isAllowed(OrganizationalPrivilegeGroup organizationalPrivilegeGroup, Privilege privilege, OrganizationUnitView organizationUnitView) {
		if (organizationalPrivilegeGroupMap == null || !organizationalPrivilegeGroupMap.containsKey(organizationalPrivilegeGroup)) {
			return false;
		} else {
			Set<OrganizationUnitView> organizationUnitViews = organizationalPrivilegeGroupMap.get(organizationalPrivilegeGroup).get(privilege);
			if (organizationUnitViews != null && organizationUnitViews.contains(organizationUnitView)) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean isAllowed(CustomObjectPrivilegeGroup customObjectPrivilegeGroup, Privilege privilege, PrivilegeObject privilegeObject) {
		if (customObjectPrivilegeGroupMap == null || !customObjectPrivilegeGroupMap.containsKey(customObjectPrivilegeGroup)) {
			return false;
		} else {
			Set<PrivilegeObject> privilegeObjects = customObjectPrivilegeGroupMap.get(customObjectPrivilegeGroup).get(privilege);
			if (privilegeObjects != null && privilegeObjects.contains(privilegeObject)) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean isAllowed(RoleAssignmentDelegatedCustomPrivilegeGroup group, Privilege privilege, PrivilegeObject privilegeObject) {
		if (delegatedCustomPrivilegeGroupMap == null || !delegatedCustomPrivilegeGroupMap.containsKey(group)) {
			return false;
		} else {
			Set<PrivilegeObject> privilegeObjects = delegatedCustomPrivilegeGroupMap.get(group).get(privilege);
			if (privilegeObjects != null && privilegeObjects.contains(privilegeObject)) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public List<OrganizationUnitView> getAllowedUnits(SimpleOrganizationalPrivilege simpleOrganizationalPrivilege) {
		if (simpleOrganizationalPrivilegeSetMap == null || !simpleOrganizationalPrivilegeSetMap.containsKey(simpleOrganizationalPrivilege)) {
			return Collections.emptyList();
		} else {
			return new ArrayList<>(simpleOrganizationalPrivilegeSetMap.get(simpleOrganizationalPrivilege));
		}
	}

	@Override
	public List<OrganizationUnitView> getAllowedUnits(OrganizationalPrivilegeGroup organizationalPrivilegeGroup, Privilege privilege) {
		if (organizationalPrivilegeGroupMap == null || !organizationalPrivilegeGroupMap.containsKey(organizationalPrivilegeGroup)) {
			return Collections.emptyList();
		} else {
			Set<OrganizationUnitView> organizationUnitViews = organizationalPrivilegeGroupMap.get(organizationalPrivilegeGroup).get(privilege);
			return organizationUnitViews != null ? new ArrayList<>(organizationUnitViews) : Collections.emptyList();
		}
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(SimpleCustomObjectPrivilege simpleCustomObjectPrivilege) {
		if (simpleCustomObjectPrivilegeSetMap == null || !simpleCustomObjectPrivilegeSetMap.containsKey(simpleCustomObjectPrivilege)) {
			return Collections.emptyList();
		} else {
			return new ArrayList<>(simpleCustomObjectPrivilegeSetMap.get(simpleCustomObjectPrivilege));
		}
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(CustomObjectPrivilegeGroup customObjectPrivilegeGroup, Privilege privilege) {
		if (customObjectPrivilegeGroupMap == null || !customObjectPrivilegeGroupMap.containsKey(customObjectPrivilegeGroup)) {
			return Collections.emptyList();
		} else {
			Set<PrivilegeObject> privilegeObjects = customObjectPrivilegeGroupMap.get(customObjectPrivilegeGroup).get(privilege);
			return privilegeObjects != null ? new ArrayList<>(privilegeObjects) : Collections.emptyList();
		}
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(RoleAssignmentDelegatedCustomPrivilegeGroup group, Privilege privilege) {
		if (delegatedCustomPrivilegeGroupMap == null || !delegatedCustomPrivilegeGroupMap.containsKey(group)) {
			return Collections.emptyList();
		} else {
			Set<PrivilegeObject> privilegeObjects = delegatedCustomPrivilegeGroupMap.get(group).get(privilege);
			return privilegeObjects != null ? new ArrayList<>(privilegeObjects) : Collections.emptyList();
		}
	}
}
