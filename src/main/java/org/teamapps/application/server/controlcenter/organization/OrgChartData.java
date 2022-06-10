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
package org.teamapps.application.server.controlcenter.organization;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.model.controlcenter.OrganizationField;
import org.teamapps.model.controlcenter.OrganizationUnit;
import org.teamapps.model.controlcenter.OrganizationUnitView;
import org.teamapps.model.controlcenter.UserRoleAssignment;

import java.util.*;
import java.util.stream.Collectors;

public class OrgChartData {

	private Set<OrganizationUnit> orgChartUnits = new HashSet<>();
	private Set<OrganizationUnit> orgChartUnitsWithAllRoles = new HashSet<>();
	private List<OrganizationUnit> rootUnits;

	private List<OrgChartNode> orgChartNodes = new ArrayList<>();
	private List<OrgChartRow> orgChartRows = new ArrayList<>();

	public OrgChartData(OrganizationField organizationField, ApplicationInstanceData applicationInstanceData) {
		List<OrganizationUnitView> upwardLeaderUnits = applicationInstanceData.getAllowedUnits(Privileges.ORGANIZATION_CHART_PERSPECTIVE, OrganizationChartPerspective.SHOW_UPWARDS_LEADERS);
		List<OrganizationUnitView> upwardAllRoleUnits = applicationInstanceData.getAllowedUnits(Privileges.ORGANIZATION_CHART_PERSPECTIVE, OrganizationChartPerspective.SHOW_UPWARDS_ALL_ROLES);
		List<OrganizationUnitView> downwardLeaderUnits = applicationInstanceData.getAllowedUnits(Privileges.ORGANIZATION_CHART_PERSPECTIVE, OrganizationChartPerspective.SHOW_DOWNWARDS_LEADERS);
		List<OrganizationUnitView> downwardAllRoleUnits = applicationInstanceData.getAllowedUnits(Privileges.ORGANIZATION_CHART_PERSPECTIVE, OrganizationChartPerspective.SHOW_DOWNWARDS_ALL_ROLES);

		Set<OrganizationUnit> upwardUnits = OrganizationUtils.convertViewSet(upwardLeaderUnits, upwardAllRoleUnits);
		Set<OrganizationUnit> upwardAllRolesUnits = OrganizationUtils.convertViewSet(upwardAllRoleUnits);
		List<OrganizationUnit> upwardRootNodes = OrganizationUtils.getRootNodes(upwardUnits);

		for (OrganizationUnit unit : upwardRootNodes) {
			List<OrganizationUnit> upwardPath = OrganizationUtils.getPath(unit);
			orgChartUnits.addAll(upwardPath);
			if (upwardAllRolesUnits.contains(unit)) {
				orgChartUnitsWithAllRoles.addAll(upwardPath);
			}
		}

		orgChartUnits.addAll(OrganizationUtils.convertViewList(downwardLeaderUnits));
		orgChartUnits.addAll(OrganizationUtils.convertViewList(downwardAllRoleUnits));
		orgChartUnitsWithAllRoles.addAll(OrganizationUtils.convertViewList(downwardAllRoleUnits));

		rootUnits = OrganizationUtils.getRootNodes(orgChartUnits);

		Map<OrganizationUnit, List<UserRoleAssignment>> roleAssignmentsByOrganizationUnit = UserRoleAssignment.getAll().stream()
				.filter(userRoleAssignment -> organizationField.equals(userRoleAssignment.getRole().getOrganizationField()))
				.filter(userRoleAssignment -> orgChartUnits.contains(userRoleAssignment.getOrganizationUnit()))
				.filter(userRoleAssignment -> userRoleAssignment.getUser() != null)
				.collect(Collectors.groupingBy(UserRoleAssignment::getOrganizationUnit));

		for (OrganizationUnit rootUnit : rootUnits) {
			traverseOrgTree(rootUnit, roleAssignmentsByOrganizationUnit, applicationInstanceData);
		}
	}

	private void traverseOrgTree(OrganizationUnit unit, Map<OrganizationUnit, List<UserRoleAssignment>> roleAssignmentsByOrganizationUnit, ApplicationInstanceData applicationInstanceData) {
		List<UserRoleAssignment> userRoleAssignments = roleAssignmentsByOrganizationUnit.get(unit);
		OrgChartNode orgChartNode = new OrgChartNode(unit, userRoleAssignments, orgChartUnitsWithAllRoles.contains(unit), applicationInstanceData);
		orgChartNodes.add(orgChartNode);
		List<OrgChartRow> rows = OrgChartRow.createRows(orgChartNode);
		orgChartRows.addAll(rows);
		for (OrganizationUnit childUnit : unit.getChildren()) {
			if (orgChartUnits.contains(childUnit)) {
				traverseOrgTree(childUnit, roleAssignmentsByOrganizationUnit, applicationInstanceData);
			}
		}
	}

	public List<OrgChartNode> getOrgChartNodes() {
		return orgChartNodes;
	}

	public List<OrgChartRow> getOrgChartRows() {
		return orgChartRows;
	}
}
