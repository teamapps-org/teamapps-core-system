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
package org.teamapps.application.server.controlcenter.accesscontrol;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.privilege.ApplicationRole;
import org.teamapps.application.api.privilege.Privilege;
import org.teamapps.application.api.privilege.PrivilegeGroup;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.application.tools.RecordListModelBuilder;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.combo.ComboBoxUtils;
import org.teamapps.application.ux.form.FormController;
import org.teamapps.application.ux.form.FormPanel;
import org.teamapps.application.ux.view.MasterDetailController;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.databinding.MutableValue;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.universaldb.pojo.Query;
import org.teamapps.ux.component.field.AbstractField;
import org.teamapps.ux.component.field.CheckBox;
import org.teamapps.ux.component.field.Label;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.field.combobox.TagComboBox;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.table.TableColumn;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.model.ComboBoxModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AccessControlAppRolePerspective extends AbstractManagedApplicationPerspective {

	private final PerspectiveSessionData perspectiveSessionData;
	private final UserSessionData userSessionData;


	public AccessControlAppRolePerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		Supplier<Query<RoleApplicationRoleAssignment>> querySupplier = () -> isAppFilter() ? RoleApplicationRoleAssignment.filter().organizationFieldFilter(NumericFilter.equalsFilter(getOrganizationField().getId())) : RoleApplicationRoleAssignment.filter();
		MasterDetailController<RoleApplicationRoleAssignment> masterDetailController = new MasterDetailController<>(ApplicationIcons.KEYS, getLocalized("accessControl.accessControlListEntries"), getApplicationInstanceData(), querySupplier, Privileges.ACCESS_CONTROL_APP_ROLE_PERSPECTIVE);
		EntityModelBuilder<RoleApplicationRoleAssignment> entityModelBuilder = masterDetailController.getEntityModelBuilder();
		FormController<RoleApplicationRoleAssignment> formController = masterDetailController.getFormController();
		ResponsiveForm<RoleApplicationRoleAssignment> form = masterDetailController.getResponsiveForm();

		Table<RoleApplicationRoleAssignment> table = entityModelBuilder.createTable();
		table.setDisplayAsList(true);
		table.setRowHeight(28);
		table.setStripedRows(false);
		entityModelBuilder.updateModels();


		TemplateField<Role> roleTableField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.createRolePropertyProvider(getApplicationInstanceData()));
		TemplateField<Application> applicationTableField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.createApplicationPropertyProvider(userSessionData));
		TemplateField<RoleApplicationRoleAssignment> applicationRoleTemplateField = createApplicationRoleTemplateField();

		TemplateField<OrganizationUnit> customOrganizationUnitTableField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.creatOrganizationUnitPropertyProvider(getApplicationInstanceData()));

		table.addColumn(RoleApplicationRoleAssignment.FIELD_ROLE, getLocalized("roles.role"), roleTableField).setDefaultWidth(200);
		table.addColumn(RoleApplicationRoleAssignment.FIELD_APPLICATION, getLocalized("applications.application"), applicationTableField).setDefaultWidth(200);
		table.addColumn(RoleApplicationRoleAssignment.FIELD_APPLICATION_ROLE_NAME, getLocalized("accessControl.applicationRole"), applicationRoleTemplateField).setDefaultWidth(200);
		table.addColumn(RoleApplicationRoleAssignment.FIELD_FIXED_ORGANIZATION_ROOT, getLocalized("accessControl.customOrganizationUnit"), customOrganizationUnitTableField).setDefaultWidth(200);

		table.setPropertyExtractor((rolePrivilegeAssignment, propertyName) -> switch (propertyName) {
			case RoleApplicationRoleAssignment.FIELD_ROLE -> rolePrivilegeAssignment.getRole();
			case RoleApplicationRoleAssignment.FIELD_APPLICATION -> rolePrivilegeAssignment.getApplication();
			case RoleApplicationRoleAssignment.FIELD_APPLICATION_ROLE_NAME -> rolePrivilegeAssignment;
			case RoleApplicationRoleAssignment.FIELD_FIXED_ORGANIZATION_ROOT -> rolePrivilegeAssignment.getFixedOrganizationRoot();
			default -> null;
		});

		ComboBox<Role> roleComboBox = ComboBoxUtils.createRecordComboBox(
				() -> isAppFilter() ? Role.filter().organizationField(NumericFilter.equalsFilter(getOrganizationField().getId())).execute() : Role.getAll(),
				PropertyProviders.createRolePropertyProvider(getApplicationInstanceData()),
				BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES
		);

		ComboBox<Application> applicationComboBox = createApplicationComboBox();
		ComboBox<ApplicationRole> applicationRoleComboBox = createApplicationRoleComboBox(applicationComboBox);
		ComboBox<OrganizationField> organizationFieldFilterComboBox = createOrganizationFieldComboBox();
		AbstractField<OrganizationUnitView> customOrganizationUnit = formController.getOrganizationUnitViewField(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, true);
		TagComboBox<OrganizationUnitType> organizationUnitTypeFilterTagComboBox = OrganizationUtils.createOrganizationUnitTypeTagComboBox(50, getApplicationInstanceData());
		CheckBox noOrgUnitInheritanceCheckBox = new CheckBox(getLocalized("accessControl.noInheritanceOfOrganizationalUnits"));


		RecordListModelBuilder<PrivilegeGroup> appRoleModelBuilder = new RecordListModelBuilder<>(getApplicationInstanceData());
		Table<PrivilegeGroup> privilegeGroupTable = appRoleModelBuilder.createListTable(false);
		TemplateField<PrivilegeGroup> privilegeGroupTemplateField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.createPrivilegeGroupPropertyProvider(userSessionData, applicationComboBox::getValue));
		TagComboBox<Privilege> privilegeTagComboBox = UiUtils.createTagComboBox(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createPrivilegePropertyProvider(userSessionData, applicationComboBox::getValue));

		privilegeGroupTable.addColumn("group", getLocalized("accessControl.privilegeGroup"), privilegeGroupTemplateField).setDefaultWidth(350);
		privilegeGroupTable.addColumn("privileges", getLocalized("accessControl.privileges"), privilegeTagComboBox).setDefaultWidth(1_000);
		privilegeGroupTable.setPropertyExtractor((record, propertyName) -> switch (propertyName) {
			case "group" -> record;
			case "privileges" -> record.getPrivileges();
			default -> null;
		});

		FormPanel formPanel = new FormPanel(getApplicationInstanceData());
		formPanel.setTable(privilegeGroupTable, true, false, false);

		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(450);
		formLayout.addSection().setCollapsible(false).setDrawHeaderLine(false);
		formLayout.addLabelAndField(null, getLocalized("roles.role"), roleComboBox);
		formLayout.addLabelAndField(null, getLocalized("applications.application"), applicationComboBox);
		formLayout.addLabelAndField(null, getLocalized("accessControl.applicationRole"), applicationRoleComboBox);
		if (!isOrgFieldFilterApplied()) {
			formLayout.addLabelAndField(null, getLocalized("accessControl.organizationFieldFilter"), organizationFieldFilterComboBox);
		}
		formLayout.addLabelAndField(null, getLocalized("accessControl.customOrganizationUnit"), customOrganizationUnit);
		formLayout.addLabelAndField(null, getLocalized("accessControl.organizationUnitTypeFilter"), organizationUnitTypeFilterTagComboBox);
		formLayout.addLabelAndField(null, getLocalized("accessControl.noInheritance"), noOrgUnitInheritanceCheckBox);

		formLayout.addSection(ApplicationIcons.SECURITY_BADGE, getLocalized(Dictionary.PRIVILEGES));
		formLayout.addLabelAndComponent(new Label(getLocalized("accessControl.applicationRolePrivileges")));
		formLayout.addLabelAndComponent(formPanel.getPanel());

		applicationComboBox.onValueChanged.addListener(() -> applicationRoleComboBox.setValue(null));
		applicationRoleComboBox.onValueChanged.addListener(role -> appRoleModelBuilder.setRecords(role.getPrivilegeGroups()));

		masterDetailController.createViews(getPerspective(), table, formLayout);

		formController.addNotNull(roleComboBox);
		formController.addNotNull(applicationComboBox);
		formController.addNotNull(applicationRoleComboBox);
		formController.setSaveEntityHandler(roleApplicationRoleAssignment -> {
			OrganizationField organizationField = isAppFilter() ? getOrganizationField() : organizationFieldFilterComboBox.getValue();
			roleApplicationRoleAssignment
					.setRole(roleComboBox.getValue())
					.setApplication(applicationComboBox.getValue())
					.setApplicationRoleName(applicationRoleComboBox.getValue().getName())
					.setOrganizationFieldFilter(organizationField)
					.setFixedOrganizationRoot(OrganizationUtils.convert(customOrganizationUnit.getValue()))
					.setOrganizationUnitTypeFilter(organizationUnitTypeFilterTagComboBox.getValue());
			return true;
		});

		entityModelBuilder.getOnSelectionEvent().addListener(roleApplicationRoleAssignment -> {
			roleComboBox.setValue(roleApplicationRoleAssignment.getRole());
			applicationComboBox.setValue(roleApplicationRoleAssignment.getApplication());
			ApplicationRole applicationRole = getApplicationRole(roleApplicationRoleAssignment);
			applicationRoleComboBox.setValue(applicationRole);
			organizationFieldFilterComboBox.setValue(roleApplicationRoleAssignment.getOrganizationFieldFilter());
			customOrganizationUnit.setValue(OrganizationUtils.convert(roleApplicationRoleAssignment.getFixedOrganizationRoot()));
			organizationUnitTypeFilterTagComboBox.setValue(roleApplicationRoleAssignment.getOrganizationUnitTypeFilter());
			appRoleModelBuilder.setRecords(applicationRole != null ? applicationRole.getPrivilegeGroups() : Collections.emptyList());
		});

		entityModelBuilder.setSelectedRecord(RoleApplicationRoleAssignment.create());
	}

	private ComboBox<OrganizationField> createOrganizationFieldComboBox() {
		return ComboBoxUtils.createRecordComboBox(
				() -> isAppFilter() ? Collections.singletonList(getOrganizationField()) : OrganizationField.getAll(),
				PropertyProviders.createOrganizationFieldPropertyProvider(getApplicationInstanceData()),
				BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE
		);
	}

	private ComboBox<ApplicationRole> createApplicationRoleComboBox(ComboBox<Application> applicationComboBox) {
		ComboBox<ApplicationRole> comboBox = new ComboBox<>(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		Supplier<List<ApplicationRole>> recordsSupplier = () -> {
			LoadedApplication loadedApplication = userSessionData.getRegistry().getLoadedApplication(applicationComboBox.getValue());
			if (loadedApplication != null) {
				List<ApplicationRole> applicationRoles = loadedApplication.getBaseApplicationBuilder().getApplicationRoles();
				return applicationRoles != null ? applicationRoles : Collections.emptyList();
			} else {
				return Collections.emptyList();
			}
		};
		PropertyProvider<ApplicationRole> propertyProvider = (applicationRole, propertyNames) -> {
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(applicationComboBox.getValue());
			Map<String, Object> map = new HashMap<>();
			if (localizationProvider == null) {
				return map;
			}
			map.put(BaseTemplate.PROPERTY_ICON, applicationRole.getIcon());
			map.put(BaseTemplate.PROPERTY_CAPTION, localizationProvider.getLocalized(applicationRole.getTitleKey()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, localizationProvider.getLocalized(applicationRole.getDescriptionKey()));
			return map;

		};
		comboBox.setPropertyProvider(propertyProvider);
		Function<ApplicationRole, String> recordToStringFunction = UiUtils.createRecordToStringFunction(propertyProvider);
		comboBox.setRecordToStringFunction(recordToStringFunction);
		comboBox.setModel(query -> query == null || query.isBlank() ? recordsSupplier.get() : recordsSupplier.get().stream().filter(record -> recordToStringFunction.apply(record).toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList()));

		return comboBox;
	}

	private TemplateField<RoleApplicationRoleAssignment> createApplicationRoleTemplateField() {
		TemplateField<RoleApplicationRoleAssignment> templateField = new TemplateField<>(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		PropertyProvider<RoleApplicationRoleAssignment> propertyProvider = (assignment, propertyNames) -> {
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(assignment.getApplication());
			Map<String, Object> map = new HashMap<>();
			if (localizationProvider == null || assignment.getApplicationRoleName() == null) {
				return map;
			}
			ApplicationRole applicationRole = getApplicationRole(assignment);
			if (applicationRole == null) {
				return map;
			}
			map.put(BaseTemplate.PROPERTY_ICON, applicationRole.getIcon());
			map.put(BaseTemplate.PROPERTY_CAPTION, localizationProvider.getLocalized(applicationRole.getTitleKey()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, localizationProvider.getLocalized(applicationRole.getDescriptionKey()));
			return map;
		};
		templateField.setPropertyProvider(propertyProvider);
		return templateField;
	}

	private ApplicationRole getApplicationRole(RoleApplicationRoleAssignment assignment) {
		if (assignment == null || assignment.getApplication() == null || assignment.getApplicationRoleName() == null) {
			return null;
		}
		LoadedApplication loadedApplication = userSessionData.getRegistry().getLoadedApplication(assignment.getApplication());
		if (loadedApplication != null) {
			return loadedApplication.getBaseApplicationBuilder().getApplicationRoles().stream().filter(appRole -> assignment.getApplicationRoleName().equals(appRole.getName())).findAny().orElse(null);
		} else {
			return null;
		}
	}

	private ComboBox<Application> createApplicationComboBox() {
		ComboBox<Application> comboBox = new ComboBox<>(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES);
		PropertyProvider<Application> propertyProvider = PropertyProviders.createApplicationPropertyProvider(userSessionData);
		Function<Application, String> recordToString = UiUtils.createRecordToStringFunction(propertyProvider);
		ComboBoxModel<Application> model = ComboBoxUtils.createComboBoxModel(this::getAvailableApplications, propertyProvider, 50);
		comboBox.setModel(model);
		comboBox.setPropertyProvider(propertyProvider);
		comboBox.setRecordToStringFunction(recordToString);
		return comboBox;
	}

	private List<Application> getAvailableApplications() {
		if (isAppFilter()) {
			return getManagedApplication().getPerspectives().stream()
					.map(p -> p.getApplicationPerspective().getApplication())
					.distinct()
					.collect(Collectors.toList());
		} else {
			return Application.getAll();
		}
	}

}

