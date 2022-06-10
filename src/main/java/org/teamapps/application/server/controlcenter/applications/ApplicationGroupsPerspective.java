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
package org.teamapps.application.server.controlcenter.applications;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.api.ui.FormMetaFields;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.ui.localize.LocalizationTranslationKeyField;
import org.teamapps.application.tools.EntityListModelBuilder;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.form.FormPanel;
import org.teamapps.common.format.Color;
import org.teamapps.databinding.MutableValue;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.ManagedApplication;
import org.teamapps.model.controlcenter.ManagedApplicationGroup;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.field.Fields;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationGroupsPerspective extends AbstractManagedApplicationPerspective {

	private final PerspectiveSessionData perspectiveSessionData;
	private final UserSessionData userSessionData;


	public ApplicationGroupsPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		View groupsView = View.createView(StandardLayout.CENTER, ApplicationIcons.WINDOWS, getLocalized("applicationGroups.title"), null);
		View applicationDetailsView = View.createView(StandardLayout.RIGHT, ApplicationIcons.WINDOWS, getLocalized("applicationGroups.title"), null);
		applicationDetailsView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.9f));

		ToolbarButtonGroup buttonGroup = applicationDetailsView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton addGroupButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.ADD, getLocalized("applicationGroups.addGroup"), getLocalized("applicationGroups.addGroup.desc")));

		buttonGroup = applicationDetailsView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton groupMoveUpButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.NAVIGATE_UP, getLocalized("applicationGroups.moveUp"), getLocalized("applicationGroups.moveUp.desc")));
		ToolbarButton groupMoveDownButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.NAVIGATE_DOWN, getLocalized("applicationGroups.moveDown"), getLocalized("applicationGroups.moveDown.desc")));

		buttonGroup = applicationDetailsView.addLocalButtonGroup(new ToolbarButtonGroup());
		ToolbarButton saveGroupButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.FLOPPY_DISK, getLocalized(Dictionary.SAVE_CHANGES)));


		EntityListModelBuilder<ManagedApplicationGroup> entityListModelBuilder = new EntityListModelBuilder<>(getApplicationInstanceData(), group -> getLocalized(group.getTitleKey()));
		entityListModelBuilder.setRecords(ManagedApplicationGroup.getAll().stream().sorted((Comparator.comparingInt(ManagedApplicationGroup::getListingPosition))).collect(Collectors.toList()));
		Table<ManagedApplicationGroup> groupTable = entityListModelBuilder.createTemplateFieldTableList(BaseTemplate.LIST_ITEM_LARGE_ICON_SINGLE_LINE, PropertyProviders.createManagedApplicationGroupPropertyProvider(getApplicationInstanceData()), 40);
		groupTable.setStripedRows(false);
		groupsView.setComponent(groupTable);
		entityListModelBuilder.onDataChanged.fire();
		entityListModelBuilder.attachSearchField(groupsView);
		entityListModelBuilder.attachViewCountHandler(groupsView, () -> getLocalized("applicationGroups.title"));


		getPerspective().addView(groupsView);
		getPerspective().addView(applicationDetailsView);

		ResponsiveForm groupForm = new ResponsiveForm(100, 150, 0);
		ResponsiveFormLayout formLayout = groupForm.addResponsiveFormLayout(400);

		ComboBox<Icon> iconComboBox = ApplicationIcons.createIconComboBox(BaseTemplate.LIST_ITEM_LARGE_ICON_SINGLE_LINE, true);

		LocalizationTranslationKeyField titleKeyField = new LocalizationTranslationKeyField(getLocalized("applications.createNewTitle"), getApplicationInstanceData(), userSessionData.getRegistry(), null);

		EntityListModelBuilder<ManagedApplication> applicationModelBuilder = new EntityListModelBuilder<>(getApplicationInstanceData());
		Table<ManagedApplication> applicationTable = applicationModelBuilder.createTemplateFieldTableList(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createManagedApplicationPropertyProvider(userSessionData), 26);
		FormPanel formPanel = new FormPanel(getApplicationInstanceData());
		formPanel.setTable(applicationTable, true, false, false);
		ToolbarButton moveUpButton = formPanel.addButton(ApplicationIcons.NAVIGATE_UP, getLocalized("applications.moveUp"));
		ToolbarButton moveDownButton = formPanel.addButton(ApplicationIcons.NAVIGATE_DOWN, getLocalized("applications.moveDown"));

		formLayout.addSection().setDrawHeaderLine(false).setCollapsible(false);
		formLayout.addLabelAndComponent(null, getLocalized("applicationGroups.groupIcon"), iconComboBox);
		formLayout.addLabelAndComponent(null, getLocalized("applicationGroups.groupTitle"), titleKeyField.getSelectionField());
		formLayout.addLabelAndComponent(null, null, titleKeyField.getKeyLinkButton());
		formLayout.addLabelAndComponent(null, getLocalized(Dictionary.APPLICATIONS), formPanel.getPanel());

		FormMetaFields formMetaFields = getApplicationInstanceData().getComponentFactory().createFormMetaFields();
		formMetaFields.addMetaFields(formLayout, false);
		entityListModelBuilder.getOnSelectionEvent().addListener(formMetaFields::updateEntity);

		Arrays.asList(iconComboBox, titleKeyField.getSelectionField()).forEach(f -> f.setRequired(true));
		applicationDetailsView.setComponent(groupForm);

		addGroupButton.onClick.addListener(() -> entityListModelBuilder.setSelectedRecord(ManagedApplicationGroup.create()));

		moveUpButton.onClick.addListener(() -> changeApplicationOrder(applicationModelBuilder, applicationTable, true));
		moveDownButton.onClick.addListener(() -> changeApplicationOrder(applicationModelBuilder, applicationTable, false));

		groupMoveUpButton.onClick.addListener(() -> changeGroupOrder(entityListModelBuilder, entityListModelBuilder.getSelectedRecord(), true));
		groupMoveDownButton.onClick.addListener(() -> changeGroupOrder(entityListModelBuilder, entityListModelBuilder.getSelectedRecord(), false));

		entityListModelBuilder.getOnSelectionEvent().addListener(group -> {
			iconComboBox.setValue(IconUtils.decodeIcon(group.getIcon()));
			titleKeyField.setKey(group.getTitleKey());
			applicationModelBuilder.setRecords(group.getApplications());
		});

		saveGroupButton.onClick.addListener(() -> {
			ManagedApplicationGroup group = entityListModelBuilder.getSelectedRecord();
			if (group == null) {
				return;
			}
			if (Fields.validateAll(iconComboBox, titleKeyField.getSelectionField())) {
				group.setIcon(IconUtils.encodeNoStyle(iconComboBox.getValue()));
				group.setTitleKey(titleKeyField.getKey());
				group.save();
				int pos = 0;
				for (ManagedApplication application : applicationModelBuilder.getRecords()) {
					application.setListingPosition(pos++).save();
				}
				entityListModelBuilder.setRecords(ManagedApplicationGroup.getAll().stream().sorted((Comparator.comparingInt(ManagedApplicationGroup::getListingPosition))).collect(Collectors.toList()));
				UiUtils.showSaveNotification(true, getApplicationInstanceData());
			} else {
				UiUtils.showSaveNotification(false, getApplicationInstanceData());
			}
		});
		entityListModelBuilder.setSelectedRecord(ManagedApplicationGroup.create());
	}

	private void changeApplicationOrder(EntityListModelBuilder<ManagedApplication> applicationModelBuilder, Table<ManagedApplication> applicationTable, boolean moveUp) {
		ManagedApplication selectedPerspective = applicationTable.getSelectedRecord();
		if (selectedPerspective != null) {
			List<ManagedApplication> applications = applicationModelBuilder.getRecords();
			int position = 0;
			int changeValue = moveUp ? -5 : 15;
			for (ManagedApplication application : applications) {
				if (application.equals(selectedPerspective)) {
					application.setListingPosition(position + changeValue);
				} else {
					position += 10;
					application.setListingPosition(position);
				}
			}
			applicationModelBuilder.setRecords(applications.stream().sorted((Comparator.comparingInt(ManagedApplication::getListingPosition))).collect(Collectors.toList()));
		}
	}

	private void changeGroupOrder(EntityListModelBuilder<ManagedApplicationGroup> groupModelBuilder, ManagedApplicationGroup group, boolean moveUp) {
		if (group != null) {
			List<ManagedApplicationGroup> groups = groupModelBuilder.getRecords();
			int position = 0;
			int changeValue = moveUp ? -5 : 15;
			for (ManagedApplicationGroup application : groups) {
				if (application.equals(group)) {
					application.setListingPosition(position + changeValue);
				} else {
					position += 10;
					application.setListingPosition(position);
				}
			}
			groupModelBuilder.setRecords(groups.stream().sorted((Comparator.comparingInt(ManagedApplicationGroup::getListingPosition))).collect(Collectors.toList()));
			int pos = 0;
			for (ManagedApplicationGroup g : groupModelBuilder.getRecords()) {
				g.setListingPosition(pos++).save();
			}
		}
	}


}
