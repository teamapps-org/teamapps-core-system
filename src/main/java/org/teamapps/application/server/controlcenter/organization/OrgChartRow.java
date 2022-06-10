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

import org.teamapps.model.controlcenter.OrganizationUnit;
import org.teamapps.model.controlcenter.RoleType;
import org.teamapps.model.controlcenter.UserRoleAssignment;

import java.util.ArrayList;
import java.util.List;

public class OrgChartRow {

	private final OrgChartNode orgUnitNode;
	private final OrgChartNode leaderNode;
	private final OrgChartNode assistantNode;
	private final OrgChartNode otherNode;

	public static List<OrgChartRow> createRows(OrgChartNode orgChartNode) {
		List<OrgChartRow> rows = new ArrayList<>();
		List<OrgChartNode> leaderNodes = orgChartNode.getNodesByType(RoleType.LEADER);
		List<OrgChartNode> assistantNodes = orgChartNode.getNodesByType(RoleType.MENTOR, RoleType.ASSISTANT, RoleType.ADMINISTRATOR);
		List<OrgChartNode> otherNodes = orgChartNode.getNodesByType(RoleType.OTHER);
		int rowsCount = Math.max(1, Math.max(leaderNodes.size(), Math.max(assistantNodes.size(),  otherNodes.size())));
		for (int i = 0; i < rowsCount; i++) {
			OrgChartNode orgUnitNode = i == 0 ? orgChartNode : null;
			OrgChartNode leaderNode = leaderNodes.size() > i ? leaderNodes.get(i) : null;
			OrgChartNode assistantNode = assistantNodes.size() > i ? assistantNodes.get(i) : null;
			OrgChartNode otherNode = otherNodes.size() > i ? otherNodes.get(i) : null;
			rows.add(new OrgChartRow(orgUnitNode, leaderNode, assistantNode, otherNode));
		}
		return rows;
	}

	public OrgChartRow(OrgChartNode orgUnitNode, OrgChartNode leaderNode, OrgChartNode assistantNode, OrgChartNode otherNode) {
		this.orgUnitNode = orgUnitNode;
		this.leaderNode = leaderNode;
		this.assistantNode = assistantNode;
		this.otherNode = otherNode;
	}

	public boolean match(String query) {
		if (orgUnitNode != null && orgUnitNode.matches(query)) return true;
		if (leaderNode != null && leaderNode.matches(query)) return true;
		if (assistantNode != null && assistantNode.matches(query)) return true;
		if (otherNode != null && otherNode.matches(query)) return true;
		return false;
	}

	public OrgChartNode getOrgUnitNode() {
		return orgUnitNode;
	}

	public OrgChartNode getLeaderNode() {
		return leaderNode;
	}

	public OrgChartNode getAssistantNode() {
		return assistantNode;
	}

	public OrgChartNode getOtherNode() {
		return otherNode;
	}
}
