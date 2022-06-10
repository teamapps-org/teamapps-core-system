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
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.ux.component.template.BaseTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class OrgChartNode {

	private final OrganizationUnit organizationUnit;
	private UserRoleAssignment userRoleAssignment;
	private String userFullName;
	private String roleName;
	private String unitNameWithPrefix;
	private String unitTypeName;
	private String searchMatchingString;
	private String userImage;
	private Icon orgUnitIcon;
	private RoleType roleType;

	private List<OrgChartNode> subNodes = new ArrayList<>();


	public OrgChartNode(OrganizationUnit organizationUnit, List<UserRoleAssignment> roleAssignments, boolean allRoles, ApplicationInstanceData applicationInstanceData) {
		this.organizationUnit = organizationUnit;
		init(roleAssignments, allRoles, applicationInstanceData);
	}

	protected OrgChartNode(OrganizationUnit organizationUnit, UserRoleAssignment assignment, String fullName, String image, String role, RoleType type, String search) {
		this.organizationUnit = organizationUnit;
		this.userRoleAssignment = assignment;
		this.userFullName = fullName;
		this.userImage = image;
		this.roleName = role;
		this.roleType = type != null ? type : RoleType.OTHER;
		this.searchMatchingString = search;
	}

	private void init(List<UserRoleAssignment> roleAssignments, boolean allRoles, ApplicationInstanceData applicationInstanceData) {
		PropertyProvider<OrganizationUnit> organizationUnitPropertyProvider = PropertyProviders.creatOrganizationUnitPropertyProvider(applicationInstanceData);
		PropertyProvider<Role> rolePropertyProvider = PropertyProviders.createRolePropertyProvider(applicationInstanceData);
		PropertyProvider<User> userPropertyProvider = PropertyProviders.createUserPropertyProvider(applicationInstanceData);

		List<UserRoleAssignment> sortedAssignments = roleAssignments == null ? Collections.emptyList() : roleAssignments.stream()
				.sorted(this::compareRoleTypes)
				.collect(Collectors.toList());

		Map<String, Object> orgUnitProperties = organizationUnitPropertyProvider.getValues(organizationUnit, null);
		unitNameWithPrefix = (String) orgUnitProperties.get(BaseTemplate.PROPERTY_CAPTION);
		unitTypeName = (String) orgUnitProperties.get(BaseTemplate.PROPERTY_DESCRIPTION);
		orgUnitIcon = (Icon) orgUnitProperties.get(BaseTemplate.PROPERTY_ICON);

		UserRoleAssignment assignment = sortedAssignments.isEmpty() ? null : sortedAssignments.get(0);
		int otherAssignmentIndex = 1;
		if (assignment != null && assignment.getRole().getRoleType() == RoleType.LEADER) {
			Map<String, Object> userProperties = userPropertyProvider.getValues(assignment.getUser(), null);
			userFullName = (String) userProperties.get(BaseTemplate.PROPERTY_CAPTION);
			userImage = (String) userProperties.get(BaseTemplate.PROPERTY_IMAGE);

			Map<String, Object> roleProperties = rolePropertyProvider.getValues(assignment.getRole(), null);
			roleName = (String) roleProperties.get(BaseTemplate.PROPERTY_CAPTION);

			roleType = assignment.getRole().getRoleType();
			userRoleAssignment = assignment;
			searchMatchingString = (unitNameWithPrefix + " " + unitTypeName + " " + userFullName + " " + roleName).toLowerCase();
		} else {
			otherAssignmentIndex = 0;
			searchMatchingString = (unitNameWithPrefix + " " + unitTypeName).toLowerCase();
		}

		if (!allRoles) {
			return;
		}
		for (int i = otherAssignmentIndex; i < sortedAssignments.size(); i++) {
			assignment = sortedAssignments.get(i);
			Map<String, Object> userProperties = userPropertyProvider.getValues(assignment.getUser(), null);
			String fullName = (String) userProperties.get(BaseTemplate.PROPERTY_CAPTION);
			String image = (String) userProperties.get(BaseTemplate.PROPERTY_IMAGE);
			Map<String, Object> roleProperties = rolePropertyProvider.getValues(assignment.getRole(), null);
			String role = (String) roleProperties.get(BaseTemplate.PROPERTY_CAPTION);
			RoleType type = assignment.getRole().getRoleType();
			String search = (unitNameWithPrefix + " " + unitTypeName + " " + fullName + " " + role).toLowerCase();
			subNodes.add(new OrgChartNode(organizationUnit, assignment, fullName, image, role, type, search));
		}
	}


	public List<OrgChartNode> getNodesByType(RoleType... types) {
		HashSet<RoleType> roleTypes = new HashSet<>(Arrays.asList(types));

		List<OrgChartNode> orgChartNodes = subNodes.stream().filter(node -> roleTypes.contains(node.getRoleType())).collect(Collectors.toList());
		if (roleTypes.contains(RoleType.LEADER)) {
			List<OrgChartNode> leaderNodes = new ArrayList<>();
			leaderNodes.add(this);
			leaderNodes.addAll(orgChartNodes);
			return leaderNodes;
		} else {
			return orgChartNodes;
		}
	}

	public boolean matches(String query) {
		if (query == null || query.isBlank()) {
			return true;
		} else {
			return searchMatchingString.contains(query.toLowerCase());
		}
	}

	public OrganizationUnit getOrganizationUnit() {
		return organizationUnit;
	}

	public UserRoleAssignment getUserRoleAssignment() {
		return userRoleAssignment;
	}

	public String getUserFullName() {
		return userFullName;
	}

	public String getRoleName() {
		return roleName;
	}

	public String getUnitNameWithPrefix() {
		return unitNameWithPrefix;
	}

	public String getUnitTypeName() {
		return unitTypeName;
	}

	public String getSearchMatchingString() {
		return searchMatchingString;
	}

	public String getUserImage() {
		return userImage;
	}

	public Icon getOrgUnitIcon() {
		return orgUnitIcon;
	}

	public RoleType getRoleType() {
		return roleType;
	}

	public List<OrgChartNode> getSubNodes() {
		return subNodes;
	}

	private int compareRoleTypes(UserRoleAssignment assignment1, UserRoleAssignment assignment2) {
		RoleType type1 = assignment1.getRole().getRoleType();
		RoleType type2 = assignment2.getRole().getRoleType();
		int ordinal1 = type1 != null ? type1.ordinal() : RoleType.OTHER.ordinal();
		int ordinal2 = type2 != null ? type2.ordinal() : RoleType.OTHER.ordinal();
		if (type1 == RoleType.LEADER && assignment1.isMainResponsible()) {
			ordinal1 = -1;
		}
		if (type2 == RoleType.LEADER && assignment2.isMainResponsible()) {
			ordinal2 = -1;
		}
		if (ordinal1 == ordinal2) {
			if (assignment1.getUser().getProfilePictureLength() == 0) {
				ordinal1++;
			}
		}
		return Integer.compare(ordinal1, ordinal2);
	}
}
