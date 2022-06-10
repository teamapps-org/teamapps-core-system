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
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.privilege.PrivilegeGroup;
import org.teamapps.application.api.privilege.PrivilegeObject;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.api.ui.FormMetaFields;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.system.utils.ValueConverterUtils;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.combo.ComboBoxUtils;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.application.ux.form.FormController;
import org.teamapps.application.ux.view.MasterDetailController;
import org.teamapps.common.format.Color;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.databinding.MutableValue;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.universaldb.pojo.Query;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.field.AbstractField;
import org.teamapps.ux.component.field.CheckBox;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.field.combobox.TagBoxWrappingMode;
import org.teamapps.ux.component.field.combobox.TagComboBox;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.table.TableColumn;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;
import org.teamapps.ux.model.ComboBoxModel;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AccessControlPerspective extends AbstractManagedApplicationPerspective {

	private final PerspectiveSessionData perspectiveSessionData;
	private final UserSessionData userSessionData;


	public AccessControlPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		Supplier<Query<RolePrivilegeAssignment>> querySupplier = () -> isAppFilter() ? RolePrivilegeAssignment.filter().organizationFieldFilter(NumericFilter.equalsFilter(getOrganizationField().getId())) : RolePrivilegeAssignment.filter();
		MasterDetailController<RolePrivilegeAssignment> masterDetailController = new MasterDetailController<>(ApplicationIcons.KEYS, getLocalized("accessControl.accessControlListEntries"), getApplicationInstanceData(), querySupplier, Privileges.ACCESS_CONTROL_PERSPECTIVE);
		EntityModelBuilder<RolePrivilegeAssignment> entityModelBuilder = masterDetailController.getEntityModelBuilder();
		FormController<RolePrivilegeAssignment> formController = masterDetailController.getFormController();
		ResponsiveForm<RolePrivilegeAssignment> form = masterDetailController.getResponsiveForm();

		Table<RolePrivilegeAssignment> table = entityModelBuilder.createTable();
		table.setDisplayAsList(true);
		table.setRowHeight(28);
		table.setStripedRows(false);
		entityModelBuilder.updateModels();


		TemplateField<Role> roleTableField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.createRolePropertyProvider(getApplicationInstanceData()));
		TemplateField<Application> applicationTableField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.createApplicationPropertyProvider(userSessionData));
		TemplateField<ApplicationPrivilegeGroup> applicationPrivilegeGroupTableField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.createApplicationPrivilegeGroupPropertyProvider(userSessionData));
		TagComboBox<ApplicationPrivilege> applicationPrivilegesTableField = UiUtils.createTagComboBox(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createApplicationPrivilegePropertyProvider(userSessionData));
		TemplateField<OrganizationUnit> customOrganizationUnitTableField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.creatOrganizationUnitPropertyProvider(getApplicationInstanceData()));

		table.addColumn(new TableColumn<RolePrivilegeAssignment, Role>(RolePrivilegeAssignment.FIELD_ROLE, getLocalized("roles.role"), roleTableField).setDefaultWidth(200));
		table.addColumn(new TableColumn<RolePrivilegeAssignment, Application>(RolePrivilegeAssignment.FIELD_APPLICATION, getLocalized("applications.application"), applicationTableField).setDefaultWidth(200));
		table.addColumn(new TableColumn<RolePrivilegeAssignment, ApplicationPrivilegeGroup>(RolePrivilegeAssignment.FIELD_PRIVILEGE_GROUP, getLocalized("accessControl.privilegeGroup"), applicationPrivilegeGroupTableField).setDefaultWidth(200));
		table.addColumn(new TableColumn<RolePrivilegeAssignment, List<ApplicationPrivilege>>(RolePrivilegeAssignment.FIELD_PRIVILEGES, getLocalized("accessControl.privileges"), applicationPrivilegesTableField).setDefaultWidth(350));
		table.addColumn(new TableColumn<RolePrivilegeAssignment, OrganizationUnit>(RolePrivilegeAssignment.FIELD_FIXED_ORGANIZATION_ROOT, getLocalized("accessControl.customOrganizationUnit"), customOrganizationUnitTableField).setDefaultWidth(200));

		table.setPropertyExtractor((rolePrivilegeAssignment, propertyName) -> switch (propertyName) {
			case RolePrivilegeAssignment.FIELD_ROLE -> rolePrivilegeAssignment.getRole();
			case RolePrivilegeAssignment.FIELD_APPLICATION -> rolePrivilegeAssignment.getApplication();
			case RolePrivilegeAssignment.FIELD_PRIVILEGE_GROUP -> rolePrivilegeAssignment.getPrivilegeGroup();
			case RolePrivilegeAssignment.FIELD_PRIVILEGES -> rolePrivilegeAssignment.getPrivileges();
			case RolePrivilegeAssignment.FIELD_FIXED_ORGANIZATION_ROOT -> rolePrivilegeAssignment.getFixedOrganizationRoot();
			default -> null;
		});

		ComboBox<Role> roleComboBox = ComboBoxUtils.createRecordComboBox(
				() -> isAppFilter() ? Role.filter().organizationField(NumericFilter.equalsFilter(getOrganizationField().getId())).execute() : Role.getAll(),
				PropertyProviders.createRolePropertyProvider(getApplicationInstanceData()),
				BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES
		);

		ComboBox<Application> applicationComboBox = createApplicationComboBox();
		ComboBox<ApplicationPrivilegeGroup> privilegeGroupComboBox = createPrivilegeGroupComboBox(applicationComboBox);
		TagComboBox<ApplicationPrivilege> privilegesTagComboBox = createPrivilegeTagComboBox(privilegeGroupComboBox);
		TagComboBox<PrivilegeObject> privilegeObjectTagComboBox = createPrivilegeObjectTagComboBox(applicationComboBox, privilegeGroupComboBox);
		CheckBox privilegeObjectInheritanceCheckBox = new CheckBox(getLocalized("accessControl.privilegeObjectInheritance"));
		ComboBox<OrganizationField> organizationFieldFilterComboBox = createOrganizationFieldComboBox();
		AbstractField<OrganizationUnitView> customOrganizationUnit = formController.getOrganizationUnitViewField(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, true);
		TagComboBox<OrganizationUnitType> organizationUnitTypeFilterTagComboBox = OrganizationUtils.createOrganizationUnitTypeTagComboBox(50, getApplicationInstanceData());
		CheckBox noOrgUnitInheritanceCheckBox = new CheckBox(getLocalized("accessControl.noInheritanceOfOrganizationalUnits"));


		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(450);
		formLayout.addSection().setCollapsible(false).setDrawHeaderLine(false);
		formLayout.addLabelAndField(null, getLocalized("roles.role"), roleComboBox);
		formLayout.addLabelAndField(null, getLocalized("applications.application"), applicationComboBox);
		formLayout.addLabelAndField(null, getLocalized("accessControl.privilegeGroup"), privilegeGroupComboBox);
		formLayout.addLabelAndField(null, getLocalized("accessControl.privileges"), privilegesTagComboBox);
		formLayout.addLabelAndField(null, getLocalized("accessControl.privilegeObjects"), privilegeObjectTagComboBox);
		formLayout.addLabelAndField(null, getLocalized("accessControl.privilegeObjectInheritance"), privilegeObjectInheritanceCheckBox);
		if (!isOrgFieldFilterApplied()) {
			formLayout.addLabelAndField(null, getLocalized("accessControl.organizationFieldFilter"), organizationFieldFilterComboBox);
		}
		formLayout.addLabelAndField(null, getLocalized("accessControl.customOrganizationUnit"), customOrganizationUnit);
		formLayout.addLabelAndField(null, getLocalized("accessControl.organizationUnitTypeFilter"), organizationUnitTypeFilterTagComboBox);
		formLayout.addLabelAndField(null, getLocalized("accessControl.noInheritance"), noOrgUnitInheritanceCheckBox);

		Consumer<ApplicationPrivilegeGroup> privilegeGroupHandler = privilegeGroup -> {
			if (privilegeGroup == null) {
				privilegesTagComboBox.setVisible(false);
				privilegeObjectTagComboBox.setVisible(false);
				privilegeObjectInheritanceCheckBox.setVisible(false);
				customOrganizationUnit.setVisible(false);
				organizationUnitTypeFilterTagComboBox.setVisible(false);
				noOrgUnitInheritanceCheckBox.setVisible(false);
				return;
			}
			switch (privilegeGroup.getApplicationPrivilegeGroupType()) {
				case SIMPLE_PRIVILEGE -> {
					privilegesTagComboBox.setVisible(false);
					privilegeObjectTagComboBox.setVisible(false);
					privilegeObjectInheritanceCheckBox.setVisible(false);
					customOrganizationUnit.setVisible(false);
					organizationUnitTypeFilterTagComboBox.setVisible(false);
					noOrgUnitInheritanceCheckBox.setVisible(false);
				}
				case SIMPLE_ORGANIZATIONAL_PRIVILEGE -> {
					privilegesTagComboBox.setVisible(false);
					privilegeObjectTagComboBox.setVisible(false);
					privilegeObjectInheritanceCheckBox.setVisible(false);
					customOrganizationUnit.setVisible(true);
					organizationUnitTypeFilterTagComboBox.setVisible(true);
					noOrgUnitInheritanceCheckBox.setVisible(true);
				}
				case SIMPLE_CUSTOM_OBJECT_PRIVILEGE -> {
					privilegesTagComboBox.setVisible(false);
					privilegeObjectTagComboBox.setVisible(true);
					privilegeObjectInheritanceCheckBox.setVisible(true);
					customOrganizationUnit.setVisible(false);
					organizationUnitTypeFilterTagComboBox.setVisible(false);
					noOrgUnitInheritanceCheckBox.setVisible(false);
				}
				case STANDARD_PRIVILEGE_GROUP -> {
					privilegesTagComboBox.setVisible(true);
					privilegeObjectTagComboBox.setVisible(false);
					privilegeObjectInheritanceCheckBox.setVisible(false);
					customOrganizationUnit.setVisible(false);
					organizationUnitTypeFilterTagComboBox.setVisible(false);
					noOrgUnitInheritanceCheckBox.setVisible(false);
				}
				case ORGANIZATIONAL_PRIVILEGE_GROUP -> {
					privilegesTagComboBox.setVisible(true);
					privilegeObjectTagComboBox.setVisible(false);
					privilegeObjectInheritanceCheckBox.setVisible(false);
					customOrganizationUnit.setVisible(true);
					organizationUnitTypeFilterTagComboBox.setVisible(true);
					noOrgUnitInheritanceCheckBox.setVisible(true);
				}
				case CUSTOM_OBJECT_PRIVILEGE_GROUP -> {
					privilegesTagComboBox.setVisible(true);
					privilegeObjectTagComboBox.setVisible(true);
					privilegeObjectInheritanceCheckBox.setVisible(true);
					customOrganizationUnit.setVisible(false);
					organizationUnitTypeFilterTagComboBox.setVisible(false);
					noOrgUnitInheritanceCheckBox.setVisible(false);
				}
				case ROLE_ASSIGNMENT_DELEGATED_CUSTOM_PRIVILEGE_GROUP -> {
					privilegesTagComboBox.setVisible(true);
					privilegeObjectTagComboBox.setVisible(false);
					privilegeObjectInheritanceCheckBox.setVisible(false);
					customOrganizationUnit.setVisible(false);
					organizationUnitTypeFilterTagComboBox.setVisible(false);
					noOrgUnitInheritanceCheckBox.setVisible(false);
				}
			}
		};

		applicationComboBox.onValueChanged.addListener(() -> {
			privilegeGroupComboBox.setValue(null);
			privilegesTagComboBox.setValue(null);
			privilegeObjectTagComboBox.setValue(null);
		});

		privilegeGroupComboBox.onValueChanged.addListener(privilegeGroup -> {
			privilegesTagComboBox.setValue(null);
			privilegeObjectTagComboBox.setValue(null);
			privilegeGroupHandler.accept(privilegeGroup);
		});




		masterDetailController.createViews(getPerspective(), table, formLayout);

		formController.addNotNull(roleComboBox);
		formController.addNotNull(applicationComboBox);
		formController.addNotNull(privilegeGroupComboBox);
		formController.addNotNull(roleComboBox);


		formController.setSaveEntityHandler(rolePrivilegeAssignment -> {
			OrganizationField organizationField = isOrgFieldFilterApplied() ? getOrganizationField() : organizationFieldFilterComboBox.getValue();
			rolePrivilegeAssignment
					.setRole(roleComboBox.getValue())
					.setApplication(applicationComboBox.getValue())
					.setPrivilegeGroup(privilegeGroupComboBox.getValue())
					.setPrivileges(privilegesTagComboBox.getValue())
					.setPrivilegeObjects(privilegeObjectTagComboBox.getValue() != null ? ValueConverterUtils.compressStringList(privilegeObjectTagComboBox.getValue().stream().map(p -> "" + p.getId()).collect(Collectors.toList())) : null)
					.setPrivilegeObjectInheritance(privilegeObjectInheritanceCheckBox.getValue())
					.setOrganizationFieldFilter(organizationField)
					.setFixedOrganizationRoot(OrganizationUtils.convert(customOrganizationUnit.getValue()))
					.setOrganizationUnitTypeFilter(organizationUnitTypeFilterTagComboBox.getValue())
					.setNoInheritanceOfOrganizationalUnits(noOrgUnitInheritanceCheckBox.getValue());
			return true;
		});

		entityModelBuilder.getOnSelectionEvent().addListener(rolePrivilegeAssignment -> {
			ApplicationPrivilegeGroup privilegeGroup = rolePrivilegeAssignment.getPrivilegeGroup();
			privilegeGroupHandler.accept(privilegeGroup);
			roleComboBox.setValue(rolePrivilegeAssignment.getRole());
			applicationComboBox.setValue(rolePrivilegeAssignment.getApplication());
			privilegeGroupComboBox.setValue(privilegeGroup);
			privilegesTagComboBox.setValue(rolePrivilegeAssignment.getPrivileges());
			//privilegeObjectTagComboBox.setValue(rolePrivilegeAssignment); //todo!
			privilegeObjectInheritanceCheckBox.setValue(rolePrivilegeAssignment.getPrivilegeObjectInheritance());
			organizationFieldFilterComboBox.setValue(rolePrivilegeAssignment.getOrganizationFieldFilter());
			customOrganizationUnit.setValue(OrganizationUtils.convert(rolePrivilegeAssignment.getFixedOrganizationRoot()));
			organizationUnitTypeFilterTagComboBox.setValue(rolePrivilegeAssignment.getOrganizationUnitTypeFilter());
			noOrgUnitInheritanceCheckBox.setValue(rolePrivilegeAssignment.isNoInheritanceOfOrganizationalUnits());
		});

		entityModelBuilder.setSelectedRecord(RolePrivilegeAssignment.create());
	}

	private ComboBox<OrganizationField> createOrganizationFieldComboBox() {
		return ComboBoxUtils.createRecordComboBox(
				() -> isAppFilter() ? Collections.singletonList(getOrganizationField()) : OrganizationField.getAll(),
				PropertyProviders.createOrganizationFieldPropertyProvider(getApplicationInstanceData()),
				BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE
		);
	}

	private TagComboBox<PrivilegeObject> createPrivilegeObjectTagComboBox(ComboBox<Application> applicationComboBox, ComboBox<ApplicationPrivilegeGroup> privilegeGroupComboBox) {
		TagComboBox<PrivilegeObject> tagComboBox = new TagComboBox<>(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES);
		PropertyProvider<PrivilegeObject> propertyProvider = PropertyProviders.createPrivilegeObjectPropertyProvider();
		Function<PrivilegeObject, String> recordToStringFunction = UiUtils.createRecordToStringFunction(propertyProvider);
		ApplicationPrivilegeGroup privilegeGroup = privilegeGroupComboBox.getValue();
		Application application = applicationComboBox.getValue();
		LoadedApplication loadedApplication = userSessionData.getRegistry().getLoadedApplication(application);
		ComboBoxModel<PrivilegeObject> comboBoxModel = ComboBoxUtils.createComboBoxModel(() -> getPrivilegeObjects(privilegeGroup, loadedApplication), propertyProvider, 50);
		tagComboBox.setModel(comboBoxModel);
		tagComboBox.setPropertyProvider(propertyProvider);
		tagComboBox.setRecordToStringFunction(recordToStringFunction);
		tagComboBox.setDistinct(true);
		tagComboBox.setWrappingMode(TagBoxWrappingMode.SINGLE_TAG_PER_LINE);
		return tagComboBox;

	}

	private List<PrivilegeObject> getPrivilegeObjects(ApplicationPrivilegeGroup privilegeGroup, LoadedApplication loadedApplication) {
		if (privilegeGroup == null || loadedApplication == null) {
			return Collections.emptyList();
		}
		PrivilegeGroup group = loadedApplication.getBaseApplicationBuilder().getPrivilegeGroups()
				.stream()
				.filter(g -> g.getName().equals(privilegeGroup.getName()))
				.findAny()
				.orElse(null);
		if (group != null) {
			return group.getPrivilegeObjectsSupplier().get();
		}
		return Collections.emptyList();
	}

	private TagComboBox<ApplicationPrivilege> createPrivilegeTagComboBox(ComboBox<ApplicationPrivilegeGroup> privilegeGroupComboBox) {
		TagComboBox<ApplicationPrivilege> tagComboBox = new TagComboBox<>(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		PropertyProvider<ApplicationPrivilege> propertyProvider = PropertyProviders.createApplicationPrivilegePropertyProvider(userSessionData);
		Function<ApplicationPrivilege, String> recordToString = UiUtils.createRecordToStringFunction(propertyProvider);
		ComboBoxModel<ApplicationPrivilege> comboBoxModel = ComboBoxUtils.createComboBoxModel(() -> privilegeGroupComboBox.getValue() != null ? privilegeGroupComboBox.getValue().getPrivileges() : null, propertyProvider, 50);
		tagComboBox.setModel(comboBoxModel);
		tagComboBox.setPropertyProvider(propertyProvider);
		tagComboBox.setRecordToStringFunction(recordToString);
		tagComboBox.setDistinct(true);
		tagComboBox.setWrappingMode(TagBoxWrappingMode.SINGLE_TAG_PER_LINE);
		tagComboBox.setShowClearButton(true);
		return tagComboBox;
	}

	private ComboBox<ApplicationPrivilegeGroup> createPrivilegeGroupComboBox(ComboBox<Application> applicationComboBox) {
		ComboBox<ApplicationPrivilegeGroup> comboBox = new ComboBox<>(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES);
		PropertyProvider<ApplicationPrivilegeGroup> propertyProvider = PropertyProviders.createApplicationPrivilegeGroupPropertyProvider(userSessionData);
		Function<ApplicationPrivilegeGroup, String> recordToString = UiUtils.createRecordToStringFunction(propertyProvider);
		ComboBoxModel<ApplicationPrivilegeGroup> comboBoxModel = ComboBoxUtils.createComboBoxModel(() -> applicationComboBox.getValue() != null ? applicationComboBox.getValue().getPrivilegeGroups() : Collections.emptyList(), propertyProvider, 50);
		comboBox.setModel(comboBoxModel);
		comboBox.setPropertyProvider(propertyProvider);
		comboBox.setRecordToStringFunction(recordToString);
		return comboBox;
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

