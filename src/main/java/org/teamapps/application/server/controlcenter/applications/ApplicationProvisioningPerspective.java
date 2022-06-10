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
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.system.utils.ApplicationUiUtils;
import org.teamapps.application.server.ui.localize.LocalizationTranslationKeyField;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.combo.RecordComboBox;
import org.teamapps.application.ux.form.FormController;
import org.teamapps.application.ux.form.FormPanel;
import org.teamapps.application.ux.form.FormWindow;
import org.teamapps.application.tools.EntityListModelBuilder;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.application.ux.view.MasterDetailController;
import org.teamapps.common.format.Color;
import org.teamapps.databinding.MutableValue;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.field.CheckBox;
import org.teamapps.ux.component.field.Fields;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.table.TableColumn;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationProvisioningPerspective extends AbstractManagedApplicationPerspective {

	private final UserSessionData userSessionData;


	public ApplicationProvisioningPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		PerspectiveSessionData perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		MasterDetailController<ManagedApplication> masterDetailController = new MasterDetailController<>(ApplicationIcons.INSTALL, getLocalized("applicationProvisioning.provisionedApplications"), getApplicationInstanceData(), ManagedApplication::filter, Privileges.APPLICATION_PROVISIONING_PERSPECTIVE);
		EntityModelBuilder<ManagedApplication> entityModelBuilder = masterDetailController.getEntityModelBuilder();
		FormController<ManagedApplication> formController = masterDetailController.getFormController();
		ResponsiveForm<ManagedApplication> form = masterDetailController.getResponsiveForm();

		Table<ManagedApplication> applicationsTable = entityModelBuilder.createTemplateFieldTableList(BaseTemplate.LIST_ITEM_VERY_LARGE_ICON_TWO_LINES, PropertyProviders.createManagedApplicationPropertyProvider(userSessionData), 60);
		applicationsTable.setStripedRows(false);
		entityModelBuilder.updateModels();

		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(400);

		ComboBox<Application> applicationComboBox = ApplicationUiUtils.createApplicationComboBox(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, userSessionData);
		ComboBox<OrganizationField> organizationFieldCombo = OrganizationUtils.createOrganizationFieldCombo(getApplicationInstanceData());
		ComboBox<Icon> iconComboBox = ApplicationIcons.createIconComboBox(BaseTemplate.LIST_ITEM_LARGE_ICON_SINGLE_LINE, true);
		iconComboBox.setShowClearButton(true);

		LocalizationTranslationKeyField titleKeyField = new LocalizationTranslationKeyField(getLocalized("applications.createNewTitle"), getApplicationInstanceData(), userSessionData.getRegistry(), applicationComboBox::getValue);
		LocalizationTranslationKeyField descriptionKeyField = new LocalizationTranslationKeyField(getLocalized("applications.createNewDescription"), getApplicationInstanceData(), userSessionData.getRegistry(), applicationComboBox::getValue);

		ComboBox<String> selectionField = (ComboBox<String>) titleKeyField.getSelectionField();
		selectionField.setTemplate(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);

		CheckBox darkThemeCheckBox = new CheckBox(getLocalized("applications.darkTheme"));
		CheckBox toolbarAppMenuCheckbox = new CheckBox(getLocalized("applications.useToolbarApplicationMenu"));
		CheckBox startOnLoginCheckbox = new CheckBox(getLocalized("applications.startOnLogin"));

		EntityListModelBuilder<ManagedApplicationPerspective> perspectiveModelBuilder = new EntityListModelBuilder<>(getApplicationInstanceData());
		Table<ManagedApplicationPerspective> perspectivesList = perspectiveModelBuilder.createTable();
		perspectivesList.setDisplayAsList(true);
		perspectivesList.setForceFitWidth(true);
		perspectivesList.setRowHeight(26);
		TemplateField<ManagedApplicationPerspective> perspectiveColumnField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createManagedApplicationPerspectivePropertyProvider(userSessionData));
		TemplateField<Application> applicationColumnField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createApplicationPropertyProvider(userSessionData));

		perspectivesList.addColumn(new TableColumn<>("perspective", getLocalized("applications.perspective"), perspectiveColumnField));
		perspectivesList.addColumn(new TableColumn<>("application", getLocalized("applications.application"), applicationColumnField));
		perspectivesList.setPropertyExtractor((record, propertyName) -> switch (propertyName){
			case "perspective" -> record;
			case "application" -> record.getApplicationPerspective() != null ? record.getApplicationPerspective().getApplication() : null;
			default -> null;
		});

		perspectivesList.setStripedRows(false);
		FormPanel formPanel = new FormPanel(getApplicationInstanceData());
		formPanel.setTable(perspectivesList, true, true, true);
		formPanel.addButtonGroup();
		ToolbarButton moveUpButton = formPanel.addButton(ApplicationIcons.NAVIGATE_UP, getLocalized("applications.moveUp"));
		ToolbarButton moveDownButton = formPanel.addButton(ApplicationIcons.NAVIGATE_DOWN, getLocalized("applications.moveDown"));

		CheckBox hideApplicationCheckBox = new CheckBox(getLocalized(Dictionary.HIDE));
		ComboBox<ManagedApplicationGroup> applicationGroupComboBox = ApplicationUiUtils.createApplicationGroupComboBox(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, getApplicationInstanceData());

		Arrays.asList(applicationComboBox, iconComboBox, titleKeyField.getKeyDisplayField(), descriptionKeyField.getKeyDisplayField(), applicationGroupComboBox).forEach(f -> f.setRequired(true));

		formLayout.addSection().setDrawHeaderLine(false).setCollapsible(false);
		formLayout.addLabelAndField(null, getLocalized("applications.mainApplication"), applicationComboBox);
		formLayout.addLabelAndField(null, getLocalized(Dictionary.HIDDEN), hideApplicationCheckBox);
		formLayout.addLabelAndField(null, getLocalized("applications.organizationField"), organizationFieldCombo);
		formLayout.addLabelAndField(null, getLocalized("applications.applicationIcon"), iconComboBox);
		formLayout.addLabelAndField(null, getLocalized("applications.appTitle"), titleKeyField.getSelectionField());
		formLayout.addLabelAndComponent(null, null, titleKeyField.getKeyLinkButton());
		formLayout.addLabelAndField(null, getLocalized("applications.appDescription"), descriptionKeyField.getSelectionField());
		formLayout.addLabelAndComponent(null, null, descriptionKeyField.getKeyLinkButton());
		formLayout.addLabelAndField(null, getLocalized("applications.darkTheme"), darkThemeCheckBox);
		formLayout.addLabelAndField(null, getLocalized("applications.toolbarAppMenu"), toolbarAppMenuCheckbox);
		formLayout.addLabelAndField(null, getLocalized("applications.startOnLogin"), startOnLoginCheckbox);
		formLayout.addLabelAndComponent(null, getLocalized("applications.perspectives"), formPanel.getPanel());
		formLayout.addLabelAndField(null, getLocalized("applications.applicationGroup"), applicationGroupComboBox);

		formPanel.getAddButton().onClick.addListener(() -> showPerspectiveFormWindow(null, perspectiveModelBuilder, formController));
		formPanel.getEditButton().onClick.addListener(() -> showPerspectiveFormWindow(perspectivesList.getSelectedRecord(), perspectiveModelBuilder, formController));
		formPanel.getDeleteButton().onClick.addListener(() -> perspectiveModelBuilder.removeRecord(perspectivesList.getSelectedRecord()));

		masterDetailController.createViews(getPerspective(), applicationsTable, formLayout);

		formController.addNotNull(applicationComboBox);
		formController.addNotNull(titleKeyField.getSelectionField());
		formController.addNotNull(descriptionKeyField.getSelectionField());
		formController.addNotNull(applicationGroupComboBox);
		formController.setSaveEntityHandler(application -> {
			if (!perspectiveModelBuilder.getRecords().isEmpty() || application.isSingleApplication() || application.getMainApplication().isUnmanagedApplication()) {
				Application mainApplication = applicationComboBox.getValue();
				application.setMainApplication(mainApplication);
				application.setHidden(hideApplicationCheckBox.getValue());
				application.setOrganizationField(organizationFieldCombo.getValue());
				application.setIcon(mainApplication.getIcon().equals(IconUtils.encodeNoStyle(iconComboBox.getValue())) ? null : IconUtils.encodeNoStyle(iconComboBox.getValue()));
				application.setTitleKey(mainApplication.getTitleKey().equals(titleKeyField.getKey()) ? null : titleKeyField.getKey());
				application.setDescriptionKey(mainApplication.getDescriptionKey().equals(descriptionKeyField.getKey()) ? null : descriptionKeyField.getKey());
				application.setDarkTheme(darkThemeCheckBox.getValue());
				application.setToolbarApplicationMenu(toolbarAppMenuCheckbox.getValue());
				application.setStartOnLogin(startOnLoginCheckbox.getValue());
				application.setPerspectives(perspectiveModelBuilder.getRecords());
				application.setApplicationGroup(applicationGroupComboBox.getValue());
				application.save();
				int pos = 0;
				for (ManagedApplicationPerspective perspective : perspectiveModelBuilder.getRecords()) {
					perspective.setListingPosition(pos++).save();
				}
				return true;
			} else {
				return false;
			}
		});

		moveUpButton.onClick.addListener(() -> changePerspectiveOrder(perspectiveModelBuilder, perspectivesList, true, formController));
		moveDownButton.onClick.addListener(() -> changePerspectiveOrder(perspectiveModelBuilder, perspectivesList, false, formController));

		applicationComboBox.onValueChanged.addListener(app -> {
			iconComboBox.setValue(app != null ? IconUtils.decodeIcon(app.getIcon()) : null);
			titleKeyField.setKey(app != null ? app.getTitleKey() : null);
			descriptionKeyField.setKey(app != null ? app.getDescriptionKey() : null);
			ManagedApplication selectedRecord = entityModelBuilder.getSelectedRecord();
			if (selectedRecord != null && !selectedRecord.isStored()) {
				List<ManagedApplicationPerspective> perspectives = app.getPerspectives().stream().filter(ApplicationPerspective::getAutoProvision).map(p -> ManagedApplicationPerspective.create().setApplicationPerspective(p)).collect(Collectors.toList());
				perspectiveModelBuilder.setRecords(perspectives);
			}
		});

		entityModelBuilder.getOnSelectionEvent().addListener(app -> {
			applicationComboBox.setValue(app.getMainApplication());
			hideApplicationCheckBox.setValue(app.getHidden());
			organizationFieldCombo.setValue(app.getOrganizationField());
			iconComboBox.setValue(app.getIcon() != null ? IconUtils.decodeIcon(app.getIcon()) : app.getMainApplication() != null ? IconUtils.decodeIcon(app.getMainApplication().getIcon()) : null);
			titleKeyField.setKey(app.getTitleKey() != null ? app.getTitleKey() : app.getMainApplication() != null ? app.getMainApplication().getTitleKey() : null);
			descriptionKeyField.setKey(app.getDescriptionKey() != null ? app.getDescriptionKey() : app.getMainApplication() != null ? app.getMainApplication().getDescriptionKey() : null);
			darkThemeCheckBox.setValue(app.isDarkTheme());
			toolbarAppMenuCheckbox.setValue(app.isToolbarApplicationMenu());
			startOnLoginCheckbox.setValue(app.isStartOnLogin());
			perspectiveModelBuilder.setRecords(app.getPerspectives().stream().filter(perspective -> perspective.getApplicationPerspective() != null).sorted((Comparator.comparingInt(ManagedApplicationPerspective::getListingPosition))).collect(Collectors.toList()));
			applicationGroupComboBox.setValue(app.getApplicationGroup());
		});
		entityModelBuilder.setSelectedRecord(ManagedApplication.create());
	}

	private void showPerspectiveFormWindow(ManagedApplicationPerspective managedApplicationPerspective, EntityListModelBuilder<ManagedApplicationPerspective> perspectiveModelBuilder, FormController<ManagedApplication> formController) {
		ManagedApplicationPerspective perspective = managedApplicationPerspective != null ? managedApplicationPerspective : ManagedApplicationPerspective.create();
		FormWindow formWindow = new FormWindow(ApplicationIcons.WINDOWS, getLocalized("applications.perspective"), getApplicationInstanceData());
		formWindow.addSaveButton();
		formWindow.addCancelButton();
		formWindow.addSection();

		ComboBox<Application> applicationComboBox = ApplicationUiUtils.createApplicationComboBox(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, userSessionData);
		RecordComboBox<ApplicationPerspective> applicationsPerspectiveCombo = new RecordComboBox<>(PropertyProviders.createApplicationPerspectivePropertyProvider(userSessionData), BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);

		ComboBox<Icon> iconComboBox = ApplicationIcons.createIconComboBox(BaseTemplate.LIST_ITEM_LARGE_ICON_SINGLE_LINE, true);

		LocalizationTranslationKeyField titleKeyField = new LocalizationTranslationKeyField(getLocalized("applications.createNewTitle"), getApplicationInstanceData(), userSessionData.getRegistry(), applicationComboBox::getValue);
		LocalizationTranslationKeyField descriptionKeyField = new LocalizationTranslationKeyField(getLocalized("applications.createNewDescription"), getApplicationInstanceData(), userSessionData.getRegistry(), applicationComboBox::getValue);
		CheckBox toolbarPerspectiveMenuCheckBox = new CheckBox(getLocalized("applications.useToolbarPerspectiveMenu"));

		formWindow.addField(getLocalized("applications.application"), applicationComboBox);
		formWindow.addField(getLocalized("applications.perspective"), applicationsPerspectiveCombo);
		formWindow.addField(getLocalized("applications.perspectiveIcon"), iconComboBox);
		formWindow.addField(getLocalized("applications.perspectiveTitle"), titleKeyField.getSelectionField());
		formWindow.addField(null, titleKeyField.getKeyLinkButton());
		formWindow.addField(getLocalized("applications.perspectiveDescription"), descriptionKeyField.getSelectionField());
		formWindow.addField(null, descriptionKeyField.getKeyLinkButton());
		formWindow.addField(getLocalized("applications.toolbarPerspectiveMenu"), toolbarPerspectiveMenuCheckBox);

		if (managedApplicationPerspective != null) {
			applicationComboBox.setValue(perspective.getApplicationPerspective().getApplication());
			applicationsPerspectiveCombo.setValue(perspective.getApplicationPerspective());
			iconComboBox.setValue(perspective.getIconOverride() != null ? IconUtils.decodeIcon(perspective.getIconOverride()) : IconUtils.decodeIcon(perspective.getApplicationPerspective().getIcon()));
			titleKeyField.setKey(perspective.getTitleKeyOverride() != null ? perspective.getTitleKeyOverride() : perspective.getApplicationPerspective().getTitleKey());
			descriptionKeyField.setKey(perspective.getDescriptionKeyOverride() != null ? perspective.getDescriptionKeyOverride() : perspective.getApplicationPerspective().getDescriptionKey());
			toolbarPerspectiveMenuCheckBox.setValue(perspective.getToolbarPerspectiveMenu());
		}

		Arrays.asList(applicationsPerspectiveCombo, iconComboBox, titleKeyField.getSelectionField(), descriptionKeyField.getSelectionField(), applicationsPerspectiveCombo).forEach(f -> f.setRequired(true));

		applicationComboBox.onValueChanged.addListener(app -> applicationsPerspectiveCombo.setRecords(app.getPerspectives()));
		applicationsPerspectiveCombo.onValueChanged.addListener(p -> {
			iconComboBox.setValue(p != null ? IconUtils.decodeIcon(p.getIcon()) : null);
			titleKeyField.setKey(p != null ? p.getTitleKey() : null);
			descriptionKeyField.setKey(p != null ? p.getDescriptionKey() : null);
		});

		formWindow.getSaveButton().onClick.addListener(() -> {
			if (Fields.validateAll(applicationsPerspectiveCombo, iconComboBox, titleKeyField.getSelectionField(), descriptionKeyField.getSelectionField(), applicationsPerspectiveCombo)) {
				ApplicationPerspective applicationPerspective = applicationsPerspectiveCombo.getValue();
				perspective.setApplicationPerspective(applicationPerspective);
				perspective.setIconOverride(applicationPerspective.getIcon().equals(iconComboBox.getValue()) ? null : IconUtils.encodeNoStyle(iconComboBox.getValue()));
				perspective.setTitleKeyOverride(applicationPerspective.getTitleKey().equals(titleKeyField.getKey()) ? null : titleKeyField.getKey());
				perspective.setDescriptionKeyOverride(applicationPerspective.getDescriptionKey().equals(descriptionKeyField.getKey()) ? null : descriptionKeyField.getKey());
				perspective.setToolbarPerspectiveMenu(toolbarPerspectiveMenuCheckBox.getValue());
				perspective.save();
				if (managedApplicationPerspective == null) {
					perspectiveModelBuilder.addRecord(perspective);
					formController.setFormDataModified();
				}
				formWindow.close();
			}
		});

		formWindow.show();
	}

	private void changePerspectiveOrder(EntityListModelBuilder<ManagedApplicationPerspective> perspectiveModelBuilder, Table<ManagedApplicationPerspective> perspectivesList, boolean moveUp, FormController<ManagedApplication> formController) {
		ManagedApplicationPerspective selectedPerspective = perspectivesList.getSelectedRecord();
		if (selectedPerspective != null) {
			List<ManagedApplicationPerspective> perspectives = perspectiveModelBuilder.getRecords();
			int position = 0;
			int changeValue = moveUp ? -5 : 15;
			for (ManagedApplicationPerspective perspective : perspectives) {
				if (perspective.equals(selectedPerspective)) {
					perspective.setListingPosition(position + changeValue);
				} else {
					position += 10;
					perspective.setListingPosition(position);
				}
			}
			perspectiveModelBuilder.setRecords(perspectives.stream().sorted((Comparator.comparingInt(ManagedApplicationPerspective::getListingPosition))).collect(Collectors.toList()));
			formController.setFormDataModified();
		}
	}


}
