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
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.privilege.Privilege;
import org.teamapps.application.api.privilege.PrivilegeType;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.Templates;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.common.format.Color;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.databinding.MutableValue;
import org.teamapps.model.controlcenter.OrganizationField;
import org.teamapps.model.controlcenter.OrganizationUnit;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.charting.common.GraphNodeIcon;
import org.teamapps.ux.component.charting.common.GraphNodeImage;
import org.teamapps.ux.component.charting.tree.BaseTreeGraphNode;
import org.teamapps.ux.component.charting.tree.TreeGraph;
import org.teamapps.ux.component.charting.tree.TreeGraphNode;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.TextField;
import org.teamapps.ux.component.table.ListTable;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.template.Template;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OrganizationChartPerspective extends AbstractManagedApplicationPerspective {

	public final static Privilege SHOW_UPWARDS_LEADERS = Privilege.create(PrivilegeType.READ, "upwardLeaders", ApplicationIcons.NAVIGATE_OPEN, "organizationChart.showUpwardsLeaders");
	public final static Privilege SHOW_UPWARDS_ALL_ROLES = Privilege.create(PrivilegeType.READ, "upwardAllRoles", ApplicationIcons.NAVIGATE_OPEN, "organizationChart.showUpwardsAllRoles");
	public final static Privilege SHOW_DOWNWARDS_LEADERS = Privilege.create(PrivilegeType.READ, "downwardLeaders", ApplicationIcons.NAVIGATE_CLOSE, "organizationChart.showDownwardsLeaders");
	public final static Privilege SHOW_DOWNWARDS_ALL_ROLES = Privilege.create(PrivilegeType.READ, "downwardsAllRoles", ApplicationIcons.NAVIGATE_CLOSE, "organizationChart.showDownwardsAllRoles");


	private final PerspectiveSessionData perspectiveSessionData;
	private final UserSessionData userSessionData;
	private TreeGraph<OrgChartNode> treeGraph;
	private float zoomFactor = 1;
	private List<TreeGraphNode<OrgChartNode>> treeNodes;

	public OrganizationChartPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		if (OrganizationField.getCount() == 0) {
			return;
		}
		View masterView = getPerspective().addView(View.createView(StandardLayout.CENTER, ApplicationIcons.PIECES, getLocalized("organizationChart.title"), null));
		masterView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.75f));
		View masterTableView = getPerspective().addView(View.createView(StandardLayout.CENTER, ApplicationIcons.PIECES, getLocalized("organizationChart.title"), null));
		masterTableView.setVisible(false);
		ToolbarButtonGroup buttonGroup;


		buttonGroup = masterTableView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.ELEMENTS_HIERARCHY, getLocalized("organizationChart.showOrgChart"), getLocalized("organizationChart.showOrgChart"))).onClick.addListener(() -> {
			masterTableView.setVisible(false);
			masterView.setVisible(true);
		});


		buttonGroup = masterView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.SPREADSHEET, getLocalized("organizationChart.showOrganizationTable"), getLocalized("organizationChart.displayOrgChartAsTable"))).onClick.addListener(() -> {
			masterTableView.setVisible(true);
			masterView.setVisible(false);
		});

		buttonGroup = masterView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton standardViewButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.ELEMENTS_HIERARCHY, getLocalized("organizationChart.standardView"), getLocalized("organizationChart.standardView.desc")));
		ToolbarButton compactViewButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.ELEMENTS_TREE, getLocalized("organizationChart.compactView"), getLocalized("organizationChart.compactView.desc")));

		buttonGroup = masterView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton zoomInButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.ZOOM_IN, getLocalized(Dictionary.ZOOM_IN), getLocalized(Dictionary.ZOOM_IN)));
		ToolbarButton zoomOutButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.ZOOM_OUT, getLocalized(Dictionary.ZOOM_OUT), getLocalized(Dictionary.ZOOM_OUT)));

		standardViewButton.onClick.addListener(() -> {
			treeGraph.setCompact(false);
			//treeGraph.setNodes(treeNodes);
		});
		compactViewButton.onClick.addListener(() -> {
			treeGraph.setCompact(true);
			//treeGraph.setNodes(Collections.emptyList());
		});
		zoomInButton.onClick.addListener(() -> {
			zoomFactor = zoomFactor * 1.5f;
			treeGraph.setZoomFactor(zoomFactor);
			treeGraph.setNodes(treeNodes);
		});
		zoomOutButton.onClick.addListener(() -> {
			zoomFactor = zoomFactor / 1.5f;
			treeGraph.setZoomFactor(zoomFactor);
			treeGraph.setNodes(treeNodes);
		});


		OrganizationField organizationField = getOrganizationField() != null ? getOrganizationField() : OrganizationField.getAll().get(0);
		OrgChartData orgChartData = new OrgChartData(organizationField, getApplicationInstanceData());

		List<OrgChartRow> orgChartRows = orgChartData.getOrgChartRows();

		ListTable<OrgChartRow> table = new ListTable<>();
		table.setDisplayAsList(true);
		table.setStripedRows(true);
		table.setRowHeight(36);
		table.setRecords(orgChartRows);

		PropertyProvider<OrgChartNode> orgUnitPropertyProvider = UiUtils.createPropertyProvider(OrgChartNode::getOrgUnitIcon, null, OrgChartNode::getUnitNameWithPrefix, OrgChartNode::getUnitTypeName);
		PropertyProvider<OrgChartNode> roleAssignmentPropertyProvider = UiUtils.createPropertyProvider(null, OrgChartNode::getUserImage, OrgChartNode::getUserFullName, OrgChartNode::getRoleName);

		TemplateField<OrgChartNode> orgUnitField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_TWO_LINES, orgUnitPropertyProvider);
		TemplateField<OrgChartNode> leaderField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, roleAssignmentPropertyProvider);
		TemplateField<OrgChartNode> assistantField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, roleAssignmentPropertyProvider);
		TemplateField<OrgChartNode> otherField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, roleAssignmentPropertyProvider);

		table.addColumn("unit", null, getLocalized("organization.organizationUnit"), orgUnitField, 220).setValueExtractor(OrgChartRow::getOrgUnitNode);
		table.addColumn("leader", null, getLocalized("organizationChart.leadershipTask"), leaderField, 230).setValueExtractor(OrgChartRow::getLeaderNode);
		table.addColumn("assistant", null, getLocalized("organizationChart.assistantMentorTask"), assistantField, 230).setValueExtractor(OrgChartRow::getAssistantNode);
		table.addColumn("other", null, getLocalized("organizationChart.otherTasks"), otherField, 250).setValueExtractor(OrgChartRow::getOtherNode);

		masterTableView.setComponent(table);
		TextField searchField = new TextField();
		searchField.setShowClearButton(true);
		searchField.setEmptyText(getLocalized(Dictionary.SEARCH___));
		searchField.onTextInput.addListener(query -> {
			List<OrgChartRow> filteredRows = orgChartData.getOrgChartRows().stream().filter(row -> row.match(query)).collect(Collectors.toList());
			table.setRecords(filteredRows);
		});
		masterTableView.getPanel().setRightHeaderField(searchField);


		treeGraph = new TreeGraph<>();
		treeNodes = new ArrayList<>();
		Map<OrganizationUnit, TreeGraphNode<OrgChartNode>> nodeByOrgUnit = new HashMap<>();

		for (OrgChartNode chartNode : orgChartData.getOrgChartNodes()) {
			TreeGraphNode<OrgChartNode> node = createNode(chartNode);
			treeNodes.add(node);
			nodeByOrgUnit.put(chartNode.getOrganizationUnit(), node);
		}
		treeNodes.forEach(node -> {
			node.setParent(nodeByOrgUnit.get(node.getRecord().getOrganizationUnit().getParent()));
			node.setParentExpandable(node.getParent() != null);
		});

		treeGraph.setPropertyProvider(createOrgChartNodePropertyProvider());
		treeGraph.setHorizontalSiblingGap(40);
		treeGraph.setHorizontalNonSignlingGap(60);
		treeGraph.setVerticalLayerGap(75);
		treeGraph.setNodes(treeNodes);


		masterView.setComponent(treeGraph);

		Consumer<OrgChartNode> ensureOpenHandler = orgChartNode -> {

			for (TreeGraphNode<OrgChartNode> treeNode : treeNodes) {
				if (treeNode.getRecord().getOrganizationUnit().equals(orgChartNode.getOrganizationUnit())) {
					treeNode.setParentExpanded(false);
					treeNode.setExpanded(true);
					treeGraph.updateNode(treeNode);
				} else {
					if (!treeNode.isParentExpanded()) {
						treeNode.setParentExpanded(true);
						treeGraph.updateNode(treeNode);
					}
					if (treeNode.isExpanded()) {
						treeNode.setExpanded(false);
						treeGraph.updateNode(treeNode);

					}
				}
			}

			OrgChartNode node = orgChartNode;
			TreeGraphNode<OrgChartNode> graphNode = nodeByOrgUnit.get(node.getOrganizationUnit());
			while (graphNode != null) {
				graphNode = nodeByOrgUnit.get(graphNode.getRecord().getOrganizationUnit().getParent());
				if (graphNode != null) {
					graphNode.setExpanded(true);
					treeGraph.updateNode(graphNode);
				}
			}
		};

		TextField orgChartSearchField = new TextField();
		orgChartSearchField.setShowClearButton(true);
		orgChartSearchField.setEmptyText(getLocalized(Dictionary.SEARCH___));
		orgChartSearchField.onTextInput.addListener(query -> {
			treeNodes.stream()
					.filter(node -> node.getRecord().matches(query))
					.peek(node -> ensureOpenHandler.accept(node.getRecord()))
					.findFirst()
					.ifPresent(graphNode -> treeGraph.moveToNode(graphNode));
		});
		masterView.getPanel().setRightHeaderField(orgChartSearchField);

	}


	private TreeGraphNode<OrgChartNode> createNode(OrgChartNode chartNode) {
		TreeGraphNode<OrgChartNode> node = new TreeGraphNode<>();
		node.setRecord(chartNode);
		node.setTemplate(Templates.ORGANIZATION_GRAPH_TEMPLATE);
		node.setWidth(240);
		node.setHeight(68);
		node.setBorderRadius(10);
		node.setBorderWidth(1f);
		node.setConnectorLineColor(Color.MATERIAL_BLUE_700);
		node.setBorderColor(Color.MATERIAL_BLUE_700);
		node.setBackgroundColor(Color.MATERIAL_BLUE_100);
		node.setExpanded(false);

		if (chartNode.getUserImage() != null) {
			node.setImage(new GraphNodeImage(chartNode.getUserImage(), 70, 70).setBorderColor(Color.MATERIAL_BLUE_700).setBorderWidth(1f).setCenterLeftDistance(0).setCenterTopDistance(34).setCornerShape(GraphNodeImage.CornerShape.CIRCLE));
		} else {
			node.setIcon(new GraphNodeIcon(chartNode.getOrgUnitIcon(), 54));
		}
		List<BaseTreeGraphNode<OrgChartNode>> sideNodes = chartNode.getSubNodes().stream().map(this::createSideNode).collect(Collectors.toList());
		;
		node.setSideListNodes(sideNodes);

		return node;
	}

	private BaseTreeGraphNode<OrgChartNode> createSideNode(OrgChartNode chartNode) {
		BaseTreeGraphNode<OrgChartNode> node = new BaseTreeGraphNode<>();
		node.setRecord(chartNode);
		node.setWidth(200);
		node.setHeight(40);
		node.setBorderRadius(7);
		node.setBorderWidth(1f);
		Color borderColor = Color.MATERIAL_GREY_700;
		Color backgroundColor = Color.MATERIAL_GREY_100;
		Template template = Templates.ORGANIZATION_GRAPH_SMALL_GREY_TEMPLATE;
		switch (chartNode.getRoleType()) {
			case LEADER -> {
				borderColor = Color.MATERIAL_BLUE_700;
				backgroundColor = Color.MATERIAL_BLUE_100;
				template = Templates.ORGANIZATION_GRAPH_SMALL_BlUE_TEMPLATE;
			}
			case ASSISTANT, MENTOR, ADMINISTRATOR -> {
				borderColor = Color.MATERIAL_GREEN_900;
				backgroundColor = Color.MATERIAL_GREEN_100;
				template = Templates.ORGANIZATION_GRAPH_SMALL_GREEN_TEMPLATE;
			}
			case OTHER -> {
				borderColor = Color.MATERIAL_GREY_700;
				backgroundColor = Color.MATERIAL_GREY_100;
				template = Templates.ORGANIZATION_GRAPH_SMALL_GREY_TEMPLATE;
			}
		}

		node.setTemplate(template);
		node.setBorderColor(borderColor);
		node.setBackgroundColor(backgroundColor);
		node.setConnectorLineColor(borderColor);

		if (chartNode.getUserImage() != null) {
			node.setImage(new GraphNodeImage(chartNode.getUserImage(), 55, 55).setBorderColor(borderColor).setBorderWidth(1f).setCenterLeftDistance(0).setCenterTopDistance(20).setCornerShape(GraphNodeImage.CornerShape.CIRCLE));
		} else {
			node.setIcon(new GraphNodeIcon(chartNode.getOrgUnitIcon(), 32));
		}
		return node;
	}


	private PropertyProvider<OrgChartNode> createOrgChartNodePropertyProvider() {
		return (orgChartNode, collection) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(Templates.PROPERTY_CAPTION, orgChartNode.getUnitNameWithPrefix());
			map.put(Templates.PROPERTY_DESCRIPTION, orgChartNode.getUserFullName());
			map.put(Templates.PROPERTY_LINE3, orgChartNode.getRoleName());
			return map;
		};
	}


}

