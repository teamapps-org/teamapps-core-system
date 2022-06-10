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
package org.teamapps.application.server.system.group;

import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.utils.RoleUtils;
import org.teamapps.model.controlcenter.*;

import java.util.*;

public class GroupMembershipHandler {


	public synchronized void updateAllGroupMemberships() {
		for (Group group : Group.getAll()) {
			updateGroupUserMemberships(group);
		}
	}

	public synchronized void updateGroupUserMemberships(Group group) {
		List<UserGroupMembership> userMemberships = new ArrayList<>();
		Set<User> userMemberSet = new HashSet<>();
		User owner = group.getOwner();
		if (owner != null) {
			userMemberSet.add(owner);
			userMemberships.add(createUserMembership(owner, group, GroupMembershipRole.OWNER));
		}
		for (User moderator : group.getModerators()) {
			if (!userMemberSet.contains(moderator)) {
				userMemberSet.add(moderator);
				userMemberships.add(createUserMembership(moderator, group, GroupMembershipRole.MODERATOR));
			}
		}
		for (User mentor : group.getMentors()) {
			if (!userMemberSet.contains(mentor)) {
				userMemberSet.add(mentor);
				userMemberships.add(createUserMembership(mentor, group, GroupMembershipRole.MENTOR));
			}
		}

		group.getMembershipDefinitions().stream().filter(definition -> definition.getGroupMemberType() == GroupMemberType.USER).forEach(definition -> {
			if (!userMemberSet.contains(definition.getUser())) {
				userMemberSet.add(definition.getUser());
				userMemberships.add(createUserMembership(definition.getUser(), group, GroupMembershipRole.PARTICIPANT));
			}
		});

		group.getMembershipDefinitions().stream().filter(definition -> definition.getGroupMemberType() == GroupMemberType.ROLE_MEMBER).forEach(definition -> {
			List<UserRoleAssignment> userRoleAssignments = RoleUtils.getUserRoleAssignments(definition.getRole(), true, definition.getOrganizationUnit(), definition.getOrganizationUnitTypesFilter());
			for (UserRoleAssignment assignment : userRoleAssignments) {
				if (!userMemberSet.contains(assignment.getUser())) {
					userMemberSet.add(assignment.getUser());
					userMemberships.add(createUserMembership(assignment.getUser(), group, GroupMembershipRole.PARTICIPANT));
				}
			}
		});

		group.getMembershipDefinitions().stream().filter(definition -> definition.getGroupMemberType() == GroupMemberType.USER_CONTAINER).forEach(definition -> {
			Set<User> users = OrganizationUtils.getAllUsers(definition.getOrganizationUnit(), definition.getOrganizationUnitTypesFilter());
			for (User user : users) {
				if (!userMemberSet.contains(user)) {
					userMemberSet.add(user);
					userMemberships.add(createUserMembership(user, group, GroupMembershipRole.PARTICIPANT));
				}
			}
		});

		Set<Group> memberGroupSet = new HashSet<>();
		calculateContainedMembershipGroups(group, memberGroupSet);
		memberGroupSet.remove(group);
		for (Group memberGroup : memberGroupSet) {
			Set<User> users = calculateNestedGroupMembers(memberGroup);
			for (User user : users) {
				if (!userMemberSet.contains(user)) {
					userMemberSet.add(user);
					userMemberships.add(createUserMembership(user, group, GroupMembershipRole.PARTICIPANT));
				}
			}
		}

		List<UserGroupMembership> currentUserMemberships = group.getUserMemberships();
		Map<User, UserGroupMembership> currentMembershipMap = new HashMap<>();
		currentUserMemberships.forEach(membership -> currentMembershipMap.put(membership.getUser(), membership));
		Map<User, UserGroupMembership> userMembershipMap = new HashMap<>();
		userMemberships.forEach(membership -> userMembershipMap.put(membership.getUser(), membership));

		List<UserGroupMembership> removeMemberships = new ArrayList<>();
		for (UserGroupMembership membership : currentUserMemberships) {
			if (!userMembershipMap.containsKey(membership.getUser())) {
				removeMemberships.add(membership);
			}
		}
		List<UserGroupMembership> addMemberships = new ArrayList<>();
		for (UserGroupMembership membership : userMemberships) {
			if (!currentMembershipMap.containsKey(membership.getUser())) {
				addMemberships.add(membership);
			}
		}

		if (!removeMemberships.isEmpty() || !addMemberships.isEmpty()) {
			if (currentUserMemberships.size() / 20 > (removeMemberships.size() + addMemberships.size())) {
				group
						.removeUserMemberships(removeMemberships)
						.addUserMemberships(addMemberships)
						.save();
			} else {
				group
						.setUserMemberships(userMemberships)
						.save();
			}
		}
	}

	private Set<User> calculateNestedGroupMembers(Group group) {
		Set<User> userMemberSet = new HashSet<>();
		User owner = group.getOwner();
		if (owner != null) {
			userMemberSet.add(owner);
		}
		for (User moderator : group.getModerators()) {
			if (!userMemberSet.contains(moderator)) {
				userMemberSet.add(moderator);
			}
		}
		for (User mentor : group.getMentors()) {
			if (!userMemberSet.contains(mentor)) {
				userMemberSet.add(mentor);
			}
		}

		group.getMembershipDefinitions().stream().filter(definition -> definition.getGroupMemberType() == GroupMemberType.USER).forEach(definition -> {
			if (!userMemberSet.contains(definition.getUser())) {
				userMemberSet.add(definition.getUser());
			}
		});

		group.getMembershipDefinitions().stream().filter(definition -> definition.getGroupMemberType() == GroupMemberType.ROLE_MEMBER).forEach(definition -> {
			List<UserRoleAssignment> userRoleAssignments = RoleUtils.getUserRoleAssignments(definition.getRole(), true, definition.getOrganizationUnit(), definition.getOrganizationUnitTypesFilter());
			for (UserRoleAssignment assignment : userRoleAssignments) {
				if (!userMemberSet.contains(assignment.getUser())) {
					userMemberSet.add(assignment.getUser());
				}
			}
		});

		group.getMembershipDefinitions().stream().filter(definition -> definition.getGroupMemberType() == GroupMemberType.USER_CONTAINER).forEach(definition -> {
			Set<User> users = OrganizationUtils.getAllUsers(definition.getOrganizationUnit(), definition.getOrganizationUnitTypesFilter());
			for (User user : users) {
				if (!userMemberSet.contains(user)) {
					userMemberSet.add(user);
				}
			}
		});
		return userMemberSet;
	}

	private void calculateContainedMembershipGroups(Group group, Set<Group> memberGroupSet) {
		group.getMembershipDefinitions().stream()
				.filter(membershipDefinition -> membershipDefinition.getGroupMemberType() == GroupMemberType.GROUP)
				.map(GroupMembershipDefinition::getGroup)
				.filter(memberGroup -> !memberGroupSet.contains(memberGroup))
				.forEach(memberGroup -> {
					memberGroupSet.add(memberGroup);
					calculateContainedMembershipGroups(memberGroup, memberGroupSet);
				});
	}


	private UserGroupMembership createUserMembership(User user, Group group, GroupMembershipRole role) {
		return UserGroupMembership.create()
				.setUser(user)
				.setGroup(group)
				.setGroupMembershipRole(role);
	}


}
