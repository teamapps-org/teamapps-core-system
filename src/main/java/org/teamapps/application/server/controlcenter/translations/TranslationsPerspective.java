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
import org.teamapps.application.server.system.config.LocalizationConfig;
import org.teamapps.application.server.system.localization.LocalizationUtil;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.ui.dialogue.UploadDialogue;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.combo.ComboBoxUtils;
import org.teamapps.common.format.Color;
import org.teamapps.data.extract.PropertyExtractor;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.databinding.MutableValue;
import org.teamapps.icons.Icon;
import org.teamapps.icons.composite.CompositeIcon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.absolutelayout.Length;
import org.teamapps.ux.component.dialogue.FormDialogue;
import org.teamapps.ux.component.field.*;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.flexcontainer.VerticalLayout;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.format.Spacing;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;
import org.teamapps.ux.session.SessionContext;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TranslationsPerspective extends AbstractManagedApplicationPerspective {

	private final UserSessionData userSessionData;

	private String currentTranslationLanguage;
	private String currentTemplate1;
	private String currentTemplate2;

	public TranslationsPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		PerspectiveSessionData perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		View localizationKeyView = View.createView(StandardLayout.CENTER, ApplicationIcons.SPELL_CHECK, getLocalized("translations.overView"), null);
		View topicImageView = View.createView(StandardLayout.CENTER_BOTTOM, ApplicationIcons.FORM, getLocalized(Dictionary.PREVIEW_IMAGE), null);
		View translationView = View.createView(StandardLayout.RIGHT, ApplicationIcons.SPELL_CHECK, getLocalized("translations.translation"), null);
		localizationKeyView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.9f));
		translationView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.9f));

		topicImageView.setVisible(false);

		ToolbarButtonGroup buttonGroup = localizationKeyView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton createMissingEntries = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.GEARWHEELS, ApplicationIcons.FORM), getLocalized("translations.createMissingEntries"), getLocalized("translations.createMissingTranslationEntries")));
		ToolbarButton startMachineTranslationButton = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.GEARWHEELS, ApplicationIcons.MESSAGES), getLocalized("translations.startMachineTranslation"), getLocalized("translations.translateAllNewEntries")));
		ToolbarButton createTranslationFilesButton = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.FOLDER_ZIP, ApplicationIcons.EARTH), getLocalized("translations.createTranslationFiles"), getLocalized("translations.createApplicationResourceFiles")));

		buttonGroup = localizationKeyView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton fixDisplayValuesButton = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.TEXT, ApplicationIcons.OK), getLocalized("translations.fixValues"), getLocalized("translations.fixValues")));


		buttonGroup = localizationKeyView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton exportTranslationsButton = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.BOX_OUT, ApplicationIcons.EARTH), getLocalized(Dictionary.EXPORT), getLocalized("translations.exportTranslations")));
		ToolbarButton importTranslationsButton = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.BOX_INTO, ApplicationIcons.EARTH), getLocalized(Dictionary.IMPORT), getLocalized("translations.importTranslations")));

		buttonGroup = localizationKeyView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton importLocalizationKeys = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.TABLES, ApplicationIcons.EARTH), getLocalized(Dictionary.IMPORT), getLocalized("translations.importLocalizationKeys")));


		buttonGroup = translationView.addLocalButtonGroup(new ToolbarButtonGroup());
		ToolbarButton previousButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.NAVIGATE_LEFT, getLocalized(Dictionary.PREVIOUS)));
		ToolbarButton nextButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.NAVIGATE_RIGHT, getLocalized(Dictionary.NEXT)));

		buttonGroup = translationView.addLocalButtonGroup(new ToolbarButtonGroup());
		ToolbarButton doneButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.OK, getLocalized(Dictionary.DONE)));
		ToolbarButton unclearButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.DELETE, getLocalized(Dictionary.UNCLEAR)));

		buttonGroup = translationView.addLocalButtonGroup(new ToolbarButtonGroup());
		ToolbarButton verifiedButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.CHECKS, getLocalized(Dictionary.VERIFIED)));
		ToolbarButton incorrectButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.SIGN_WARNING, getLocalized(Dictionary.INCORRECT)));

		buttonGroup = translationView.addLocalButtonGroup(new ToolbarButtonGroup());
		ToolbarButton copyTranslationButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.COPY, getLocalized("translations.copyTranslation")));

		buttonGroup = translationView.addLocalButtonGroup(new ToolbarButtonGroup());
		ToolbarButton saveAdminValuesButton = buttonGroup.addButton(ToolbarButton.createSmall(ApplicationIcons.FLOPPY_DISKS, getLocalized(Dictionary.SAVE)));

		ComboBox<Language> languageCombo = Language.createComboBox(getApplicationInstanceData());
		ComboBox<Language> template1Combo = Language.createComboBox(getApplicationInstanceData());
		ComboBox<Language> template2Combo = Language.createComboBox(getApplicationInstanceData());
		template2Combo.setShowClearButton(true);

		ComboBox<TranslationWorkState> workStateComboBox = createWorkStateComboBox();
		ComboBox<LocalizationTopic> topicComboBox = createTopicComboBox();
		ComboBox<TranslationMode> modeComboBox = createTranslationModeComboBox();

		workStateComboBox.setValue(TranslationWorkState.ALL);
		modeComboBox.setValue(TranslationMode.TRANSLATE);
		languageCombo.setValue(Language.FR_FRENCH);
		template1Combo.setValue(Language.EN_ENGLISH);
		template2Combo.setValue(Language.DE_GERMAN);

		currentTranslationLanguage = "fr";
		currentTemplate1 = "en";
		currentTemplate2 = "de";

		ResponsiveForm<?> selectionForm = new ResponsiveForm<>(50, 75, 200);
		selectionForm.setMargin(Spacing.px(0));
		ResponsiveFormLayout formLayout = selectionForm.addResponsiveFormLayout(500);
		formLayout.addSection().setCollapsible(false).setPadding(new Spacing(0, 5)).setMargin(new Spacing(4, 2, 4, 2));

		formLayout.addLabelAndField(null, getLocalized("translations.translation"), languageCombo);
		formLayout.addLabelAndField(null, getLocalized("translations.status"), workStateComboBox, false);
		formLayout.addLabelAndField(null, getLocalized("translations.template1"), template1Combo);
		formLayout.addLabelAndField(null, getLocalized("translations.topic"), topicComboBox, false);
		formLayout.addLabelAndField(null, getLocalized("translations.template2"), template2Combo);
		formLayout.addLabelAndField(null, getLocalized("translations.mode"), modeComboBox, false);


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
			map.put("key", key.getKey());
			map.put("language", valueMap.get(currentTranslationLanguage));
			map.put("status", valueMap.get(currentTranslationLanguage));
			map.put("template1", valueMap.get(currentTemplate1));
			map.put("template2", valueMap.get(currentTemplate2));
			return map;
		});

		TemplateField<LocalizationValue> template1Column = createLocalizationValueTemplateField(true, false);
		TemplateField<LocalizationValue> template2Column = createLocalizationValueTemplateField(true, false);
		TemplateField<LocalizationValue> languageColumn = createLocalizationValueTemplateField(false, false);
		TemplateField<LocalizationValue> stateColumn = createLocalizationValueTemplateField(true, true);

		keyTable.addColumn("template1", getLocalized("translations.template1"), template1Column).setDefaultWidth(230);
		keyTable.addColumn("template2", getLocalized("translations.template2"), template2Column).setDefaultWidth(230);
		keyTable.addColumn("language", getLocalized("translations.translationLanguage"), languageColumn).setDefaultWidth(230);
		keyTable.addColumn("status", getLocalized("translations.status"), stateColumn).setDefaultWidth(170);
		keyTable.addColumn("key", getLocalized("translations.key"), new TextField()).setDefaultWidth(300);

		Function<String, String> languageByTableFieldNameFunction = field -> switch (field) {
			case "template1" -> currentTemplate1;
			case "template2" -> currentTemplate2;
			case "language" -> currentTranslationLanguage;
			default -> null;
		};
		entityModelBuilder.setCustomFieldSorter(fieldName -> {
			if (fieldName.equals("key")) {
				return Comparator.comparing(LocalizationKey::getKey, Comparator.nullsFirst(String::compareToIgnoreCase));
			}
			if (fieldName.equals("status")) {
				//todo sort by state
			}
			String language = languageByTableFieldNameFunction.apply(fieldName);
			Comparator<String> userStringComparator = getUser().getComparator(true);
			if (language != null) {
				return (k1, k2) -> userStringComparator.compare(TranslationUtils.getDisplayValueNonNull(k1, language), TranslationUtils.getDisplayValueNonNull(k2, language));
			}
			return null;
		});
		entityModelBuilder.setCustomFullTextFilter((localizationKey, s) -> {
			if (localizationKey.getKey().toLowerCase().contains(s)) {
				return true;
			}
			Map<String, LocalizationValue> valueMap = TranslationUtils.getValueMap(localizationKey);
			if (matchLocalizationValue(s, currentTemplate1, valueMap)) {
				return true;
			}
			if (matchLocalizationValue(s, currentTemplate2, valueMap)) {
				return true;
			}
			if (matchLocalizationValue(s, currentTranslationLanguage, valueMap)) {
				return true;
			}
			return false;
		});

		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.addComponent(selectionForm);
		verticalLayout.addComponentFillRemaining(keyTable);
		localizationKeyView.setComponent(verticalLayout);

		TemplateField<LocalizationValue> template1HeaderField = createLocalizationValueHeaderField(true);
		TemplateField<LocalizationValue> template2HeaderField = createLocalizationValueHeaderField(true);
		TemplateField<LocalizationValue> machineTranslationHeaderField = createLocalizationValueHeaderField(true);
		TemplateField<LocalizationValue> translationHeaderField = createLocalizationValueHeaderField(true);


		DisplayField template1ValueField = new DisplayField(true, false);
		DisplayField template2ValueField = new DisplayField(true, false);
		DisplayField machineTranslationValueField = new DisplayField(true, false);
		MultiLineTextField translationField = new MultiLineTextField();
		translationField.setCssStyle("height", "100px");

		MultiLineTextField adminLocalOverrideField = new MultiLineTextField();
		adminLocalOverrideField.setCssStyle("height", "100px");
		MultiLineTextField adminKeyOverrideField = new MultiLineTextField();
		adminKeyOverrideField.setCssStyle("height", "100px");
		MultiLineTextField keyCommentsField = new MultiLineTextField();
		keyCommentsField.setCssStyle("height", "100px");

		MultiLineTextField proofReadNotesField = new MultiLineTextField();
		proofReadNotesField.setCssStyle("height", "100px");


		//template1ValueField.setCssStyle("background-color", Color.RED.withAlpha(0.69f).toHtmlColorString());
		template1ValueField.setCssStyle(".field-border", "border-color", "#ec9a1a");
		template1ValueField.setCssStyle(".field-border-glow", "box-shadow", "0 0 3px 0 #ec9a1a");


		ResponsiveForm<?> form = new ResponsiveForm<>(120, 120, 0);
		formLayout = form.addResponsiveFormLayout(500);
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
		formLayout.addLabelAndField(null, getLocalized("translations.translationNotes"), keyCommentsField);
		formLayout.addLabelAndField(null, getLocalized("translations.finalTranslationLocal"), adminLocalOverrideField);
		formLayout.addLabelAndField(null, getLocalized("translations.finalTranslationGlobal"), adminKeyOverrideField);

		FormMetaFields formMetaFields = getApplicationInstanceData().getComponentFactory().createFormMetaFields();
		formMetaFields.addMetaFields(formLayout, false);
		entityModelBuilder.getOnSelectionEvent().addListener(formMetaFields::updateEntity);

		translationView.setComponent(form);

		getPerspective().addView(localizationKeyView);
		getPerspective().addView(topicImageView);
		getPerspective().addView(translationView);

		Consumer<TranslationMode> translationModeChangeHandler = translationMode -> {
			if (translationMode == null) translationMode = getAvailableModes().get(0);
			switch (translationMode) {
				case TRANSLATE -> {
					doneButton.setVisible(true);
					unclearButton.setVisible(true);
					copyTranslationButton.setVisible(true);
					verifiedButton.setVisible(false);
					incorrectButton.setVisible(false);
					saveAdminValuesButton.setVisible(false);
					proofReadNotesField.setVisible(false);
					workStateComboBox.setValue(TranslationWorkState.TRANSLATION_REQUIRED);
					adminLocalOverrideField.setVisible(false);
					adminKeyOverrideField.setVisible(false);
					keyCommentsField.setVisible(false);
				}
				case PROOFREAD -> {
					doneButton.setVisible(false);
					unclearButton.setVisible(false);
					copyTranslationButton.setVisible(false);
					verifiedButton.setVisible(true);
					incorrectButton.setVisible(true);
					saveAdminValuesButton.setVisible(false);
					proofReadNotesField.setVisible(true);
					workStateComboBox.setValue(TranslationWorkState.VERIFICATION_REQUIRED);
					adminLocalOverrideField.setVisible(false);
					adminKeyOverrideField.setVisible(false);
					keyCommentsField.setVisible(false);
				}
				case ADMINISTRATE -> {
					doneButton.setVisible(false);
					unclearButton.setVisible(false);
					copyTranslationButton.setVisible(false);
					verifiedButton.setVisible(false);
					incorrectButton.setVisible(false);
					saveAdminValuesButton.setVisible(true);
					proofReadNotesField.setVisible(false);
					workStateComboBox.setValue(TranslationWorkState.ALL);
					adminLocalOverrideField.setVisible(true);
					adminKeyOverrideField.setVisible(true);
					keyCommentsField.setVisible(true);

				}
			}
			Predicate<LocalizationKey> filterPredicate = TranslationUtils.getFilterPredicate(workStateComboBox.getValue(), currentTranslationLanguage, topicComboBox.getValue());
			entityModelBuilder.setCustomFilter(filterPredicate);
		};
		translationModeChangeHandler.accept(getAvailableModes().get(0));

		createMissingEntries.onClick.addListener(() -> {
			int values = LocalizationUtil.createRequiredLanguageValues(LocalizationKey.getAll(), userSessionData.getRegistry().getSystemConfig().getLocalizationConfig());
			UiUtils.showNotification(values > 0 ? ApplicationIcons.OK : ApplicationIcons.ERROR, getLocalized("translations.createdArg0MissingTranslationEntries", values));
		});

		startMachineTranslationButton.onClick.addListener(() -> {
			int values = LocalizationUtil.translateAllValues(userSessionData.getRegistry().getTranslationService(), userSessionData.getRegistry().getSystemConfig().getLocalizationConfig());
			UiUtils.showNotification(values > 0 ? ApplicationIcons.OK : ApplicationIcons.ERROR, getLocalized("translations.translatingArg0Entries", values));
		});

		createTranslationFilesButton.onClick.addListener(() -> {
			try {
				File file = LocalizationUtil.createTranslationResourceFiles();
				SessionContext.current().download(file, "Translations.zip");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		exportTranslationsButton.onClick.addListener(() -> {
			try {
				File file = LocalizationUtil.createTranslationExport(isAppFilter() ? getMainApplication() : null);
				SessionContext.current().download(file, "Translation-Export.zip");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		importTranslationsButton.onClick.addListener(() -> UploadDialogue.createFileUploadDialogue(file -> {
			try {
				LocalizationUtil.importTranslationExport(file, isAppFilter() ? getMainApplication() : null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, getApplicationInstanceData()));

		fixDisplayValuesButton.onClick.addListener(() -> {
			for (LocalizationValue localizationValue : LocalizationValue.getAll()) {
				if (localizationValue.getOriginal() != null && localizationValue.getAdminKeyOverride() == null && localizationValue.getAdminLocalOverride() == null && !localizationValue.getOriginal().equals(localizationValue.getCurrentDisplayValue())) {
					Application application = localizationValue.getLocalizationKey().getApplication();
					String app = application != null ? application.getName() : "";
					System.out.println(app + " - " +  localizationValue.getLocalizationKey().getKey() + ": " + localizationValue.getOriginal() + " -> " + localizationValue.getCurrentDisplayValue() + ", " + localizationValue.getMachineTranslation());
					localizationValue.setCurrentDisplayValue(localizationValue.getOriginal()).save();
				}
			}

		});


		importLocalizationKeys.onClick.addListener(() -> UploadDialogue.createFileUploadDialogue(file -> {
			try {
				LocalizationConfig localizationConfig = userSessionData.getRegistry().getSystemConfig().getLocalizationConfig();
				String result = LocalizationUtil.importLocalizationKeyFile(file, isAppFilter() ? getMainApplication() : null, localizationConfig);
				userSessionData.getRegistry().updateGlobalLocalizationProvider();
				FormDialogue formDialogue = new FormDialogue(ApplicationIcons.TABLES, "Localization key import result", "Localization key import result");
				formDialogue.setSize(600, 450);
				MultiLineTextField multiLineTextField = new MultiLineTextField();
				multiLineTextField.setValue(result);
				multiLineTextField.setCssStyle("height", Length.ofPixels(250).toCssString());
				formDialogue.addField(null, "Import messages", multiLineTextField);
				formDialogue.addOkButton("OK");
				formDialogue.setAutoCloseOnOk(true);
				formDialogue.show();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, getApplicationInstanceData()));

		modeComboBox.onValueChanged.addListener(translationModeChangeHandler);

		previousButton.onClick.addListener(entityModelBuilder::selectPreviousRecord);
		nextButton.onClick.addListener(entityModelBuilder::selectNextRecord);

		doneButton.onClick.addListener(() -> {
			LocalizationKey selectedRecord = entityModelBuilder.getSelectedRecord();
			LocalizationValue value = TranslationUtils.getValue(selectedRecord, currentTranslationLanguage);
			LocalizationValue templateValue = TranslationUtils.getValue(selectedRecord, currentTemplate1);
			String translation = translationField.getValue();
			if (translation != null && value != null && templateValue != null && templateValue.getCurrentDisplayValue() != null &&
					(TranslationUtils.createTranslationStates(TranslationState.TRANSLATION_REQUESTED, TranslationState.UNCLEAR)).contains(value.getTranslationState())) {
				if (translation.contains("\n") && !templateValue.getCurrentDisplayValue().contains("\n")) {
					UiUtils.showNotification(ApplicationIcons.ERROR, getLocalized("translations.translationMayNotContainLineBreaks"));
					return;
				}
				value
						.setTranslation(translation)
						.setCurrentDisplayValue(translation)
						.setTranslationState(TranslationState.OK)
						.setTranslationVerificationState(TranslationVerificationState.VERIFICATION_REQUESTED)
						.save();
				entityModelBuilder.selectNextRecord();
				UiUtils.showSaveNotification(true, getApplicationInstanceData());
			}
		});

		saveAdminValuesButton.onClick.addListener(() -> {
			LocalizationValue value = TranslationUtils.getValue(entityModelBuilder.getSelectedRecord(), currentTranslationLanguage);
			if (value != null) {
				if (keyCommentsField.getValue() != null) {
					value.getLocalizationKey().setComments(keyCommentsField.getValue()).save();
				}
				value
						.setAdminLocalOverride(adminLocalOverrideField.getValue())
						.setAdminKeyOverride(adminKeyOverrideField.getValue())
						.save();
				if (adminLocalOverrideField.getValue() != null || adminKeyOverrideField.getValue() != null) {
					value
							.setCurrentDisplayValue(adminKeyOverrideField.getValue() != null ? adminKeyOverrideField.getValue() : adminLocalOverrideField.getValue())
							.save();
				}
				UiUtils.showSaveNotification(true, getApplicationInstanceData());
			}

		});

		unclearButton.onClick.addListener(() -> {
			LocalizationValue value = TranslationUtils.getValue(entityModelBuilder.getSelectedRecord(), currentTranslationLanguage);
			if (value != null && value.getTranslationState() == TranslationState.TRANSLATION_REQUESTED) {
				value.setTranslationState(TranslationState.UNCLEAR).save();
				entityModelBuilder.selectNextRecord();
				entityModelBuilder.updateModels();
				UiUtils.showNotification(ApplicationIcons.OK, getLocalized("translations.translationSuccessfullyRejected"));
			}
		});

		copyTranslationButton.onClick.addListener(() -> {
			LocalizationValue value = TranslationUtils.getValue(entityModelBuilder.getSelectedRecord(), currentTranslationLanguage);
			translationField.setValue(value.getMachineTranslation());
		});

		verifiedButton.onClick.addListener(() -> {
			LocalizationValue value = TranslationUtils.getValue(entityModelBuilder.getSelectedRecord(), currentTranslationLanguage);
			if (value != null && value.getTranslation() != null && value.getTranslationState() == TranslationState.OK) {
				value.setTranslationVerificationState(TranslationVerificationState.OK).save();
				entityModelBuilder.selectNextRecord();
				UiUtils.showSaveNotification(true, getApplicationInstanceData());
			}
		});

		incorrectButton.onClick.addListener(() -> {
			LocalizationValue value = TranslationUtils.getValue(entityModelBuilder.getSelectedRecord(), currentTranslationLanguage);
			String notes = proofReadNotesField.getValue();
			if (notes != null && value != null && value.getTranslation() != null && value.getTranslationState() == TranslationState.OK) {
				value
						.setTranslationVerificationState(TranslationVerificationState.CORRECTIONS_REQUIRED)
						.setTranslationState(TranslationState.TRANSLATION_REQUESTED)
						.setNotes(notes)
						.save();
				entityModelBuilder.selectNextRecord();
				UiUtils.showNotification(ApplicationIcons.OK, getLocalized("translations.translationSuccessfullyRejected"));
			}
		});


		entityModelBuilder.getOnSelectionEvent().addListener(key -> {
			Map<String, LocalizationValue> valueMap = TranslationUtils.getValueMap(key);
			translationField.clearCustomFieldMessages();
			LocalizationValue languageValue = valueMap.get(currentTranslationLanguage);
			LocalizationValue template1Value = valueMap.get(currentTemplate1);
			LocalizationValue template2Value = valueMap.get(currentTemplate2);
			machineTranslationHeaderField.setValue(languageValue);
			machineTranslationValueField.setValue(languageValue == null ? " --- " : languageValue.getMachineTranslation() != null ? languageValue.getMachineTranslation() : " --- ");
			translationHeaderField.setValue(languageValue);
			translationField.setValue(languageValue != null ? languageValue.getTranslation() : null);
			template1HeaderField.setValue(template1Value);
			template1ValueField.setValue(template1Value == null ? " --- " : template1Value.getCurrentDisplayValue() != null ? template1Value.getCurrentDisplayValue() : " --- ");
			template2HeaderField.setValue(template2Value);
			template2ValueField.setValue(template2Value == null ? " --- " : template2Value.getCurrentDisplayValue() != null ? template2Value.getCurrentDisplayValue() : " --- ");
			if (languageValue != null) {
				if (languageValue.getLocalizationKey().getComments() != null) {
					translationField.addCustomFieldMessage(FieldMessage.Severity.INFO, key.getComments());
				}
				if (languageValue.getNotes() != null) {
					translationField.addCustomFieldMessage(FieldMessage.Severity.WARNING, languageValue.getNotes());
				}
			}
			proofReadNotesField.setValue(languageValue != null ? languageValue.getNotes() : null);
			adminLocalOverrideField.setValue(languageValue != null ? languageValue.getAdminLocalOverride() : null);
			adminKeyOverrideField.setValue(languageValue != null ? languageValue.getAdminKeyOverride() : null);
			keyCommentsField.setValue(key.getComments());
		});

		languageCombo.onValueChanged.addListener(language -> {
			currentTranslationLanguage = language != null ? language.getIsoCode() : null;
			Predicate<LocalizationKey> filterPredicate = TranslationUtils.getFilterPredicate(workStateComboBox.getValue(), currentTranslationLanguage, topicComboBox.getValue());
			entityModelBuilder.setCustomFilter(filterPredicate);
		});

		template1Combo.onValueChanged.addListener(language -> {
			currentTemplate1 = language != null ? language.getIsoCode() : null;
			entityModelBuilder.updateModels();
		});

		template2Combo.onValueChanged.addListener(language -> {
			currentTemplate2 = language != null ? language.getIsoCode() : null;
			entityModelBuilder.updateModels();
		});

		workStateComboBox.onValueChanged.addListener(state -> {
			Predicate<LocalizationKey> filterPredicate = state != null ? TranslationUtils.getFilterPredicate(state, currentTranslationLanguage, topicComboBox.getValue()) : null;
			entityModelBuilder.setCustomFilter(filterPredicate);
		});

		topicComboBox.onValueChanged.addListener(topic -> {
			Predicate<LocalizationKey> filterPredicate = TranslationUtils.getFilterPredicate(workStateComboBox.getValue(), currentTranslationLanguage, topic);
			entityModelBuilder.setCustomFilter(filterPredicate);
		});
	}

	private Boolean matchLocalizationValue(String query, String language, Map<String, LocalizationValue> valueMap) {
		LocalizationValue localizationValue = valueMap.get(language);
		if (localizationValue != null) {
			String displayValue = localizationValue.getCurrentDisplayValue();
			if (displayValue != null && displayValue.toLowerCase().contains(query)) {
				return true;
			}
		}
		return false;
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
		if (value == null || value.getTranslationVerificationState() == null) return null;
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
		if (value == null || value.getTranslationVerificationState() == null) return null;
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
