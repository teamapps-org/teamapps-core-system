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
import org.teamapps.application.api.privilege.Privilege;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.api.ui.FormMetaFields;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.bootstrap.ApplicationInfoDataElement;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.tools.EntityListModelBuilder;
import org.teamapps.common.format.Color;
import org.teamapps.databinding.MutableValue;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.model.controlcenter.*;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.field.*;
import org.teamapps.ux.component.field.combobox.TagBoxWrappingMode;
import org.teamapps.ux.component.field.combobox.TagComboBox;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ApplicationUpdatesPerspective extends AbstractManagedApplicationPerspective {

	private final PerspectiveSessionData perspectiveSessionData;
	private final UserSessionData userSessionData;
	private final TwoWayBindableValue<Application> selectedApplication = TwoWayBindableValue.create();
	private final ApplicationsPerspectiveComponents perspectiveComponents;


	public ApplicationUpdatesPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		perspectiveComponents = new ApplicationsPerspectiveComponents(getApplicationInstanceData(), perspectiveInfoBadgeValue);
		selectedApplication.onChanged().addListener(app -> perspectiveComponents.getSelectedApplication().set(app));
		createUi();
	}

	private void createUi() {
		selectedApplication.set(getMainApplication());
		View applicationVersionsView = getPerspective().addView(View.createView(StandardLayout.CENTER, ApplicationIcons.BOX_SOFTWARE, getLocalized("applications.versions"), null));
		View applicationDetailsView = getPerspective().addView(View.createView(StandardLayout.RIGHT, ApplicationIcons.BOX_SOFTWARE, getLocalized("applications.application"), null));
		applicationDetailsView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.9f));

		EntityListModelBuilder<ApplicationVersion> applicationVersionModelBuilder = new EntityListModelBuilder<>(getApplicationInstanceData());
		applicationVersionModelBuilder.setRecords(selectedApplication.get().getVersions());
		applicationVersionModelBuilder.setRecordStringFunction(ApplicationVersion::getVersion);
		Table<ApplicationVersion> applicationsTable = applicationVersionModelBuilder.createTemplateFieldTableList(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, PropertyProviders.createApplicationVersionPropertyProvider(userSessionData), 36);
		applicationVersionModelBuilder.attachSearchField(applicationVersionsView);
		applicationVersionModelBuilder.attachViewCountHandler(applicationVersionsView, () -> getLocalized("applications.versions"));
		applicationVersionModelBuilder.updateModels();
		applicationVersionsView.setComponent(applicationsTable);


		ResponsiveForm<?> form = new ResponsiveForm<>(100, 0, 0);
		applicationDetailsView.setComponent(form);
		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(400);
		TextField appNameField = new TextField();
		TextField appTitleField = new TextField();
		TextField appDescriptionField = new TextField();
		TemplateField<Boolean> appTypeField = UiUtils.createBooleanTemplateField(ApplicationIcons.ERROR, getLocalized("applications.unmanagedApplication"), ApplicationIcons.OK, getLocalized("applications.managedApplication"));
		TextField installedAppVersionField = new TextField();

		TagComboBox<ApplicationPerspective> appPerspectivesCombo = new TagComboBox<>(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		appPerspectivesCombo.setWrappingMode(TagBoxWrappingMode.SINGLE_TAG_PER_LINE);
		appPerspectivesCombo.setPropertyProvider(PropertyProviders.createApplicationPerspectivePropertyProvider(userSessionData));

		TagComboBox<ManagedApplication> asMainAppCombo = UiUtils.createTagComboBox(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createManagedApplicationPropertyProvider(userSessionData));
		TagComboBox<ManagedApplication> usedInCombo = UiUtils.createTagComboBox(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createManagedApplicationPropertyProvider(userSessionData));

		TextField appVersionField = new TextField();
		DisplayField releaseField = new DisplayField(false, false);

		TemplateField<String> dataModelChangesField = UiUtils.createIconFixedIconTemplateField(ApplicationIcons.DATA_CLOUD);
		TemplateField<String> localizationChangesField = UiUtils.createIconFixedIconTemplateField(ApplicationIcons.EARTH);
		TemplateField<String> privilegeChangesField = UiUtils.createIconFixedIconTemplateField(ApplicationIcons.KEYS);
		TemplateField<String> perspectiveChangesField = UiUtils.createIconFixedIconTemplateField(ApplicationIcons.WINDOWS);

		DisplayField dataModelChangesDisplayField = new DisplayField(true, true);
		DisplayField localizationChangesDisplayField = new DisplayField(true, true);
		DisplayField privilegeChangesDisplayField = new DisplayField(true, true);
		DisplayField perspectiveChangesDisplayField = new DisplayField(true, true);

		List<AbstractField<?>> fields = Arrays.asList(
				appNameField,
				appTitleField,
				appDescriptionField,
				appTypeField,
				installedAppVersionField,
				appPerspectivesCombo,
				asMainAppCombo,
				usedInCombo,
				appVersionField
		);
		fields.forEach(field -> field.setEditingMode(FieldEditingMode.READONLY));

		formLayout.addSection().setCollapsible(false).setDrawHeaderLine(false);
		formLayout.addLabelAndComponent(null, getLocalized("applications.appName"), appNameField);
		formLayout.addLabelAndComponent(null, getLocalized("applications.appTitle"), appTitleField);
		formLayout.addLabelAndComponent(null, getLocalized("applications.appDescription"), appDescriptionField);
		formLayout.addLabelAndComponent(null, getLocalized("applications.appType"), appTypeField);
		formLayout.addLabelAndComponent(null, getLocalized("applications.appVersion"), appVersionField);
		formLayout.addLabelAndComponent(null, getLocalized("applications.perspectives"), appPerspectivesCombo);
		formLayout.addLabelAndComponent(null, getLocalized("applications.installedAsMainApp"), asMainAppCombo);
		formLayout.addLabelAndComponent(null, getLocalized("applications.usdInApplications"), usedInCombo);

		formLayout.addSection(ApplicationIcons.BOX_SOFTWARE, getLocalized("applications.appVersion")).setDrawHeaderLine(true);
		formLayout.addLabelAndComponent(null, getLocalized("applications.appVersion"), appVersionField);
		formLayout.addLabelAndComponent(null, getLocalized("applications.releaseNotes"), releaseField);

		formLayout.addLabelAndComponent(null, getLocalized("applications.dataModelChanges"), dataModelChangesField);
		formLayout.addLabelAndComponent(null, getLocalized("applications.localizationDataChanges"), localizationChangesField);
		formLayout.addLabelAndComponent(null, getLocalized("applications.privilegesDataChanges"), privilegeChangesField);
		formLayout.addLabelAndComponent(null, getLocalized("applications.perspectivesDataChanges"), perspectiveChangesField);

		formLayout.addSection(ApplicationIcons.DATA_CLOUD, getLocalized("applications.dataModelChanges")).setDrawHeaderLine(true).setCollapsed(true);
		formLayout.addLabelAndComponent(dataModelChangesDisplayField);

		formLayout.addSection(ApplicationIcons.EARTH, getLocalized("applications.localizationDataChanges")).setDrawHeaderLine(true).setCollapsed(true);
		formLayout.addLabelAndComponent(localizationChangesDisplayField);

		formLayout.addSection(ApplicationIcons.KEYS, getLocalized("applications.privilegesDataChanges")).setDrawHeaderLine(true).setCollapsed(true);
		formLayout.addLabelAndComponent(privilegeChangesDisplayField);

		formLayout.addSection(ApplicationIcons.WINDOWS, getLocalized("applications.perspectivesDataChanges")).setDrawHeaderLine(true).setCollapsed(true);
		formLayout.addLabelAndComponent(perspectiveChangesDisplayField);

		FormMetaFields formMetaFields = getApplicationInstanceData().getComponentFactory().createFormMetaFields();
		formMetaFields.addMetaFields(formLayout, false);
		applicationVersionModelBuilder.getOnSelectionEvent().addListener(formMetaFields::updateEntity);

		applicationVersionModelBuilder.getOnSelectionEvent().addListener(version -> {
			Application app = version.getApplication();
			applicationVersionModelBuilder.setRecords(app.getVersions());
			appNameField.setValue(app.getName());
			appTitleField.setValue(getLocalized(app.getTitleKey()));
			appDescriptionField.setValue(getLocalized(app.getDescriptionKey()));
			appTypeField.setValue(app.getUnmanagedApplication());
			appVersionField.setValue(app.getInstalledVersion().getVersion());
			appPerspectivesCombo.setValue(app.getPerspectives());
			asMainAppCombo.setValue(app.getInstalledAsMainApplication());
			usedInCombo.setValue(app.getPerspectives().stream()
					.flatMap(p -> p.getManagedPerspectives().stream())
					.map(ManagedApplicationPerspective::getManagedApplication)
					.filter(Objects::nonNull)
					.distinct()
					.collect(Collectors.toList()));

			dataModelChangesField.setValue(ApplicationInfoDataElement.getChangeString(version.getDataModelData()));
			localizationChangesField.setValue(ApplicationInfoDataElement.getChangeString(version.getLocalizationData()));
			privilegeChangesField.setValue(ApplicationInfoDataElement.getChangeString(version.getPrivilegeData()));
			perspectiveChangesField.setValue(ApplicationInfoDataElement.getChangeString(version.getPerspectiveData()));
			dataModelChangesDisplayField.setValue(ApplicationInfoDataElement.getMultiLineChangeHtml(version.getDataModelData(), getLocalized("applications.addedData"), getLocalized("applications.removedData")));
			localizationChangesDisplayField.setValue(ApplicationInfoDataElement.getMultiLineChangeHtml(version.getLocalizationData(), getLocalized("applications.addedData"), getLocalized("applications.removedData")));
			privilegeChangesDisplayField.setValue(ApplicationInfoDataElement.getMultiLineChangeHtml(version.getPrivilegeData(), getLocalized("applications.addedData"), getLocalized("applications.removedData")));
			perspectiveChangesDisplayField.setValue(ApplicationInfoDataElement.getMultiLineChangeHtml(version.getPerspectiveData(), getLocalized("applications.addedData"), getLocalized("applications.removedData")));
		});


		ToolbarButtonGroup buttonGroup = applicationDetailsView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		if (isAllowed(Privileges.APPLICATION_UPDATES_PERSPECTIVE, Privilege.EXECUTE)) {
			buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.UPLOAD, getLocalized("applications.installUpdate"), getLocalized("applications.installUpdate"))).onClick.addListener(() -> {
				perspectiveComponents.showInstallApplicationDialogue(selectedApplication.get());
			});
		}

		buttonGroup = applicationDetailsView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		perspectiveComponents.createToolbarButtons(buttonGroup);

		if (isAllowed(Privileges.APPLICATION_UPDATES_PERSPECTIVE, Privilege.RESTORE)) {
			buttonGroup = applicationDetailsView.addLocalButtonGroup(new ToolbarButtonGroup());
			ToolbarButton rollbackButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.SIGN_WARNING_HARMFUL, getLocalized("applications.performRollback")));
			rollbackButton.onClick.addListener(() -> {

			});
		}

		selectedApplication.set(getMainApplication());
		applicationVersionModelBuilder.setSelectedRecord(getMainApplication().getInstalledVersion());
	}

}
