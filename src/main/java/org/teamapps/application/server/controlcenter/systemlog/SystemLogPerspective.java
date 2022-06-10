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
package org.teamapps.application.server.controlcenter.systemlog;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.system.utils.ApplicationUiUtils;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.combo.ComboBoxUtils;
import org.teamapps.application.ux.form.FormController;
import org.teamapps.application.ux.view.MasterDetailController;
import org.teamapps.common.format.Color;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.databinding.MutableValue;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.universaldb.pojo.Query;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.field.*;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.field.combobox.TagComboBox;
import org.teamapps.ux.component.field.datetime.InstantDateTimeField;
import org.teamapps.ux.component.flexcontainer.VerticalLayout;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.format.Spacing;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.table.TableColumn;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.timegraph.TimeGraph;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SystemLogPerspective extends AbstractManagedApplicationPerspective {

	private final UserSessionData userSessionData;

	public SystemLogPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		PerspectiveSessionData perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		Supplier<Query<SystemLog>> querySupplier = () -> isAppFilter() ? SystemLog.filter().application(NumericFilter.equalsFilter(getMainApplication().getId())) : SystemLog.filter();
		MasterDetailController<SystemLog> masterDetailController = new MasterDetailController<>(ApplicationIcons.CONSOLE, getLocalized("systemLog.systemLogs"), getApplicationInstanceData(), querySupplier, Privileges.SYSTEM_LOG_PERSPECTIVE);
		EntityModelBuilder<SystemLog> entityModelBuilder = masterDetailController.getEntityModelBuilder();
		FormController<SystemLog> formController = masterDetailController.getFormController();
		ResponsiveForm<SystemLog> form = masterDetailController.getResponsiveForm();

		ComboBox<LogLevel> logLeveComboBox = createLogLeveComboBox();
		ComboBox<User> userComboBox = createUserComboBox();
		ComboBox<String> exceptionClassComboBox = createExceptionClassComboBox();

		ComboBox<ManagedApplication> applicationComboBox = ComboBoxUtils.createRecordComboBox(() -> isAppFilter() ? Collections.singletonList(getManagedApplication()) : ManagedApplication.getAll(), PropertyProviders.createManagedApplicationPropertyProvider(userSessionData), BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		ComboBox<ManagedApplicationPerspective> perspectiveComboBox = ComboBoxUtils.createRecordComboBox(() -> applicationComboBox.getValue() == null ? Collections.emptyList() : applicationComboBox.getValue().getPerspectives(), PropertyProviders.createManagedApplicationPerspectivePropertyProvider(userSessionData), BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		ComboBox<ApplicationVersion> applicationVersionComboBox = ComboBoxUtils.createRecordComboBox(() -> applicationComboBox.getValue() == null ? Collections.emptyList() : applicationComboBox.getValue().getMainApplication().getVersions(), PropertyProviders.createApplicationVersionPropertyProvider(userSessionData), BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);

		if (isAppFilter()) {
			applicationComboBox.setValue(getManagedApplication());
			applicationComboBox.setVisible(false);
		}

		logLeveComboBox.setShowClearButton(true);
		userComboBox.setShowClearButton(true);
		exceptionClassComboBox.setShowClearButton(true);
		applicationComboBox.setShowClearButton(true);
		perspectiveComboBox.setShowClearButton(true);
		applicationVersionComboBox.setShowClearButton(true);

		ResponsiveForm<?> selectionForm = new ResponsiveForm<>(50, 75, 200);
		selectionForm.setMargin(Spacing.px(0));
		ResponsiveFormLayout formLayout = selectionForm.addResponsiveFormLayout(500);
		formLayout.addSection().setCollapsible(false).setPadding(new Spacing(0, 5)).setMargin(new Spacing(4, 2, 4, 2));

		formLayout.addLabelAndField(null, getLocalized("systemLog.logLevel"), logLeveComboBox);
		formLayout.addLabelAndField(null, getLocalized("applications.application"), applicationComboBox, false);
		formLayout.addLabelAndField(null, getLocalized("systemLog.user"), userComboBox);
		formLayout.addLabelAndField(null, getLocalized("applications.perspective"), perspectiveComboBox, false);
		formLayout.addLabelAndField(null, getLocalized("systemLog.exceptionClass"), exceptionClassComboBox);
		formLayout.addLabelAndField(null, getLocalized("applications.installedVersion"), applicationVersionComboBox, false);

		entityModelBuilder.updateModels();

		Table<SystemLog> table = entityModelBuilder.createTable();
		table.setDisplayAsList(true);
		table.setStripedRows(false);
		table.setRowHeight(28);
		table.setCssStyle("background-color", "white");
		table.setCssStyle("border-top", "1px solid " + Color.MATERIAL_GREY_400.toHtmlColorString());


		InstantDateTimeField timeField = new InstantDateTimeField();
		TemplateField<LogLevel> logLevelField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, createLogLevelPropertyProvider());
		TemplateField<SystemLog> messageField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, createSystemLogPropertyProvider());
		TemplateField<ManagedApplication> managedApplicationField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createManagedApplicationPropertyProvider(userSessionData));
		TemplateField<ManagedApplicationPerspective> perspectiveField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createManagedApplicationPerspectivePropertyProvider(userSessionData));
		TemplateField<User> userField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createUserPropertyProvider(getApplicationInstanceData()));


		table.addColumn(new TableColumn<>(SystemLog.FIELD_META_CREATION_DATE, getLocalized(Dictionary.DATE), timeField));
		table.addColumn(new TableColumn<SystemLog, LogLevel>(SystemLog.FIELD_LOG_LEVEL, getLocalized("systemLog.logLevel"), logLevelField).setDefaultWidth(75));
		table.addColumn(new TableColumn<SystemLog, SystemLog>(SystemLog.FIELD_MESSAGE, getLocalized("systemLog.logMessage"), messageField).setDefaultWidth(230));
		table.addColumn(new TableColumn<>(SystemLog.FIELD_MANAGED_APPLICATION, getLocalized("applicationProvisioning.provisionedApplication"), managedApplicationField));
		table.addColumn(new TableColumn<>(SystemLog.FIELD_MANAGED_PERSPECTIVE, getLocalized("systemLog.provisionedPerspective"), perspectiveField));
		table.addColumn(new TableColumn<>(SystemLog.FIELD_META_CREATED_BY, getLocalized("systemLog.user"), userField));
		table.addColumn(new TableColumn<>(SystemLog.FIELD_APPLICATION_VERSION, getLocalized("applications.installedVersion"), new TextField()));

		table.setPropertyExtractor((systemLog, propertyName) -> switch (propertyName) {
			case SystemLog.FIELD_META_CREATION_DATE -> systemLog.getMetaCreationDate();
			case SystemLog.FIELD_META_CREATED_BY -> systemLog.getMetaCreatedBy() > 0 ? User.getById(systemLog.getMetaCreatedBy()) : null;
			case SystemLog.FIELD_LOG_LEVEL -> systemLog.getLogLevel();
			case SystemLog.FIELD_MESSAGE -> systemLog;
			case SystemLog.FIELD_MANAGED_APPLICATION -> systemLog.getManagedApplication();
			case SystemLog.FIELD_MANAGED_PERSPECTIVE -> systemLog.getManagedPerspective();
			case SystemLog.FIELD_APPLICATION -> systemLog.getApplication();
			case SystemLog.FIELD_APPLICATION_VERSION -> systemLog.getApplicationVersion() != null ? systemLog.getApplicationVersion().getVersion() : null;
			default -> null;
		});

		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.addComponent(selectionForm);
		verticalLayout.addComponentFillRemaining(table);

		VerticalLayout detailsVerticalLayout = new VerticalLayout();
		detailsVerticalLayout.addComponent(form);
		formLayout = form.addResponsiveFormLayout(450);


		TemplateField<Application> applicationFormField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createApplicationPropertyProvider(userSessionData));
		TemplateField<ManagedApplication> managedApplicationFormField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createManagedApplicationPropertyProvider(userSessionData));
		TemplateField<ManagedApplicationPerspective> perspectiveFormField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createManagedApplicationPerspectivePropertyProvider(userSessionData));
		TemplateField<LogLevel> logLevelFormField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, createLogLevelPropertyProvider());
		TemplateField<User> userFormField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createUserPropertyProvider(getApplicationInstanceData()));
		DisplayField exceptionClassFormField = new DisplayField();
		DisplayField messageFormField = new DisplayField();
		MultiLineTextField detailsFormField = new MultiLineTextField();
		InstantDateTimeField timeFormField = new InstantDateTimeField();
		timeFormField.setEditingMode(FieldEditingMode.READONLY);

		formLayout.addSection().setCollapsible(false).setDrawHeaderLine(false);
		formLayout.addLabelAndField(null, getLocalized("applicationProvisioning.provisionedApplication"), managedApplicationFormField);
		formLayout.addLabelAndField(null, getLocalized("applications.application"), applicationFormField);
		formLayout.addLabelAndField(null, getLocalized("applications.perspective"), perspectiveFormField);
		formLayout.addLabelAndField(null, getLocalized("systemLog.logLevel"), logLevelFormField);
		formLayout.addLabelAndField(null, getLocalized("systemLog.exceptionClass"), exceptionClassFormField);
		formLayout.addLabelAndField(null, getLocalized("systemLog.user"), userFormField);
		formLayout.addLabelAndField(null, getLocalized(Dictionary.DATE), timeFormField);
		formLayout.addLabelAndField(null, getLocalized("systemLog.logMessage"), messageFormField);

		detailsVerticalLayout.addComponentFillRemaining(detailsFormField);

		masterDetailController.createViews(getPerspective(), verticalLayout, formLayout, false);
		masterDetailController.setDetailComponent(detailsVerticalLayout);

		Runnable onFilterChange = () -> {
			Predicate<SystemLog> filter = createFilter(logLeveComboBox.getValue(), userComboBox.getValue(), exceptionClassComboBox.getValue(), applicationComboBox.getValue(), perspectiveComboBox.getValue(), applicationVersionComboBox.getValue());
			entityModelBuilder.setCustomFilter(filter);
		};

		logLeveComboBox.onValueChanged.addListener(value -> onFilterChange.run());
		userComboBox.onValueChanged.addListener(value -> onFilterChange.run());
		exceptionClassComboBox.onValueChanged.addListener(value -> onFilterChange.run());
		applicationComboBox.onValueChanged.addListener(value -> onFilterChange.run());
		perspectiveComboBox.onValueChanged.addListener(value -> onFilterChange.run());
		applicationVersionComboBox.onValueChanged.addListener(value -> onFilterChange.run());

		entityModelBuilder.getOnSelectionEvent().addListener(log -> {
			managedApplicationFormField.setValue(log.getManagedApplication());
			applicationFormField.setValue(log.getApplication());
			perspectiveFormField.setValue(log.getManagedPerspective());
			logLevelFormField.setValue(log.getLogLevel());
			userFormField.setValue(log.getMetaCreatedBy() > 0 ? User.getById(log.getMetaCreatedBy()) : null);
			exceptionClassFormField.setValue(log.getExceptionClass());
			messageFormField.setValue(log.getMessage());
			detailsFormField.setValue(log.getDetails());
			timeFormField.setValue(log.getMetaCreationDate());
		});
	}

	private Predicate<SystemLog> createFilter(LogLevel logLevel, User user, String exceptionClass, ManagedApplication managedApplication, ManagedApplicationPerspective managedApplicationPerspective, ApplicationVersion applicationVersion) {
		if (logLevel == null && managedApplication == null && managedApplicationPerspective == null && applicationVersion == null && user == null && exceptionClass == null) return null;
		return systemLog -> {
			if (logLevel != null && !logLevel.equals(systemLog.getLogLevel())) {
				return false;
			}
			if (user != null && systemLog.getMetaCreatedBy() != user.getId()) {
				return false;
			}
			if (exceptionClass != null && !exceptionClass.equals(systemLog.getExceptionClass())) {
				return false;
			}
			if (managedApplication != null && !managedApplication.equals(systemLog.getManagedApplication())) {
				return false;
			}
			if (managedApplicationPerspective != null && !managedApplicationPerspective.equals(systemLog.getManagedPerspective())) {
				return false;
			}
			if (applicationVersion != null && !applicationVersion.equals(systemLog.getApplicationVersion())) {
				return false;
			}
			return true;
		};
	}

//	private Predicate<SystemLog> createFilter(LogLevel logLevel, Application application, User user, String exceptionClass) {
//		if (logLevel == null && application == null && user == null && exceptionClass == null) return null;
//		return systemLog -> {
//			if (logLevel != null && !logLevel.equals(systemLog.getLogLevel())) {
//				return false;
//			}
//			if (application != null && !application.equals(systemLog.getApplication())) {
//				return false;
//			}
//			if (user != null && systemLog.getMetaCreatedBy() != user.getId()) {
//				return false;
//			}
//			if (exceptionClass != null && !exceptionClass.equals(systemLog.getExceptionClass())) {
//				return false;
//			}
//			return true;
//		};
//	}

	private ComboBox<LogLevel> createLogLeveComboBox() {
		ComboBox<LogLevel> comboBox = ComboBoxUtils.createRecordComboBox(Arrays.asList(LogLevel.values()), createLogLevelPropertyProvider(), BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		comboBox.setDropDownTemplate(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE);
		return comboBox;
	}

	private ComboBox<User> createUserComboBox() {
		ComboBox<User> comboBox = new ComboBox<>(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		comboBox.setPropertyProvider(PropertyProviders.createUserPropertyProvider(getApplicationInstanceData()));
		comboBox.setRecordToStringFunction(user -> user.getFirstName() + " " + user.getLastName());
		comboBox.setModel(query -> query == null || query.isBlank() ?
				User.getAll().stream().limit(50).collect(Collectors.toList()) :
				User.filter().parseFullTextFilter(query).execute().stream().limit(50).collect(Collectors.toList()));
		return comboBox;
	}

	private ComboBox<String> createExceptionClassComboBox() {
		Set<String> exceptionClasses = new HashSet<>();
		SystemLog.getAll().forEach(log -> {
			if (log.getExceptionClass() != null) {
				exceptionClasses.add(log.getExceptionClass());
			}
		});
		return ComboBoxUtils.createRecordComboBox(new ArrayList<>(exceptionClasses), PropertyProviders.createStringPropertyProvider(ApplicationIcons.BUG), BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
	}


	private PropertyProvider<LogLevel> createLogLevelPropertyProvider() {
		return (logLevel, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, getLoglevelIcon(logLevel));
			map.put(BaseTemplate.PROPERTY_CAPTION, logLevel.name());
			return map;
		};
	}

	private PropertyProvider<SystemLog> createSystemLogPropertyProvider() {
		return (log, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, getLoglevelIcon(log.getLogLevel()));
			map.put(BaseTemplate.PROPERTY_CAPTION, log.getMessage());
			return map;
		};
	}

	private Icon getLoglevelIcon(LogLevel level) {
		return switch (level) {
			case INFO -> ApplicationIcons.INFORMATION;
			case WARNING -> ApplicationIcons.SIGN_WARNING;
			case ERROR -> ApplicationIcons.DELETE;
		};
	}

}

