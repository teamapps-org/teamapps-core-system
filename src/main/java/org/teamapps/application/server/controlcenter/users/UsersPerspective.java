/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
 * ---
 * Copyright (C) 2020 - 2024 TeamApps.org
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
package org.teamapps.application.server.controlcenter.users;

import org.apache.commons.io.IOUtils;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.perspective.PerspectiveBuilder;
import org.teamapps.application.api.application.perspective.PerspectiveMenuPanel;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.localization.Language;
import org.teamapps.application.api.password.SecurePasswordHash;
import org.teamapps.application.api.privilege.Privilege;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.controlcenter.roles.UserRoleAssignmentPerspectiveBuilder;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.postaladdress.PostalAddressForm;
import org.teamapps.application.server.system.privilege.UserPrivileges;
import org.teamapps.application.server.system.privilege.UserPrivilegesView;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.system.utils.ValueConverterUtils;
import org.teamapps.application.server.ui.address.AddressForm;
import org.teamapps.application.tools.EntityListModelBuilder;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.combo.ComboBoxUtils;
import org.teamapps.application.ux.form.FormController;
import org.teamapps.application.ux.form.FormPanel;
import org.teamapps.application.ux.view.MasterDetailController;
import org.teamapps.application.ux.window.ApplicationWindow;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.databinding.MutableValue;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.pojo.Entity;
import org.teamapps.universaldb.pojo.Query;
import org.teamapps.ux.component.dialogue.FormDialogue;
import org.teamapps.ux.component.field.*;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.field.combobox.TagBoxWrappingMode;
import org.teamapps.ux.component.field.combobox.TagComboBox;
import org.teamapps.ux.component.field.datetime.InstantDateTimeField;
import org.teamapps.ux.component.field.upload.PictureChooser;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.format.VerticalElementAlignment;
import org.teamapps.ux.component.itemview.SimpleItemView;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.table.TableColumn;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;
import org.teamapps.ux.resource.ByteArrayResource;
import org.teamapps.ux.resource.Resource;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UsersPerspective extends AbstractManagedApplicationPerspective {

	private final UserSessionData userSessionData;
	private final boolean withDetailFields = false;

	public UsersPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		PerspectiveSessionData perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		List<OrganizationUnitView> allowedUnits = getAllowedUnits(Privileges.USERS_PERSPECTIVE, Privilege.READ);
		Set<Integer> unitIdSet = allowedUnits.stream().map(Entity::getId).collect(Collectors.toSet());

		Supplier<Query<User>> querySupplier = () -> User.filter().customFilter(user -> user.getOrganizationUnit() == null || unitIdSet.contains(user.getOrganizationUnit().getId()));
		MasterDetailController<User> masterDetailController = new MasterDetailController<>(ApplicationIcons.USERS_CROWD, getLocalized("users.users"), getApplicationInstanceData(), querySupplier, Privileges.USERS_PERSPECTIVE, User.FIELD_ORGANIZATION_UNIT);
		EntityModelBuilder<User> entityModelBuilder = masterDetailController.getEntityModelBuilder();
		FormController<User> formController = masterDetailController.getFormController();
		ResponsiveForm<User> form = masterDetailController.getResponsiveForm();

		Table<User> table = entityModelBuilder.createListTable(false);
		table.setRowHeight(32);
		entityModelBuilder.updateModels();

		TemplateField<User> userTableField = UiUtils.createTemplateField(BaseTemplate.createTreeSingleLineNodeTemplate(28, VerticalElementAlignment.CENTER, 30), PropertyProviders.createUserPropertyProvider(getApplicationInstanceData()));
		TemplateField<OrganizationUnit> orgUnitTableField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.creatOrganizationUnitPropertyProvider(getApplicationInstanceData()));
		TemplateField<UserAccountStatus> accountStatusTableField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, createAccountStatusPropertyProvider());
		InstantDateTimeField lastLogin = new InstantDateTimeField();
		ComboBox<Language> languageComboBox = Language.createComboBox(getApplicationInstanceData());
		TagComboBox<UserRoleAssignment> userRoleAssignmentTableField = UiUtils.createTagComboBox(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createUserRoleAssignmentPropertyProviderNoUserDisplay(getApplicationInstanceData()));

		table.addColumn(User.FIELD_LAST_NAME, getLocalized("users.user"), userTableField).setDefaultWidth(250);
		table.addColumn(User.FIELD_ORGANIZATION_UNIT, getLocalized("users.organizationUnit"), orgUnitTableField).setDefaultWidth(150);
		table.addColumn(User.FIELD_USER_ACCOUNT_STATUS, getLocalized("users.accountStatus"), accountStatusTableField).setDefaultWidth(120);
		table.addColumn("lastLogin", getLocalized("users.lastLogin"), lastLogin).setDefaultWidth(200);
		table.addColumn(User.FIELD_DISPLAY_LANGUAGE, getLocalized(Dictionary.LANGUAGE), languageComboBox).setDefaultWidth(350);
		table.addColumn(User.FIELD_ROLE_ASSIGNMENTS, getLocalized("users.roles"), userRoleAssignmentTableField).setDefaultWidth(1500);

		table.setPropertyExtractor((user, propertyName) -> switch (propertyName) {
			case User.FIELD_LAST_NAME -> user;
			case User.FIELD_ORGANIZATION_UNIT -> user.getOrganizationUnit();
			case User.FIELD_USER_ACCOUNT_STATUS -> user.getUserAccountStatus();
			case "lastLogin" -> user.getLoginStats() != null ? user.getLoginStats().getLastLogin() : null;
			case User.FIELD_DISPLAY_LANGUAGE -> user.getDisplayLanguage() == null ? null : Language.getLanguageByIsoCode(user.getDisplayLanguage());
			case User.FIELD_ROLE_ASSIGNMENTS -> user.getRoleAssignments();
			default -> null;
		});

		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(450);

		ToolbarButtonGroup buttonGroup = new ToolbarButtonGroup();
		ToolbarButton updatePasswordButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.KEYS, getLocalized(Dictionary.RESET_PASSWORD)));
		formController.addToolbarButtonGroup(buttonGroup);

		buttonGroup = new ToolbarButtonGroup();
		ToolbarButton userPrivilegesButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.LOCK_OPEN, getLocalized(Dictionary.PRIVILEGES)));
		formController.addToolbarButtonGroup(buttonGroup);

		PictureChooser pictureChooser = new PictureChooser();
		pictureChooser.setImageDisplaySize(120, 120);
		pictureChooser.setTargetImageSize(240, 240);
		//pictureChooser.setBrowseButtonIcon(ApplicationIcons.UPLOAD);
		pictureChooser.setMaxFileSize(5_000_000);

		TextField firstNameField = new TextField();
		TextField lastNameField = new TextField();
		ComboBox<Language> languageField = Language.createComboBox(getApplicationInstanceData());
		TextField emailField = new TextField();
		TextField mobileField = new TextField();
		TextField loginField = new TextField();
		PasswordField passwordField = new PasswordField();
		ComboBox<UserAccountStatus> accountStatusComboBox = createAccountStatusComboBox();
		AbstractField<OrganizationUnitView> organizationUnitViewField = formController.getOrganizationUnitViewField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_TWO_LINES, false);

		EntityListModelBuilder<UserRoleAssignment> userRoleAssignmentModelBuilder = new EntityListModelBuilder<>(getApplicationInstanceData(), userRoleAssignment ->  userRoleAssignment.getRole().getTitle().getText() + " " + userRoleAssignment.getOrganizationUnit().getName().getText());
		Table<UserRoleAssignment> roleMemberTable = userRoleAssignmentModelBuilder.createListTable(true);
		roleMemberTable.setHideHeaders(true);
		roleMemberTable.setDisplayAsList(true);
		roleMemberTable.setRowHeight(40);
		TemplateField<Role> roleTemplateField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, PropertyProviders.createRolePropertyProvider(getApplicationInstanceData()));
		TemplateField<OrganizationUnit> organizationUnitTemplateField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, PropertyProviders.creatOrganizationUnitWithPathPropertyProvider(getApplicationInstanceData()));
		roleMemberTable.addColumn(new TableColumn<>("role", roleTemplateField));
		roleMemberTable.addColumn(new TableColumn<>("orgUnit", organizationUnitTemplateField));
		roleMemberTable.setPropertyExtractor((userRoleAssignment, propertyName) -> switch (propertyName){
			case "role" -> userRoleAssignment.getRole();
			case "orgUnit" -> userRoleAssignment.getOrganizationUnit();
			default -> null;
		});

		FormPanel roleMembersPanel = new FormPanel(getApplicationInstanceData());
		roleMembersPanel.setTable(roleMemberTable, userRoleAssignmentModelBuilder, ApplicationIcons.USERS_THREE_RELATION, getLocalized("userRoleAssignment.allRolesOfTheUser"),  true, false, false);

		formLayout.addSection(ApplicationIcons.USER, getLocalized("users.user"));
		formLayout.addLabelAndField(null, getLocalized("users.profilePicture"), pictureChooser);
		formLayout.addLabelAndField(null, getLocalized(Dictionary.FIRST_NAME), firstNameField);
		formLayout.addLabelAndField(null, getLocalized(Dictionary.LAST_NAME), lastNameField);
		formLayout.addLabelAndField(null, getLocalized(Dictionary.LANGUAGE), languageField);

		if (withDetailFields) {
			formLayout.addLabelAndField(null, getLocalized(Dictionary.E_MAIL), emailField);
			formLayout.addLabelAndField(null, getLocalized(Dictionary.MOBILE_NUMBER), mobileField);
		}
		formLayout.addLabelAndField(null, getLocalized(Dictionary.USER_NAME), loginField);
		if (withDetailFields) {
			formLayout.addLabelAndField(null, getLocalized("users.accountStatus"), accountStatusComboBox);
		}
		formLayout.addLabelAndField(null, getLocalized("users.organizationUnit"), organizationUnitViewField);

		if (withDetailFields) {
			AddressForm addressForm = new AddressForm(getApplicationInstanceData());
			addressForm.createAddressSection(formLayout);
			addressForm.addFields(formLayout);
		}

		formLayout.addSection(ApplicationIcons.USERS_THREE_RELATION, getLocalized("userRoleAssignment.allRolesOfTheUser")).setCollapsed(true);
		formLayout.addLabelAndComponent(roleMembersPanel.getPanel());


		formController.addNotBlank(firstNameField);
		formController.addNotBlank(lastNameField);
		//formController.addEmailOrEmpty(emailField);
		formController.addPhoneOrEmptyNumber(mobileField);
		formController.addMinCharactersOrEmpty(loginField, 2);
		//formController.addMinCharactersOrEmpty(passwordField, 9);
		//formController.addNotNull(languageField);
		//formController.addValidator(organizationUnitViewField, unit -> unit != null && OrganizationUtils.convert(unit).getType().isAllowUsers() ? null : getLocalized("users.wrongOrMissingOrgUnit"));
		formController.addValidator(organizationUnitViewField, unit -> unit != null ? null : getLocalized("users.wrongOrMissingOrgUnit"));

		masterDetailController.createViews(getPerspective(), table, formLayout);

		updatePasswordButton.onClick.addListener(() -> {
			User user = entityModelBuilder.getSelectedRecord();
			if (user != null) {
				showUpdatePasswordDialogue(user);
			}
		});

		userPrivilegesButton.onClick.addListener(() -> {
			User user = entityModelBuilder.getSelectedRecord();
			if (user != null) {
				showUserPrivilegesWindow(user);
			}
		});

		formController.setSaveEntityHandler(user -> {
//			if (!addressForm.validateAddress() || (user.getAddress() != null && user.getAddress().isStored() && !addressForm.getAddress().equals(user.getAddress()))) {
//				return false;
//			}
			OrganizationUnit organizationUnit = OrganizationUtils.convert(organizationUnitViewField.getValue());
			byte[] picture = readUserPicture(pictureChooser);
//			Address address = addressForm.getAddress().save();
			user
					.setFirstName(firstNameField.getValue())
					.setLastName(lastNameField.getValue())
					.setDisplayLanguage(languageField.getValue() == null ? null : languageField.getValue().getIsoCode())
					.setEmail(emailField.getValue())
					.setMobile(mobileField.getValue())
					.setLogin(loginField.getValue())
					.setUserAccountStatus(accountStatusComboBox.getValue())
					.setOrganizationUnit(organizationUnit)
					//.setAddress(address.isStored() ? address : null)
			;
			if (picture != null && picture.length != user.getProfilePictureLength()) {
				user.setProfilePicture(picture);
			}
			return true;
		});

		entityModelBuilder.getOnSelectionEvent().addListener(user -> {
			pictureChooser.setValue(user.getProfilePicture() != null ? new ByteArrayResource(user.getProfilePicture(), "image.jpg") : null);
			firstNameField.setValue(user.getFirstName());
			lastNameField.setValue(user.getLastName());
			languageField.setValue(Language.getLanguageByIsoCode(user.getDisplayLanguage()));
			emailField.setValue(user.getEmail());
			mobileField.setValue(user.getMobile());
			loginField.setValue(user.getLogin());
			passwordField.setValue(user.getPassword());
			accountStatusComboBox.setValue(user.getUserAccountStatus());
			//addressForm.setAddress(user.getAddress());
			formController.clearMessages();
			userRoleAssignmentModelBuilder.setRecords(user.getRoleAssignments());
		});

		Supplier<User> createNewEntitySupplier = () -> User.create().setUserAccountStatus(UserAccountStatus.ACTIVE);
		//Supplier<User> createNewEntitySupplier = () -> User.create().setAddress(Address.create()).setUserAccountStatus(UserAccountStatus.ACTIVE);
		formController.setCreateNewEntitySupplier(createNewEntitySupplier);
		entityModelBuilder.setSelectedRecord(createNewEntitySupplier.get());
	}

	private void showUpdatePasswordDialogue(User user) {
		FormDialogue formDialogue = FormDialogue.create(ApplicationIcons.KEYS, getLocalized(Dictionary.RESET_PASSWORD), getLocalized(Dictionary.RESET_PASSWORD));
		PasswordField passwordField = new PasswordField();
		formDialogue.addField(ApplicationIcons.KEYS, getLocalized(Dictionary.PASSWORD), passwordField);
		formDialogue.addOkCancelButtons(getLocalized(Dictionary.O_K), getLocalized(Dictionary.CANCEL));
		formDialogue.onOk.addListener(() -> {
			String value = passwordField.getValue();
			if (value.length() > 5) {
				user.setPassword(SecurePasswordHash.createDefault().createSecureHash(value)).save();
				UiUtils.showSaveNotification(true, getApplicationInstanceData());
				formDialogue.close();
			} else {
				UiUtils.showSaveNotification(false, getApplicationInstanceData());
			}
		});
		formDialogue.setCloseable(true);
		formDialogue.setCloseOnEscape(true);
		formDialogue.setMaximizable(true);
		formDialogue.show();
	}

	private void showUserPrivilegesWindow(User user) {
		UserPrivileges userPrivileges = new UserPrivileges(user, userSessionData.getRegistry(), null);
		UserPrivilegesView userPrivilegesView = new UserPrivilegesView(userPrivileges, getApplicationInstanceData());
		ApplicationWindow window = new ApplicationWindow(ApplicationIcons.LOCK_OPEN, getLocalized(Dictionary.PRIVILEGES), getApplicationInstanceData());
		window.setContent(userPrivilegesView.getResponsiveForm());
		window.addCancelButton();
		window.setWindowPreferredSize(1000, 800, 0.1f);
		window.show();
	}


	private byte[] readUserPicture(PictureChooser pictureChooser) {
		if (pictureChooser.getValue() != null) {
			Resource value = pictureChooser.getValue();
			try {
				return IOUtils.readFully(value.getInputStream(), (int) value.getLength());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public String getRolesString(List<UserRoleAssignment> assignments, int limit) {
		return assignments.stream()
				.filter(assignment -> assignment.getRole() != null)
				.map(assignment -> getLocalized(assignment.getRole().getTitle()))
				.limit(limit)
				.collect(Collectors.joining(", "));
	}

	private ComboBox<UserAccountStatus> createAccountStatusComboBox() {
		return ComboBoxUtils.createRecordComboBox(Arrays.asList(UserAccountStatus.values()), createAccountStatusPropertyProvider(), BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
	}

	private PropertyProvider<UserAccountStatus> createAccountStatusPropertyProvider() {
		return (userAccountStatus, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, getAccountStatusIcon(userAccountStatus));
			map.put(BaseTemplate.PROPERTY_CAPTION, getLocalized("users.accountStatus." + userAccountStatus.name()));
			return map;
		};
	}

	private Icon getAccountStatusIcon(UserAccountStatus status) {
		return switch (status) {
			case ACTIVE -> ApplicationIcons.OK;
			case INACTIVE -> ApplicationIcons.ERROR;
			case SUPER_ADMIN -> ApplicationIcons.PILOT;
		};
	}
}

