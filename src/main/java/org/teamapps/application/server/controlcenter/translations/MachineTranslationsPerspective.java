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
package org.teamapps.application.server.controlcenter.translations;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.localization.Language;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.api.ui.FormMetaFields;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.application.ux.combo.ComboBoxUtils;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.common.format.Color;
import org.teamapps.data.extract.PropertyExtractor;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.databinding.MutableValue;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.icons.Icon;
import org.teamapps.icons.composite.CompositeIcon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.field.DisplayField;
import org.teamapps.ux.component.field.MultiLineTextField;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.table.TableColumn;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;

import java.util.*;
import java.util.function.Function;

public class MachineTranslationsPerspective extends AbstractManagedApplicationPerspective {

	private final PerspectiveSessionData perspectiveSessionData;
	private final UserSessionData userSessionData;

	private String currentLanguage;
	private String currentTemplate1;
	private String currentTemplate2;

	private boolean language1Visible;
	private boolean language2Visible;
	private boolean machineTranslationVisible;

	/*
		Export to property files
			each language one property file, sorted by keys

		Run machine translation
			Check that there is no other task running

		Maybe better a translation app:
			Application translation
				For Translators
				For Proofreaders
				Dashboard: numbers, status of all languages, per language
				For Administrators
			Machine translation
			Language settings: required languages
			Machine translation config

	 */


	public MachineTranslationsPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		View localizationKeyView = View.createView(StandardLayout.CENTER, ApplicationIcons.EARTH_LINK, getLocalized("machineTranslation.title"), null);
		View translationView = View.createView(StandardLayout.RIGHT, ApplicationIcons.EARTH_LINK, getLocalized("machineTranslation.title"), null);
		localizationKeyView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.9f));
		translationView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.9f));


		ToolbarButtonGroup buttonGroup = localizationKeyView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton overViewButtonOn = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.SPELL_CHECK, ApplicationIcons.CHECKBOX), getLocalized("translations.overView"), getLocalized("translations.showOverView")));
		ToolbarButton overViewButtonOff = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.SPELL_CHECK, ApplicationIcons.DELETE), getLocalized("translations.overView"), getLocalized("translations.hideOverView")));
		overViewButtonOn.setVisible(false);


		buttonGroup = translationView.addLocalButtonGroup(new ToolbarButtonGroup());
		ToolbarButton previousButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.NAVIGATE_LEFT, getLocalized(Dictionary.PREVIOUS)));
		ToolbarButton nextButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.NAVIGATE_RIGHT, getLocalized(Dictionary.NEXT)));


		ComboBox<Language> languageCombo = Language.createComboBox(getApplicationInstanceData());


		EntityModelBuilder<LocalizationKey> entityModelBuilder = new EntityModelBuilder<>(() -> isAppFilter() ? LocalizationKey.filter().application(NumericFilter.equalsFilter(getMainApplication().getId())) : LocalizationKey.filter(), getApplicationInstanceData());
		entityModelBuilder.updateModels();
		entityModelBuilder.attachSearchField(localizationKeyView);
		entityModelBuilder.attachViewCountHandler(localizationKeyView, () -> getLocalized("translations.overView"));
		Table<LocalizationKey> keyTable = entityModelBuilder.createTable();
		keyTable.setDisplayAsList(true);
		keyTable.setStripedRows(false);

		keyTable.setCssStyle("background-color", "white");
		keyTable.setCssStyle("border-top", "1px solid " + Color.MATERIAL_GREY_400.toHtmlColorString());
		keyTable.setRowHeight(28);

		keyTable.setPropertyProvider((key, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			Map<String, LocalizationValue> valueMap = TranslationUtils.getValueMap(key);
			map.put("key", key);
			map.put("language", valueMap.get(currentLanguage));
			map.put("status", valueMap.get(currentLanguage));
			map.put("template1", valueMap.get(currentTemplate1));
			map.put("template2", valueMap.get(currentTemplate2));
			return map;
		});

		TemplateField<LocalizationValue> template1Column = createLocalizationValueTemplateField(true, false);
		TemplateField<LocalizationValue> template2Column = createLocalizationValueTemplateField(true, false);
		TemplateField<LocalizationValue> languageColumn = createLocalizationValueTemplateField(false, false);
		TemplateField<LocalizationValue> stateColumn = createLocalizationValueTemplateField(true, true);

		keyTable.addColumn(new TableColumn<>("template1", getLocalized("translations.template1"), template1Column));
		keyTable.addColumn(new TableColumn<>("template2", getLocalized("translations.template2"), template2Column));
		keyTable.addColumn(new TableColumn<>("language", getLocalized("translations.language"), languageColumn));
		keyTable.addColumn(new TableColumn<>("status", getLocalized("translations.status"), stateColumn));

		Function<String, String> languageByTableFieldNameFunction = field -> switch (field) {
			case "template1" -> currentTemplate1;
			case "template2" -> currentTemplate2;
			case "language" -> currentLanguage;
			default -> null;
		};
		entityModelBuilder.setCustomFieldSorter(fieldName -> {
			String language = languageByTableFieldNameFunction.apply(fieldName);
			if (language != null) {
				return (k1, k2) -> TranslationUtils.getDisplayValueNonNull(k1, language).compareToIgnoreCase(TranslationUtils.getDisplayValueNonNull(k2, language));
			}
			return null; //todo sort by state
		});

		localizationKeyView.setComponent(keyTable);

		TemplateField<LocalizationValue> template1HeaderField = createLocalizationValueHeaderField(true);
		TemplateField<LocalizationValue> template2HeaderField = createLocalizationValueHeaderField(true);
		TemplateField<LocalizationValue> machineTranslationHeaderField = createLocalizationValueHeaderField(true);
		TemplateField<LocalizationValue> translationHeaderField = createLocalizationValueHeaderField(true);


		DisplayField template1ValueField = new DisplayField(true, false);
		DisplayField template2ValueField = new DisplayField(true, false);
		DisplayField machineTranslationValueField = new DisplayField(true, false);
		MultiLineTextField translationField = new MultiLineTextField();
		translationField.setCssStyle("height", "100px");

		MultiLineTextField proofReadNotesField = new MultiLineTextField();
		proofReadNotesField.setCssStyle("height", "100px");


		//template1ValueField.setCssStyle("background-color", Color.RED.withAlpha(0.69f).toHtmlColorString());
		template1ValueField.setCssStyle(".field-border", "border-color", "#ec9a1a");
		template1ValueField.setCssStyle(".field-border-glow", "box-shadow", "0 0 3px 0 #ec9a1a");


		ResponsiveForm form = new ResponsiveForm(120, 120, 0);
		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(500);
		formLayout.addSection(null, getLocalized("translations.template1")).setCollapsible(false).setDrawHeaderLine(false).setHideWhenNoVisibleFields(true);
		formLayout.addComponent(0, 0, template1HeaderField);
		formLayout.addComponent(0, 1, template1ValueField);

		formLayout.addSection(null, getLocalized("translations.template2")).setCollapsible(false).setDrawHeaderLine(true).setHideWhenNoVisibleFields(true);
		formLayout.addComponent(0, 0, template2HeaderField);
		formLayout.addComponent(0, 1, template2ValueField);

		formLayout.addSection(null, getLocalized("translations.automaticTranslation")).setCollapsible(false).setDrawHeaderLine(true).setHideWhenNoVisibleFields(true);
		formLayout.addComponent(0, 0, machineTranslationHeaderField);
		formLayout.addComponent(0, 1, machineTranslationValueField);


		formLayout.addSection(null, getLocalized("translations.translation")).setCollapsible(false).setDrawHeaderLine(true).setHideWhenNoVisibleFields(true);
		formLayout.addComponent(0, 0, translationHeaderField);
		formLayout.addComponent(0, 1, translationField);

		formLayout.addSection(null, getLocalized("translations.mode.proofread")).setCollapsible(false).setDrawHeaderLine(true).setHideWhenNoVisibleFields(true);
		formLayout.addLabelAndField(null, getLocalized("translations.errorNotes"), proofReadNotesField);

		formLayout.addSection(null, getLocalized("translations.administration")).setCollapsible(false).setDrawHeaderLine(true).setHideWhenNoVisibleFields(true);

		FormMetaFields formMetaFields = getApplicationInstanceData().getComponentFactory().createFormMetaFields();
		formMetaFields.addMetaFields(formLayout, false);
		entityModelBuilder.getOnSelectionEvent().addListener(formMetaFields::updateEntity);

		translationView.setComponent(form);

	}

	private TemplateField<LocalizationValue> createLocalizationValueHeaderField(boolean skipState) {
		TemplateField<LocalizationValue> templateField = new TemplateField<>(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		templateField.setPropertyProvider((value, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			if (value == null) return map;
			Language language = Language.getLanguageByIsoCode(value.getLanguage());
			map.put(BaseTemplate.PROPERTY_ICON, language.getIcon());
			String title = language.getLanguageLocalized(getApplicationInstanceData());
			if (!skipState) {
				title += " (" + getLocalizationStateText(value) + ")";
			}
			map.put(BaseTemplate.PROPERTY_CAPTION, title);
			return map;
		});
		return templateField;
	}

	private TemplateField<LocalizationValue> createLocalizationValueTemplateField(boolean withStateIcon, boolean withStateText) {
		TemplateField<LocalizationValue> templateField = new TemplateField<>(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		templateField.setPropertyProvider((value, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			if (value == null) return map;
			if (withStateIcon) {
				map.put(BaseTemplate.PROPERTY_ICON, getLocalizationStateIcon(value));
			}
			if (withStateText) {
				map.put(BaseTemplate.PROPERTY_CAPTION, getLocalizationStateText(value));
			} else {
				map.put(BaseTemplate.PROPERTY_CAPTION, value.getCurrentDisplayValue());
			}
			return map;
		});
		return templateField;
	}

	private Icon getLocalizationStateIcon(LocalizationValue value) {
		if (value == null) return null;
		return switch (value.getTranslationVerificationState()) {
			case VERIFICATION_REQUESTED -> ApplicationIcons.CHECKS;
			case OK -> ApplicationIcons.OK;
			case CORRECTIONS_REQUIRED -> ApplicationIcons.SIGN_WARNING;
			default -> switch (value.getTranslationState()) {
				case TRANSLATION_REQUESTED -> ApplicationIcons.BRIEFCASE;
				case UNCLEAR -> ApplicationIcons.QUESTION;
				case NOT_NECESSARY -> ApplicationIcons.OK;
				default -> ApplicationIcons.FOLDER;
			};
		};
	}

	private String getLocalizationStateText(LocalizationValue value) {
		if (value == null) return null;
		return switch (value.getTranslationVerificationState()) {
			case VERIFICATION_REQUESTED -> getLocalized(TranslationWorkState.VERIFICATION_REQUIRED.getTranslationKey());
			case OK -> getLocalized(TranslationWorkState.VERIFIED.getTranslationKey());
			case CORRECTIONS_REQUIRED -> getLocalized(TranslationWorkState.CORRECTIONS_REQUIRED.getTranslationKey());
			default -> switch (value.getTranslationState()) {
				case TRANSLATION_REQUESTED -> getLocalized(TranslationWorkState.TRANSLATION_REQUIRED.getTranslationKey());
				case UNCLEAR -> getLocalized(TranslationWorkState.UNCLEAR.getTranslationKey());
				case NOT_NECESSARY -> getLocalized(TranslationWorkState.TRANSLATION_NOT_NECESSARY.getTranslationKey());
				default -> "?";
			};
		};
	}

	private ComboBox<TranslationWorkState> createWorkStateComboBox() {
		PropertyExtractor<TranslationWorkState> propertyExtractor = (workState, propertyName) -> switch (propertyName) {
			case BaseTemplate.PROPERTY_ICON -> workState.getIcon();
			case BaseTemplate.PROPERTY_CAPTION -> getLocalized(workState.getTranslationKey());
			default -> null;
		};
		ComboBox<TranslationWorkState> comboBox = ComboBoxUtils.createRecordComboBox(Arrays.asList(TranslationWorkState.values()), propertyExtractor, BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		comboBox.setDropDownTemplate(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE);
		return comboBox;
	}

	private ComboBox<LocalizationTopic> createTopicComboBox() {
		PropertyProvider<Application> applicationPropertyProvider = PropertyProviders.createApplicationPropertyProvider(userSessionData);
		PropertyProvider<LocalizationTopic> propertyProvider = (localizationTopic, propertyName) -> {
			Map<String, Object> map = new HashMap<>();
			if (localizationTopic == null) return map;
			if (localizationTopic.getApplication() != null) {
				Map<String, Object> values = applicationPropertyProvider.getValues(localizationTopic.getApplication(), Collections.emptyList());
				map.put(BaseTemplate.PROPERTY_ICON, values.get(BaseTemplate.PROPERTY_ICON));
				map.put(BaseTemplate.PROPERTY_CAPTION, values.get(BaseTemplate.PROPERTY_CAPTION));
			} else {
				map.put(BaseTemplate.PROPERTY_ICON, localizationTopic.getIcon() != null ? IconUtils.decodeIcon(localizationTopic.getIcon()) : ApplicationIcons.TAGS);
				map.put(BaseTemplate.PROPERTY_CAPTION, localizationTopic.getTitle());
			}
			return map;
		};
		ComboBox<LocalizationTopic> comboBox = ComboBoxUtils.createRecordComboBox(isAppFilter() ? LocalizationTopic.filter().application(NumericFilter.equalsFilter(getMainApplication().getId())).execute() : LocalizationTopic.getAll(), propertyProvider, BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		comboBox.setDropDownTemplate(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE);
		comboBox.setShowClearButton(true);
		return comboBox;
	}

	private ComboBox<TranslationMode> createTranslationModeComboBox() {
		PropertyExtractor<TranslationMode> propertyExtractor = (mode, propertyName) -> switch (propertyName) {
			case BaseTemplate.PROPERTY_ICON -> mode.getIcon();
			case BaseTemplate.PROPERTY_CAPTION -> getLocalized(mode.getTranslationKey());
			default -> null;
		};
		ComboBox<TranslationMode> comboBox = ComboBoxUtils.createRecordComboBox(Arrays.asList(TranslationMode.values()), propertyExtractor, BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		comboBox.setDropDownTemplate(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE);
		return comboBox;
	}


	private List<TranslationMode> getAvailableModes() {
		//todo
		return Arrays.asList(TranslationMode.values());
	}


}
