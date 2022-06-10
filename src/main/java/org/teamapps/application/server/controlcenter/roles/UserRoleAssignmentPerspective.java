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

import org.teamapps.application.api.application.ApplicationRoleAssignmentPrivilegeObjectProvider;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.privilege.PrivilegeObject;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.system.utils.RoleUtils;
import org.teamapps.application.tools.EntityListModelBuilder;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.combo.ComboBoxUtils;
import org.teamapps.application.ux.form.FormController;
import org.teamapps.application.ux.form.FormPanel;
import org.teamapps.application.ux.view.MasterDetailController;
import org.teamapps.common.format.Color;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.databinding.MutableValue;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.universaldb.pojo.Query;
import org.teamapps.ux.component.field.AbstractField;
import org.teamapps.ux.component.field.CheckBox;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.format.*;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.table.TableColumn;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.template.Template;
import org.teamapps.ux.component.template.gridtemplate.GridTemplate;
import org.teamapps.ux.component.template.gridtemplate.ImageElement;
import org.teamapps.ux.component.template.gridtemplate.TextElement;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UserRoleAssignmentPerspective extends AbstractManagedApplicationPerspective {

	private final UserSessionData userSessionData;


	public UserRoleAssignmentPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		PerspectiveSessionData perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		Supplier<Query<UserRoleAssignment>> querySupplier = () -> isAppFilter() ? UserRoleAssignment.filter().filterRole(Role.filter().organizationField(NumericFilter.equalsFilter(getOrganizationField().getId()))) : UserRoleAssignment.filter();
		MasterDetailController<UserRoleAssignment> masterDetailController = new MasterDetailController<>(ApplicationIcons.USERS_THREE_RELATION, getLocalized("userRoleAssignment.userRoleAssignments"), getApplicationInstanceData(), querySupplier, Privileges.USER_ROLE_ASSIGNMENT_PERSPECTIVE, UserRoleAssignment.FIELD_ORGANIZATION_UNIT);
		EntityModelBuilder<UserRoleAssignment> entityModelBuilder = masterDetailController.getEntityModelBuilder();
		FormController<UserRoleAssignment> formController = masterDetailController.getFormController();
		ResponsiveForm<UserRoleAssignment> form = masterDetailController.getResponsiveForm();

		Table<UserRoleAssignment> table = entityModelBuilder.createTable();
		table.setDisplayAsList(true);
		table.setRowHeight(64);
		table.setForceFitWidth(true);
		table.setHideHeaders(true);
		entityModelBuilder.updateModels();

		PropertyProvider<User> userPropertyProvider = PropertyProviders.createUserPropertyProvider(getApplicationInstanceData());
		PropertyProvider<Role> rolePropertyProvider = PropertyProviders.createRolePropertyProvider(getApplicationInstanceData());
		PropertyProvider<OrganizationUnit> organizationUnitPropertyProvider = PropertyProviders.creatOrganizationUnitPropertyProvider(getApplicationInstanceData());
		TemplateField<UserRoleAssignment> assignmentTemplateField = new TemplateField<>(createRoleAssignmentTemplate());
		assignmentTemplateField.setPropertyProvider((assignment, collection) -> {
			Map<String, Object> map = new HashMap<>();
			Map<String, Object> userValues = userPropertyProvider.getValues(assignment.getUser(), null);
			Map<String, Object> roleValues = rolePropertyProvider.getValues(assignment.getRole(), null);
			Map<String, Object> unitValues = organizationUnitPropertyProvider.getValues(assignment.getOrganizationUnit(), null);
			map.put("image", userValues.get(BaseTemplate.PROPERTY_IMAGE));
			map.put("line1", userValues.get(BaseTemplate.PROPERTY_CAPTION));
			map.put("line2", roleValues.get(BaseTemplate.PROPERTY_CAPTION));
			map.put("line3", unitValues.get(BaseTemplate.PROPERTY_CAPTION));
			return map;
		});
		table.addColumn("data", null, assignmentTemplateField).setValueExtractor(assignment -> assignment);

		entityModelBuilder.setCustomFieldSorter(fieldName -> {
			Comparator<String> comparator = getUser().getComparator(true);
			List<String> rankedLanguages = getUser().getRankedLanguages();
			return switch (fieldName) {
				case UserRoleAssignment.FIELD_USER -> (r1, r2) -> comparator.compare(r1.getUser().getLastName(), r2.getUser().getLastName());
				case UserRoleAssignment.FIELD_ROLE -> (r1, r2) -> comparator.compare(r1.getRole().getTitle().getText(rankedLanguages), r2.getRole().getTitle().getText(rankedLanguages));
				case UserRoleAssignment.FIELD_ORGANIZATION_UNIT -> (r1, r2) -> comparator.compare(r1.getOrganizationUnit().getName().getText(rankedLanguages), r2.getOrganizationUnit().getName().getText(rankedLanguages));
				default -> null;
			};
		});

		entityModelBuilder.setCustomFullTextFilter((r, query) -> {
			List<String> rankedLanguages = getUser().getRankedLanguages();
			return r.getUser() != null && r.getOrganizationUnit() != null && r.getRole() != null && (matches(r.getUser().getFirstName(), query) ||
					matches(r.getUser().getLastName(), query) ||
					matches(r.getRole().getTitle().getText(rankedLanguages), query) ||
					matches(r.getOrganizationUnit().getName().getText(rankedLanguages), query));
		});

		table.setPropertyExtractor((userRoleAssignment, propertyName) -> switch (propertyName) {
			case UserRoleAssignment.FIELD_USER -> userRoleAssignment.getUser();
			case UserRoleAssignment.FIELD_ROLE -> userRoleAssignment.getRole();
			case UserRoleAssignment.FIELD_ORGANIZATION_UNIT -> userRoleAssignment.getOrganizationUnit();
			default -> null;
		});

		ComboBox<User> userCombobox = ComboBoxUtils.createComboBox(query -> query == null || query.isBlank() ?
						User.getAll().stream().limit(50).collect(Collectors.toList()) :
						User.filter().parseFullTextFilter(query).execute().stream().limit(50).collect(Collectors.toList()),
				PropertyProviders.createUserPropertyProvider(getApplicationInstanceData()), BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES);
		ComboBox<Role> roleComboBox = ComboBoxUtils.createRecordComboBox(
				() -> isAppFilter() ? Role.filter().organizationField(NumericFilter.equalsFilter(getOrganizationField().getId())).execute() : Role.getAll(),
				PropertyProviders.createRolePropertyProvider(getApplicationInstanceData()),
				BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES
		);

		AbstractField<OrganizationUnitView> organizationComboBox = formController.getOrganizationUnitViewField(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, false);
		CheckBox mainResponsibleField = new CheckBox(getLocalized("userRoleAssignment.mainResponsible"));

		ComboBox<PrivilegeObject> delegatedPrivilegeObjectComboBox = ComboBoxUtils.createRecordComboBox(() -> {
			Role role = roleComboBox.getValue();
			if (role != null && role.isDelegatedCustomPrivilegeObjectRole() && organizationComboBox.getValue() != null) {
				ApplicationRoleAssignmentPrivilegeObjectProvider privilegeObjectProvider = getDelegatePrivilegeObjectProvider();
				return privilegeObjectProvider != null ? privilegeObjectProvider.getPrivilegeObjects(organizationComboBox.getValue()) : Collections.emptyList();
			}
			return Collections.emptyList();
		},  (privilegeObject, collection) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, privilegeObject.getIcon());
			map.put(BaseTemplate.PROPERTY_CAPTION, getLocalized(privilegeObject.getTitleKey()));
			return map;
		}, BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		delegatedPrivilegeObjectComboBox.setVisible(false);

		roleComboBox.onValueChanged.addListener(role -> {
			if (role == null || !role.isDelegatedCustomPrivilegeObjectRole()) {
				delegatedPrivilegeObjectComboBox.setVisible(false);
			} else {
				delegatedPrivilegeObjectComboBox.setValue(null);
				delegatedPrivilegeObjectComboBox.setVisible(true);
			}
		});

		EntityListModelBuilder<UserRoleAssignment> userRoleAssignmentModelBuilder = new EntityListModelBuilder<>(getApplicationInstanceData(), userRoleAssignment ->  userRoleAssignment.getRole().getTitle().getText() + " " + userRoleAssignment.getOrganizationUnit().getName().getText());
		Table<UserRoleAssignment> roleMemberTable = userRoleAssignmentModelBuilder.createListTable(true);
		roleMemberTable.setHideHeaders(true);
		TemplateField<Role> roleTemplateField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.createRolePropertyProvider(getApplicationInstanceData()));
		TemplateField<OrganizationUnit> organizationUnitTemplateField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.creatOrganizationUnitPropertyProvider(getApplicationInstanceData()));
		roleMemberTable.addColumn(new TableColumn<>("role", roleTemplateField));
		roleMemberTable.addColumn(new TableColumn<>("orgUnit", organizationUnitTemplateField));
		roleMemberTable.setPropertyExtractor((userRoleAssignment, propertyName) -> switch (propertyName){
			case "role" -> userRoleAssignment.getRole();
			case "orgUnit" -> userRoleAssignment.getOrganizationUnit();
			default -> null;
		});

		FormPanel roleMembersPanel = new FormPanel(getApplicationInstanceData());
		roleMembersPanel.setTable(roleMemberTable, userRoleAssignmentModelBuilder, ApplicationIcons.USERS_THREE_RELATION, getLocalized("userRoleAssignment.allRolesOfTheUser"),  true, false, false);

		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(450);
		formLayout.addSection(ApplicationIcons.USERS_THREE_RELATION, getLocalized("userRoleAssignment.title"));
		formLayout.addLabelAndField(null, getLocalized("userRoleAssignment.user"), userCombobox);
		formLayout.addLabelAndField(null, getLocalized("userRoleAssignment.role"), roleComboBox);
		formLayout.addLabelAndField(null, getLocalized("userRoleAssignment.orgUnit"), organizationComboBox);
		formLayout.addLabelAndField(null, getLocalized("userRoleAssignment.customPrivilegeObject"), delegatedPrivilegeObjectComboBox);
		formLayout.addLabelAndField(null, getLocalized("userRoleAssignment.mainResponsible"), mainResponsibleField);


		formLayout.addSection(ApplicationIcons.USER, getLocalized("userRoleAssignment.allRolesOfTheUser")).setCollapsed(true);
		formLayout.addLabelAndComponent(roleMembersPanel.getPanel());

		masterDetailController.createViews(getPerspective(), table, formLayout);

		formController.addNotNull(userCombobox);
		formController.addNotNull(roleComboBox);
		formController.addNotNull(organizationComboBox);
		formController.addValidator(roleComboBox, role -> {
			OrganizationUnit organizationUnit = OrganizationUtils.convert(organizationComboBox.getValue());
			if (organizationUnit != null && role != null) {
				OrganizationUnitType type = organizationUnit.getType();
				List<OrganizationUnitType> unitTypes = role.getAllowedOrganizationUnitTypes();
				if (!unitTypes.isEmpty() && !unitTypes.contains(type)) {
					return getLocalized("userRoleAssignment.wrongOrgUnitForThisRole");
				}
				if (role.isDelegatedCustomPrivilegeObjectRole() && delegatedPrivilegeObjectComboBox.getValue() == null) {
					return getLocalized("userRoleAssignment.missingCustomPrivilegeObject");
				}
			}
			return null;
		});
		formController.setSaveEntityHandler(userRoleAssignment -> {
			userRoleAssignment
					.setUser(userCombobox.getValue())
					.setRole(roleComboBox.getValue())
					.setOrganizationUnit(OrganizationUtils.convert(organizationComboBox.getValue()))
					.setDelegatedCustomPrivilegeObjectId(delegatedPrivilegeObjectComboBox.getValue() != null ? delegatedPrivilegeObjectComboBox.getValue().getId() : 0)
					.setMainResponsible(mainResponsibleField.getValue())
					.setLastVerified(Instant.now());
			return true;
		});

		entityModelBuilder.getOnSelectionEvent().addListener(userRoleAssignment -> {
			userCombobox.setValue(userRoleAssignment.getUser());
			roleComboBox.setValue(userRoleAssignment.getRole());
			organizationComboBox.setValue(OrganizationUtils.convert(userRoleAssignment.getOrganizationUnit()));
			mainResponsibleField.setValue(userRoleAssignment.isMainResponsible());
			delegatedPrivilegeObjectComboBox.setValue(null);
			if (userRoleAssignment.getDelegatedCustomPrivilegeObjectId() > 0) {
				ApplicationRoleAssignmentPrivilegeObjectProvider privilegeObjectProvider = getDelegatePrivilegeObjectProvider();
				if (privilegeObjectProvider != null) {
					delegatedPrivilegeObjectComboBox.setValue(privilegeObjectProvider.getPrivilegeObjectById(userRoleAssignment.getDelegatedCustomPrivilegeObjectId()));
				}
				delegatedPrivilegeObjectComboBox.setVisible(true);
			} else {
				delegatedPrivilegeObjectComboBox.setVisible(false);
			}
			userRoleAssignmentModelBuilder.setRecords(userRoleAssignment.getUser() != null ? userRoleAssignment.getUser().getRoleAssignments() : Collections.emptyList());
		});
		entityModelBuilder.setSelectedRecord(UserRoleAssignment.create());
	}

	private boolean matches(String value, String query) {
		return value != null && value.toLowerCase().contains(query);
	}

	private ApplicationRoleAssignmentPrivilegeObjectProvider getDelegatePrivilegeObjectProvider() {
		LoadedApplication loadedApplication = userSessionData.getRegistry().getLoadedApplication(getMainApplication());
		return loadedApplication.getBaseApplicationBuilder().getRoleAssignmentDelegatedPrivilegeObjectProvider();
	}

	public static Template createRoleAssignmentTemplate() {
		GridTemplate tpl = new GridTemplate()
				.setPadding(new Spacing(2))
				.addColumn(SizingPolicy.AUTO)
				.addColumn(SizingPolicy.FRACTION)
				.addRow(SizeType.FIXED, 22, 22, 0, 0)
				.addRow(SizeType.FIXED, 20, 20, 0, 0)
				.addRow(SizeType.FIXED, 20, 20, 0, 0)
				.addElement(new ImageElement("image", 0, 0, 60, 60).setRowSpan(4)
						.setBorder(new Border(new Line(Color.GRAY, LineType.SOLID, 0.5f)).setBorderRadius(300))
						.setShadow(Shadow.withSize(0.5f))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setMargin(new Spacing(0, 8, 0, 4)))
				.addElement(new TextElement("line1", 0, 1)
						.setWrapLines(false)
						.setFontStyle(new FontStyle(1f, Color.MATERIAL_GREY_900, null, true, false, false))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT))
				.addElement(new TextElement("line2", 1, 1)
						.setWrapLines(false)
						.setFontStyle(new FontStyle(1f, Color.MATERIAL_BLUE_900, null, true, false, false))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT))
				.addElement(new TextElement("line3", 2, 1)
						.setWrapLines(false)
						.setFontStyle(new FontStyle(1f, Color.MATERIAL_GREY_700, null, false, false, false))
						.setVerticalAlignment(VerticalElementAlignment.CENTER)
						.setHorizontalAlignment(HorizontalElementAlignment.LEFT));
		return tpl;
	}

}

