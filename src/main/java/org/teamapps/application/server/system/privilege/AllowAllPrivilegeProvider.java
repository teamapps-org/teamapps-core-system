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
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.model.controlcenter.OrganizationUnit;
import org.teamapps.model.controlcenter.OrganizationUnitView;

import java.util.Collections;
import java.util.List;

public class AllowAllPrivilegeProvider implements ApplicationPrivilegeProvider {
	@Override
	public boolean isAllowed(SimplePrivilege simplePrivilege) {
		return true;
	}

	@Override
	public boolean isAllowed(SimpleOrganizationalPrivilege simpleOrganizationalPrivilege, OrganizationUnitView organizationUnitView) {
		return true;
	}

	@Override
	public boolean isAllowed(SimpleCustomObjectPrivilege simpleCustomObjectPrivilege, PrivilegeObject privilegeObject) {
		return true;
	}

	@Override
	public boolean isAllowed(StandardPrivilegeGroup standardPrivilegeGroup, Privilege privilege) {
		return true;
	}

	@Override
	public boolean isAllowed(OrganizationalPrivilegeGroup organizationalPrivilegeGroup, Privilege privilege, OrganizationUnitView organizationUnitView) {
		return true;
	}

	@Override
	public boolean isAllowed(CustomObjectPrivilegeGroup customObjectPrivilegeGroup, Privilege privilege, PrivilegeObject privilegeObject) {
		return true;
	}

	@Override
	public boolean isAllowed(RoleAssignmentDelegatedCustomPrivilegeGroup group, Privilege privilege, PrivilegeObject privilegeObject) {
		return true;
	}


	@Override
	public List<OrganizationUnitView> getAllowedUnits(SimpleOrganizationalPrivilege simpleOrganizationalPrivilege) {
		return OrganizationUtils.convertList(OrganizationUnit.getAll());
	}

	@Override
	public List<OrganizationUnitView> getAllowedUnits(OrganizationalPrivilegeGroup organizationalPrivilegeGroup, Privilege privilege) {
		return OrganizationUtils.convertList(OrganizationUnit.getAll());
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(SimpleCustomObjectPrivilege simpleCustomObjectPrivilege) {
		return simpleCustomObjectPrivilege.getPrivilegeObjectsSupplier().get();
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(CustomObjectPrivilegeGroup customObjectPrivilegeGroup, Privilege privilege) {
		return customObjectPrivilegeGroup.getPrivilegeObjectsSupplier().get();
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(RoleAssignmentDelegatedCustomPrivilegeGroup group, Privilege privilege) {
		return Collections.emptyList();
	}
}
