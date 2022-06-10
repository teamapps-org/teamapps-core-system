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
package org.teamapps.application.server.controlcenter.database;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.privilege.Privilege;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.common.format.Color;
import org.teamapps.databinding.MutableValue;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.icons.Icon;
import org.teamapps.icons.composite.CompositeIcon;
import org.teamapps.universaldb.UniversalDB;
import org.teamapps.universaldb.index.DatabaseIndex;
import org.teamapps.universaldb.index.TableIndex;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.perspective.Perspective;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.itemview.SimpleItemGroup;
import org.teamapps.ux.component.itemview.SimpleItemView;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;
import org.teamapps.ux.component.tree.Tree;
import org.teamapps.ux.component.tree.TreeNodeInfoImpl;
import org.teamapps.ux.model.ListTreeModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabasePerspective extends AbstractManagedApplicationPerspective {

	private final UserSessionData userSessionData;
	private final UniversalDB universalDB;
	private final TwoWayBindableValue<TableIndex> selectedTable = TwoWayBindableValue.create();
	private boolean showDeletedRecords = false;
	private View timeLineView;
	private View tableView;
	private View formView;

	public DatabasePerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		PerspectiveSessionData perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		universalDB = userSessionData.getRegistry().getUniversalDB();
		createUi();
	}

	private void createUi() {
		timeLineView = getPerspective().addView(View.createView(StandardLayout.TOP, ApplicationIcons.CHART_LINE, getLocalized(Dictionary.TIMELINE), null));
		tableView = getPerspective().addView(View.createView(StandardLayout.CENTER, ApplicationIcons.TABLE, getLocalized(Dictionary.TABLE), null));
		formView = getPerspective().addView(View.createView(StandardLayout.RIGHT, ApplicationIcons.FORM, getLocalized(Dictionary.FORM), null));
		formView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.9f));



		List<DatabaseIndex> databases = universalDB.getSchemaIndex().getDatabases();
		List<DatabaseNode> databaseNodes = new ArrayList<>();
		List<TableIndex> applicationDbTables = new ArrayList<>();
		TableIndex firstIndex = null;
		String fixedDatabaseName = isAppFilter() ? getMainApplication().getName() : null;
		for (DatabaseIndex database : databases) {
			DatabaseNode dbNode = new DatabaseNode(database);
			if (fixedDatabaseName == null || fixedDatabaseName.equals(database.getName())) {
				databaseNodes.add(dbNode);
				for (TableIndex table : database.getTables()) {
					if (fixedDatabaseName != null) {
						applicationDbTables.add(table);
					}
					DatabaseNode tableNode = new DatabaseNode(table, dbNode);
					databaseNodes.add(tableNode);
					if (firstIndex == null) {
						firstIndex = table;
					}
				}
			}
		}

		ListTreeModel<DatabaseNode> treeModel = new ListTreeModel<>(databaseNodes);
		Tree<DatabaseNode> tree = new Tree<>(treeModel);
		treeModel.setTreeNodeInfoFunction(node -> new TreeNodeInfoImpl<>(node.getParent(), false, true, false));
		tree.setPropertyProvider((databaseNode, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, databaseNode.getIcon());
			map.put(BaseTemplate.PROPERTY_CAPTION, databaseNode.getTitle());
			map.put(BaseTemplate.PROPERTY_BADGE, databaseNode.getBadge());
			return map;
		});
		tree.setEntryTemplate(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE);
		tree.setOpenOnSelection(true);
		tree.setShowExpanders(false);
		tree.setEnforceSingleExpandedPath(true);
		tree.setTemplateDecider(databaseNode -> databaseNode.getTableIndex() != null ? BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE : BaseTemplate.LIST_ITEM_LARGE_ICON_SINGLE_LINE);
		this.setPerspectiveMenuPanel(tree, null);

		tree.onNodeSelected.addListener(node -> {
			TableIndex tableIndex = node.getTableIndex();
			if (tableIndex != null) {
				selectedTable.set(tableIndex);
			}
		});
		selectedTable.onChanged().addListener(this::showTable);
		selectedTable.set(firstIndex);

		createToolbarButtons(getPerspective(), applicationDbTables);
	}

	public void showTable(TableIndex tableIndex) {
		new TableExplorerView(getApplicationInstanceData(), tableIndex, timeLineView, tableView, formView, showDeletedRecords);
	}

	public void showTimeGraph(boolean visible) {
		timeLineView.setVisible(visible);
	}

	private void createToolbarButtons(Perspective perspective, List<TableIndex> applicationDbTables) {
		ToolbarButtonGroup buttonGroup = perspective.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		if (!applicationDbTables.isEmpty()) {
			ToolbarButton tablesSelectionButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.DATA_TABLE, getLocalized(Dictionary.TABLE), getLocalized(getMainApplication().getTitleKey())));
			SimpleItemView<?> itemView = new SimpleItemView<>();
			SimpleItemGroup<?> itemGroup = itemView.addSingleColumnGroup(ApplicationIcons.DATA_TABLE, getLocalized(Dictionary.TABLE));
			for (TableIndex tableIndex : applicationDbTables) {
				itemGroup.addItem(ApplicationIcons.TABLE, DbExplorerUtils.createTitleFromCamelCase(tableIndex.getName()), DbExplorerUtils.createTitleFromCamelCase(tableIndex.getDatabaseIndex().getName())).onClick.addListener(() -> selectedTable.set(tableIndex));
			}
			tablesSelectionButton.setDropDownComponent(itemView);
		}


		buttonGroup = perspective.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton showTimeGraphButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.CHART_LINE, getLocalized(Dictionary.TIMELINE), getLocalized(Dictionary.TIMELINE)));
		ToolbarButton hideTimeGraphButton = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.CHART_LINE, ApplicationIcons.ERROR), getLocalized(Dictionary.TIMELINE), getLocalized(Dictionary.TIMELINE)));
		hideTimeGraphButton.setVisible(false);

		if (isAllowed(Privileges.DATABASE_PERSPECTIVE, Privilege.SHOW_RECYCLE_BIN)) {
			buttonGroup = perspective.addWorkspaceButtonGroup(new ToolbarButtonGroup());
			ToolbarButton showDeletedButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.GARBAGE_EMPTY, getLocalized(Dictionary.RECYCLE_BIN), getLocalized(Dictionary.SHOW_RECYCLE_BIN)));
			ToolbarButton hideDeletedButton = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.GARBAGE_EMPTY, ApplicationIcons.ERROR), getLocalized(Dictionary.RECYCLE_BIN), getLocalized(Dictionary.RECYCLE_BIN)));
			hideDeletedButton.setVisible(false);

			showDeletedButton.onClick.addListener(() -> {
				showDeletedButton.setVisible(false);
				hideDeletedButton.setVisible(true);
				showDeletedRecords = true;
				showTable(selectedTable.get());
				tableView.focus();
			});

			hideDeletedButton.onClick.addListener(() -> {
				showDeletedButton.setVisible(true);
				hideDeletedButton.setVisible(false);
				showDeletedRecords = false;
				showTable(selectedTable.get());
				tableView.focus();
			});
		}

		showTimeGraphButton.onClick.addListener(() -> {
			showTimeGraphButton.setVisible(false);
			hideTimeGraphButton.setVisible(true);
			showTimeGraph(true);
			timeLineView.focus();
		});

		hideTimeGraphButton.onClick.addListener(() -> {
			hideTimeGraphButton.setVisible(false);
			showTimeGraphButton.setVisible(true);
			showTimeGraph(false);
			tableView.focus();
		});


	}

	public static class DatabaseNode {
		private final DatabaseIndex databaseIndex;
		private final TableIndex tableIndex;
		private DatabaseNode parent;

		public DatabaseNode(DatabaseIndex databaseIndex) {
			this.databaseIndex = databaseIndex;
			this.tableIndex = null;
		}

		public DatabaseNode(TableIndex tableIndex, DatabaseNode parent) {
			this.tableIndex = tableIndex;
			this.databaseIndex = null;
			this.parent = parent;
		}

		public Icon getIcon() {
			return tableIndex != null ? ApplicationIcons.TABLE : ApplicationIcons.DATA_TABLE;
		}

		public String getTitle() {
			String value = tableIndex != null ? tableIndex.getName() : databaseIndex.getName();
			return DbExplorerUtils.createTitleFromCamelCase(value);
		}

		public String getBadge() {
			if (tableIndex != null) {
				return tableIndex.getDeletedRecordsCount() > 0 ? tableIndex.getCount() + " ⌦" + tableIndex.getDeletedRecordsCount() : "" + tableIndex.getCount();
			} else {
				return null;
			}
		}

		public DatabaseIndex getDatabaseIndex() {
			return databaseIndex;
		}

		public TableIndex getTableIndex() {
			return tableIndex;
		}

		public DatabaseNode getParent() {
			return parent;
		}
	}
}
