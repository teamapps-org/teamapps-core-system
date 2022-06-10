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

import org.teamapps.application.api.application.AbstractApplicationView;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.privilege.*;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.tools.RecordListModelBuilder;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.application.ux.PropertyData;
import org.teamapps.application.ux.form.FormPanel;
import org.teamapps.application.ux.org.OrganizationTree;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.OrganizationFieldView;
import org.teamapps.model.controlcenter.OrganizationUnitView;
import org.teamapps.ux.component.field.Label;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.tree.Tree;
import org.teamapps.ux.component.tree.TreeNodeInfo;
import org.teamapps.ux.model.ListTreeModel;

import java.util.*;

public class UserPrivilegesView extends AbstractApplicationView {


	private final UserPrivileges userPrivileges;

	private Map<Privilege, Set<OrganizationUnitView>> privilegeOrgUnitsMap;
	private ResponsiveForm<?> responsiveForm;

	public UserPrivilegesView(UserPrivileges userPrivileges, ApplicationInstanceData applicationInstanceData) {
		super(applicationInstanceData);
		this.userPrivileges = userPrivileges;
		createUi();
	}

	private void createUi() {
		responsiveForm = new ResponsiveForm<>(100, 200, 0);
		ResponsiveFormLayout formLayout = responsiveForm.addResponsiveFormLayout(1000);

		Map<Application, List<PrivilegeApplicationKey>> applicationKeyMap = userPrivileges.getApplicationKeyMap();
		List<PrivilegeNode> treeNodes = new ArrayList<>();
		applicationKeyMap.entrySet()
				.stream()
				.sorted((o1, o2) -> getLocalized(o1.getKey().getTitleKey()).compareTo(o2.getKey().getTitleKey()))
				.forEach(entry -> {
					PrivilegeNode appNode = new PrivilegeNode(entry.getKey(), null);
					treeNodes.add(appNode);
					List<PrivilegeApplicationKey> privilegeKeys = entry.getValue();
					for (PrivilegeApplicationKey privilegeApplicationKey : privilegeKeys) {
						PrivilegeNode parent = appNode;
						if (privilegeKeys.size() > 1) {
							OrganizationFieldView organizationFieldView = privilegeApplicationKey.getOrganizationFieldView();
							parent = new PrivilegeNode(organizationFieldView, appNode);
							treeNodes.add(parent);
						}
						List<PrivilegeGroup> privilegeGroups = userPrivileges.getPrivilegeGroups(privilegeApplicationKey);
						for (PrivilegeGroup privilegeGroup : privilegeGroups) {
							treeNodes.add(new PrivilegeNode(privilegeGroup, privilegeApplicationKey, parent));
						}
					}
				});
		ListTreeModel<PrivilegeNode> listTreeModel = new ListTreeModel<>(treeNodes);
		listTreeModel.setTreeNodeInfoFunction(node -> node);
		Tree<PrivilegeNode> tree = new Tree<>(listTreeModel);
		tree.setPropertyProvider((node, propertyNames) -> PropertyData.create(node.getIcon(), node.getTitle(), node.getTitle()));
		tree.setEntryTemplate(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES);
		FormPanel mainFormPanel = new FormPanel(getApplicationInstanceData(), tree);
		mainFormPanel.setHeight(500);

		RecordListModelBuilder<Privilege> privilegeModelBuilder = new RecordListModelBuilder<>(getApplicationInstanceData(), privilege -> getLocalized(privilege.getTitleKey()));
		Table<Privilege> privilegeTable = privilegeModelBuilder.createListTable(true);
		FormPanel privilegeFormPanel = new FormPanel(getApplicationInstanceData());
		privilegeFormPanel.setTable(privilegeTable, true, false, false);


		OrganizationTree organizationTree = new OrganizationTree(getApplicationInstanceData());
		FormPanel organizationPanel = new FormPanel(getApplicationInstanceData(), organizationTree.getTree());
		organizationPanel.setHeight(400);

		formLayout.addSection().setCollapsible(false).setDrawHeaderLine(false);
		formLayout.addLabelAndComponent(null, null, mainFormPanel.getPanel());
		formLayout.addLabelAndComponent(null, null, privilegeFormPanel.getPanel());
		formLayout.addLabelAndComponent(null, null, organizationPanel.getPanel());

		privilegeFormPanel.getPanel().setVisible(false);
		organizationPanel.getPanel().setVisible(false);


		privilegeTable.onSingleRowSelected.addListener(privilege -> {
			if (privilegeOrgUnitsMap != null) {
				Set<OrganizationUnitView> organizationUnitViews = privilegeOrgUnitsMap.get(privilege);
				organizationTree.setOrganizationUnits(organizationUnitViews);
			}
		});

		tree.onNodeSelected.addListener(node -> {
			PrivilegeGroup privilegeGroup = node.getPrivilegeGroup();
			privilegeOrgUnitsMap = null;
			if (privilegeGroup == null) {
				privilegeFormPanel.getPanel().setVisible(false);
				organizationPanel.getPanel().setVisible(false);
			} else {
				PrivilegeApplicationKey applicationKey = node.getPrivilegeApplicationKey();
				if (privilegeGroup instanceof SimplePrivilege) {
					SimplePrivilege privilege = (SimplePrivilege) privilegeGroup;
					privilegeFormPanel.getPanel().setVisible(false);
					organizationPanel.getPanel().setVisible(false);
				} else if (privilegeGroup instanceof SimpleOrganizationalPrivilege) {
					SimpleOrganizationalPrivilege privilege = (SimpleOrganizationalPrivilege) privilegeGroup;
					Set<OrganizationUnitView> organizationUnitViews = userPrivileges.getSimpleOrganizationPrivilegeMap().get(applicationKey).get(privilege);
					organizationTree.setOrganizationUnits(organizationUnitViews);
					privilegeFormPanel.getPanel().setVisible(false);
					organizationPanel.getPanel().setVisible(true);
				} else if (privilegeGroup instanceof SimpleCustomObjectPrivilege) {
					SimpleCustomObjectPrivilege privilege = (SimpleCustomObjectPrivilege) privilegeGroup;
					privilegeFormPanel.getPanel().setVisible(false);
					organizationPanel.getPanel().setVisible(false);
				} else if (privilegeGroup instanceof StandardPrivilegeGroup) {
					StandardPrivilegeGroup privilege = (StandardPrivilegeGroup) privilegeGroup;
					Set<Privilege> privileges = userPrivileges.getStandardPrivilegeMap().get(applicationKey).get(privilege);
					privilegeModelBuilder.setRecords(new ArrayList<>(privileges));
					privilegeFormPanel.getPanel().setVisible(true);
					organizationPanel.getPanel().setVisible(false);
				} else if (privilegeGroup instanceof OrganizationalPrivilegeGroup) {
					OrganizationalPrivilegeGroup privilege = (OrganizationalPrivilegeGroup) privilegeGroup;
					privilegeOrgUnitsMap = userPrivileges.getOrganizationPrivilegeGroupMap().get(applicationKey).get(privilege);
					Set<Privilege> privileges = privilegeOrgUnitsMap.keySet();
					privilegeModelBuilder.setRecords(new ArrayList<>(privileges));
					organizationTree.setOrganizationUnits(Collections.emptyList());
					privilegeFormPanel.getPanel().setVisible(true);
					organizationPanel.getPanel().setVisible(true);
				} else if (privilegeGroup instanceof CustomObjectPrivilegeGroup) {
					CustomObjectPrivilegeGroup privilege = (CustomObjectPrivilegeGroup) privilegeGroup;
					Map<Privilege, Set<PrivilegeObject>> privilegeSetMap = userPrivileges.getCustomObjectPrivilegeGroupMap().get(applicationKey).get(privilege);
					privilegeModelBuilder.setRecords(new ArrayList<>(privilegeSetMap.keySet()));
					privilegeFormPanel.getPanel().setVisible(true);
					organizationPanel.getPanel().setVisible(false);
				}
			}
		});

	}

	public UserPrivileges getUserPrivileges() {
		return userPrivileges;
	}

	public ResponsiveForm<?> getResponsiveForm() {
		return responsiveForm;
	}

	public class PrivilegeNode implements TreeNodeInfo {
		private Application application;
		private OrganizationFieldView organizationField;
		private PrivilegeGroup privilegeGroup;
		private final PrivilegeNode parent;
		private PrivilegeApplicationKey privilegeApplicationKey;

		public PrivilegeNode(Application application, PrivilegeNode parent) {
			this.application = application;
			this.parent = parent;
		}

		public PrivilegeNode(OrganizationFieldView organizationField, PrivilegeNode parent) {
			this.organizationField = organizationField;
			this.parent = parent;
		}

		public PrivilegeNode(PrivilegeGroup privilegeGroup, PrivilegeApplicationKey privilegeApplicationKey, PrivilegeNode parent) {
			this.privilegeGroup = privilegeGroup;
			this.privilegeApplicationKey = privilegeApplicationKey;
			this.parent = parent;
		}

		public PrivilegeApplicationKey getPrivilegeApplicationKey() {
			return privilegeApplicationKey;
		}

		public Icon getIcon() {
			if (application != null) {
				return IconUtils.decodeIcon(application.getIcon());
			} else if (organizationField != null) {
				return IconUtils.decodeIcon(organizationField.getIcon());
			} else if (privilegeGroup != null) {
				return privilegeGroup.getIcon();
			} else {
				return ApplicationIcons.WINDOWS;
			}
		}

		public String getTitle() {
			if (application != null) {
				return getLocalized(application.getTitleKey());
			} else if (organizationField != null) {
				return getLocalized(organizationField.getTitle());
			} else if (privilegeGroup != null) {
				return getLocalized(privilegeGroup.getTitleKey());
			} else {
				return getLocalized(Dictionary.APPLICATIONS);
			}
		}

		public Application getApplication() {
			return application;
		}

		public OrganizationFieldView getOrganizationField() {
			return organizationField;
		}

		public PrivilegeGroup getPrivilegeGroup() {
			return privilegeGroup;
		}

		@Override
		public Object getParent() {
			return parent;
		}

		@Override
		public boolean isExpanded() {
			return false;
		}

		@Override
		public boolean isLazyChildren() {
			return false;
		}
	}


}
