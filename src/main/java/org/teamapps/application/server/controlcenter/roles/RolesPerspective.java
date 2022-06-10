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
package org.teamapps.application.server.controlcenter.roles;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.privilege.MergedApplicationPrivileges;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.system.utils.RoleUtils;
import org.teamapps.application.tools.EntityListModelBuilder;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.combo.ComboBoxUtils;
import org.teamapps.application.ux.form.FormController;
import org.teamapps.application.ux.form.FormPanel;
import org.teamapps.application.ux.localize.TranslatableField;
import org.teamapps.application.ux.localize.TranslatableTextUtils;
import org.teamapps.application.ux.view.MasterDetailController;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.databinding.MutableValue;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.universaldb.pojo.Query;
import org.teamapps.ux.component.absolutelayout.Length;
import org.teamapps.ux.component.field.CheckBox;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.field.combobox.TagBoxWrappingMode;
import org.teamapps.ux.component.field.combobox.TagComboBox;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.panel.Panel;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.table.TableColumn;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.template.BaseTemplateTreeNode;
import org.teamapps.ux.component.tree.SimpleTree;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RolesPerspective extends AbstractManagedApplicationPerspective {


	private final PerspectiveSessionData perspectiveSessionData;
	private final UserSessionData userSessionData;

	public RolesPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		Supplier<Query<Role>> querySupplier = () -> isAppFilter() ? Role.filter().organizationField(NumericFilter.equalsFilter(getOrganizationField().getId())) : Role.filter();
		MasterDetailController<Role> masterDetailController = new MasterDetailController<>(ApplicationIcons.WORKER, getLocalized("roles.roles"), getApplicationInstanceData(), querySupplier, Privileges.ROLES_PERSPECTIVE);
		EntityModelBuilder<Role> entityModelBuilder = masterDetailController.getEntityModelBuilder();
		FormController<Role> formController = masterDetailController.getFormController();
		ResponsiveForm<Role> form = masterDetailController.getResponsiveForm();

		Table<Role> table = entityModelBuilder.createTable();
		table.setDisplayAsList(true);
		table.setRowHeight(28);
		table.setStripedRows(false);
		entityModelBuilder.updateModels();

		TemplateField<Role> roleTableField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createRolePropertyProvider(getApplicationInstanceData()));
		TemplateField<OrganizationField> organizationFieldTableField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createOrganizationFieldPropertyProvider(getApplicationInstanceData()));
		TagComboBox<OrganizationUnitType> allowedOrganizationUnitTypesTableField = UiUtils.createTagComboBox(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.creatOrganizationUnitTypePropertyProvider(getApplicationInstanceData()));
		CheckBox noDirectMembershipsTableField = new CheckBox(getLocalized("roles.noDirectMemberships"));

		table.addColumn(Role.FIELD_TITLE, getLocalized(Dictionary.NAME), roleTableField).setDefaultWidth(200);
		if (!isAppFilter()) {
			table.addColumn(Role.FIELD_ORGANIZATION_FIELD, getLocalized("organizationField.organizationField"), organizationFieldTableField).setDefaultWidth(130);
		}
		table.addColumn(Role.FIELD_ALLOWED_ORGANIZATION_UNIT_TYPES, getLocalized("roles.allowedOrganizationUnitTypes"), allowedOrganizationUnitTypesTableField).setDefaultWidth(350);
		table.addColumn(Role.FIELD_NO_DIRECT_MEMBERSHIPS, getLocalized("roles.noMemberships"), noDirectMembershipsTableField).setDefaultWidth(200);

		table.setPropertyExtractor((role, propertyName) -> switch (propertyName) {
			case Role.FIELD_TITLE -> role;
			case Role.FIELD_ORGANIZATION_FIELD -> role.getOrganizationField();
			case Role.FIELD_ALLOWED_ORGANIZATION_UNIT_TYPES -> role.getAllowedOrganizationUnitTypes();
			case Role.FIELD_NO_DIRECT_MEMBERSHIPS -> role.getNoDirectMemberships();
			default -> null;
		});

		TranslatableField titleField = TranslatableTextUtils.createTranslatableField(getApplicationInstanceData());
		ComboBox<Icon> iconComboBox = ApplicationIcons.createIconComboBox();
		ComboBox<RoleType> roleTypeComboBox = ComboBoxUtils.createRecordComboBox(Arrays.asList(RoleType.values()), (roleType, collection) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, ApplicationIcons.WORKER);
			map.put(BaseTemplate.PROPERTY_CAPTION, roleType.name());
			return map;
		}, BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		ComboBox<Role> parentRoleComboBox = ComboBoxUtils.createRecordComboBox(
				() -> isAppFilter() ? Role.filter().organizationField(NumericFilter.equalsFilter(getOrganizationField().getId())).execute() : Role.getAll(),
				PropertyProviders.createRolePropertyProvider(getApplicationInstanceData()),
				BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE
		);
		TagComboBox<OrganizationUnitType> allowedOrganizationUnitTypesTagCombo = createOrgUnitTypeTagComboBox();
		ComboBox<OrganizationField> organizationFieldComboBox = ComboBoxUtils.createRecordComboBox(
				() -> isAppFilter() ? Collections.singletonList(getOrganizationField()) : OrganizationField.getAll(),
				PropertyProviders.createOrganizationFieldPropertyProvider(getApplicationInstanceData()),
				BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE
		);
		TagComboBox<Role> generalizationRolesTagCombo = createRoleTagComboBox();
		TagComboBox<Role> specializationRolesTagCombo = createRoleTagComboBox();
		TagComboBox<Role> privilegesReceivingRolesTagCombo = createRoleTagComboBox();
		TagComboBox<Role> privilegesSendingRolesTagCombo = createRoleTagComboBox();
		CheckBox noDirectMembershipsCheckBox = new CheckBox(getLocalized("roles.noDirectMemberships"));
		CheckBox customPrivilegeRoleCheckBox = new CheckBox(getLocalized("roles.customPrivilegeRole"));

		EntityListModelBuilder<UserRoleAssignment> userRoleAssignmentModelBuilder = new EntityListModelBuilder<>(getApplicationInstanceData(), userRoleAssignment -> userRoleAssignment.getUser().getFirstName() + " " + userRoleAssignment.getUser().getLastName());
		Table<UserRoleAssignment> roleMemberTable = userRoleAssignmentModelBuilder.createListTable(true);
		roleMemberTable.setHideHeaders(true);

		TemplateField<User> userTemplateField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.createUserPropertyProvider(getApplicationInstanceData()));
		TemplateField<OrganizationUnit> organizationUnitTemplateField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.creatOrganizationUnitPropertyProvider(getApplicationInstanceData()));
		roleMemberTable.addColumn(new TableColumn<>("user", userTemplateField));
		roleMemberTable.addColumn(new TableColumn<>("orgUnit", organizationUnitTemplateField));
		roleMemberTable.setPropertyExtractor((userRoleAssignment, propertyName) -> switch (propertyName) {
			case "user" -> userRoleAssignment.getUser();
			case "orgUnit" -> userRoleAssignment.getOrganizationUnit();
			default -> null;
		});

		FormPanel roleMembersPanel = new FormPanel(getApplicationInstanceData());
		roleMembersPanel.setTable(roleMemberTable, userRoleAssignmentModelBuilder, ApplicationIcons.USERS, getLocalized("users.users"), true, false, false);


		SimpleTree<Object> privilegesTree = new SimpleTree<>();
		privilegesTree.setTemplatesByDepth(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, BaseTemplate.LIST_ITEM_MEDIUM_ICON_TWO_LINES, BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		Panel privilegesPanel = new Panel();
		privilegesPanel.setIcon(ApplicationIcons.SECURITY_BADGE);
		privilegesPanel.setTitle(getLocalized(Dictionary.PRIVILEGES));
		privilegesPanel.setCssStyle("height", Length.ofPixels(300).toCssString());
		privilegesPanel.setContent(privilegesTree);

		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(450);
		formLayout.addSection(ApplicationIcons.WORKER, getLocalized("roles.role"));

		formLayout.addLabelAndField(null, getLocalized("roles.role"), titleField);
		formLayout.addLabelAndField(null, getLocalized("roles.icon"), iconComboBox);
		formLayout.addLabelAndField(null, getLocalized("roles.type"), roleTypeComboBox);

		formLayout.addLabelAndField(null, getLocalized("roles.parentRole"), parentRoleComboBox);
		formLayout.addLabelAndField(null, getLocalized("roles.allowedOrganizationUnitTypes"), allowedOrganizationUnitTypesTagCombo);
		if (!isOrgFieldFilterApplied()) {
			formLayout.addLabelAndField(null, getLocalized("roles.organizationField"), organizationFieldComboBox);
		}
		formLayout.addLabelAndField(null, getLocalized("roles.generalizationRoles"), generalizationRolesTagCombo);
		formLayout.addLabelAndField(null, getLocalized("roles.specializationRoles"), specializationRolesTagCombo);
		formLayout.addLabelAndField(null, getLocalized("roles.privilegesReceivingRoles"), privilegesReceivingRolesTagCombo);
		formLayout.addLabelAndField(null, getLocalized("roles.privilegesSendingRoles"), privilegesSendingRolesTagCombo);
		formLayout.addLabelAndField(null, getLocalized("roles.noMemberships"), noDirectMembershipsCheckBox);
		formLayout.addLabelAndField(null, getLocalized("roles.customPrivilegeRole"), customPrivilegeRoleCheckBox);

		formLayout.addSection(ApplicationIcons.USERS_CROWD, getLocalized("roles.members")).setCollapsed(true);
		formLayout.addLabelAndComponent(roleMembersPanel.getPanel());

		formLayout.addSection(ApplicationIcons.SECURITY_BADGE, getLocalized(Dictionary.PRIVILEGES)).setCollapsed(true);
		formLayout.addLabelAndComponent(privilegesPanel);

		masterDetailController.createViews(getPerspective(), table, formLayout);

		formController.addNotNull(titleField);
		formController.addNotNull(iconComboBox);
		formController.setSaveEntityHandler(role -> {
			OrganizationField organizationField = isOrgFieldFilterApplied() ? getOrganizationField() : organizationFieldComboBox.getValue();
			role
					.setTitle(titleField.getValue())
					.setIcon(IconUtils.encodeNoStyle(iconComboBox.getValue()))
					.setRoleType(roleTypeComboBox.getValue())
					.setParent(parentRoleComboBox.getValue())
					.setAllowedOrganizationUnitTypes(allowedOrganizationUnitTypesTagCombo.getValue())
					.setOrganizationField(organizationField)
					.setGeneralizationRoles(generalizationRolesTagCombo.getValue())
					.setSpecializationRoles(specializationRolesTagCombo.getValue())
					.setPrivilegesReceivingRoles(privilegesReceivingRolesTagCombo.getValue())
					.setPrivilegesSendingRoles(privilegesSendingRolesTagCombo.getValue())
					.setNoDirectMemberships(noDirectMembershipsCheckBox.getValue())
					.setDelegatedCustomPrivilegeObjectRole(customPrivilegeRoleCheckBox.getValue());
			return true;
		});

		entityModelBuilder.getOnSelectionEvent().addListener(role -> {
			titleField.setValue(role.getTitle());
			iconComboBox.setValue(IconUtils.decodeIcon(role.getIcon()));
			roleTypeComboBox.setValue(role.getRoleType());
			parentRoleComboBox.setValue(role.getParent());
			allowedOrganizationUnitTypesTagCombo.setValue(role.getAllowedOrganizationUnitTypes());
			organizationFieldComboBox.setValue(role.getOrganizationField());
			generalizationRolesTagCombo.setValue(role.getGeneralizationRoles());
			specializationRolesTagCombo.setValue(role.getSpecializationRoles());
			privilegesReceivingRolesTagCombo.setValue(role.getPrivilegesReceivingRoles());
			privilegesSendingRolesTagCombo.setValue(role.getPrivilegesSendingRoles());
			noDirectMembershipsCheckBox.setValue(role.getNoDirectMemberships());
			customPrivilegeRoleCheckBox.setValue(role.isDelegatedCustomPrivilegeObjectRole());
			userRoleAssignmentModelBuilder.setRecords(RoleUtils.getMembers(role, true));

			privilegesTree.removeAllNodes();
			List<MergedApplicationPrivileges> mergedApplicationPrivileges = RoleUtils.calcPrivileges(role, userSessionData);
			List<BaseTemplateTreeNode<Object>> treeNodes = mergedApplicationPrivileges.stream().flatMap(priv -> priv.getTreeRecords().stream()).collect(Collectors.toList());
			privilegesTree.getModel().setNodes(treeNodes);
		});
		entityModelBuilder.setSelectedRecord(Role.create());
	}

	private TagComboBox<Role> createRoleTagComboBox() {
		TagComboBox<Role> tagComboBox = new TagComboBox<>(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		tagComboBox.setModel(query ->
				query == null || query.isBlank() ?
						isAppFilter() ? Role.filter().organizationField(NumericFilter.equalsFilter(getOrganizationField().getId())).execute() : Role.getAll().stream().limit(50).collect(Collectors.toList()) :
						isAppFilter() ? Role.filter().parseFullTextFilter(query).organizationField(NumericFilter.equalsFilter(getOrganizationField().getId())).execute() : Role.filter().parseFullTextFilter(query).execute()
		);
		PropertyProvider<Role> propertyProvider = PropertyProviders.createRolePropertyProvider(getApplicationInstanceData());
		tagComboBox.setPropertyProvider(propertyProvider);
		tagComboBox.setRecordToStringFunction(unitType -> (String) propertyProvider.getValues(unitType, Collections.emptyList()).get(BaseTemplate.PROPERTY_CAPTION));
		tagComboBox.setWrappingMode(TagBoxWrappingMode.SINGLE_TAG_PER_LINE);
		tagComboBox.setDistinct(true);
		return tagComboBox;
	}

	private TagComboBox<OrganizationUnitType> createOrgUnitTypeTagComboBox() {
		TagComboBox<OrganizationUnitType> tagComboBox = new TagComboBox<>(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		tagComboBox.setModel(query -> query == null || query.isBlank() ?
				OrganizationUnitType.getAll().stream().limit(50).collect(Collectors.toList()) :
				OrganizationUnitType.filter().parseFullTextFilter(query).execute().stream().limit(50).collect(Collectors.toList())
		);
		PropertyProvider<OrganizationUnitType> propertyProvider = PropertyProviders.creatOrganizationUnitTypePropertyProvider(getApplicationInstanceData());
		tagComboBox.setPropertyProvider(propertyProvider);
		tagComboBox.setRecordToStringFunction(unitType -> (String) propertyProvider.getValues(unitType, Collections.emptyList()).get(BaseTemplate.PROPERTY_CAPTION));
		tagComboBox.setWrappingMode(TagBoxWrappingMode.SINGLE_TAG_PER_LINE);
		tagComboBox.setDistinct(true);
		return tagComboBox;

	}


}

