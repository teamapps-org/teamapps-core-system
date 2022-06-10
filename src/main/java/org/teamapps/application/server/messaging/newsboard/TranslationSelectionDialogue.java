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
package org.teamapps.application.server.messaging.newsboard;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Language;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.config.LocalizationConfig;
import org.teamapps.application.server.system.machinetranslation.TranslationService;
import org.teamapps.application.ux.form.FormWindow;
import org.teamapps.model.controlcenter.NewsBoardMessage;
import org.teamapps.model.controlcenter.NewsBoardMessageTranslation;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.field.combobox.TagComboBox;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TranslationSelectionDialogue extends FormWindow {

	private final NewsBoardMessage message;
	private final boolean translateAll;
	private final LocalizationConfig localizationConfig;
	private final TranslationService translationService;
	private final Runnable runOnSuccess;

	public TranslationSelectionDialogue(NewsBoardMessage message, boolean translateAll, LocalizationConfig localizationConfig, TranslationService translationService, Runnable runOnSuccess, ApplicationInstanceData applicationInstanceData) {
		super(ApplicationIcons.EARTH, applicationInstanceData.getLocalized("newsBoard.translations"), applicationInstanceData);
		this.message = message;
		this.translateAll = translateAll;
		this.localizationConfig = localizationConfig;
		this.translationService = translationService;
		this.runOnSuccess = runOnSuccess;
		createUi();
	}

	private void createUi() {
		ComboBox<Language> languageComboBox = Language.createComboBox(getApplicationInstanceData());
		Language originalLanguage = Language.getLanguageByIsoCode(message.getLanguage());
		languageComboBox.setValue(originalLanguage);

		Set<Language> availableLanguages = message.getTranslations().stream().map(translation -> Language.getLanguageByIsoCode(translation.getLanguage())).filter(Objects::nonNull).collect(Collectors.toSet());
		if (originalLanguage != null) {
			availableLanguages.add(originalLanguage);
		}
		List<Language> translatableLanguages = localizationConfig.getRequiredLanguages().stream().map(Language::getLanguageByIsoCode).filter(Objects::nonNull).collect(Collectors.toList());
		List<Language> allowedLanguages = translatableLanguages.stream().filter(language -> !availableLanguages.contains(language)).collect(Collectors.toList());

		TagComboBox<Language> languageTagComboBox = Language.createTagComboBox(getApplicationInstanceData(), new HashSet<>(allowedLanguages));
		languageTagComboBox.setDistinct(true);
		if (translateAll) {
			languageTagComboBox.setValue(allowedLanguages);
		}

		addField(getLocalized("newsBoard.originalLanguage"), languageComboBox);
		addField(getLocalized("newsBoard.translationLanguages"), languageTagComboBox);

		addOkButton();
		addButtonGroup();
		addButton(ApplicationIcons.ADD, getLocalized("newsBoard.addAllLanguages")).onClick.addListener(() -> languageTagComboBox.setValue(null));
		addButton(ApplicationIcons.DELETE, getLocalized("newsBoard.removeLanguages")).onClick.addListener(() -> languageTagComboBox.setValue(allowedLanguages));
		addButtonGroup();
		addCancelButton();

		getOkButton().onClick.addListener(() -> {
			Language mainLanguage = languageComboBox.getValue();
			List<Language> translationLanguages = languageTagComboBox.getValue();
			if (message.getHtmlMessage() != null && translationService != null && mainLanguage != null && translationLanguages != null && !translationLanguages.isEmpty()) {
				message.save();
				runTaskAsync(ApplicationIcons.GEARWHEELS, getLocalized("newsBoard.translatingMessage__"), () -> {
					translateMessage(mainLanguage, translationLanguages);
					return true;
				}, result -> {
					runOnSuccess.run();
				});
				close();
			}
		});

		show();
	}

	private void translateMessage(Language original, List<Language> languages) {
		String originalMessage = message.getHtmlMessage();
		if (originalMessage.isBlank()) {
			return;
		}
		List<CompletableFuture<Void>> futures;
		try {
			futures = new ArrayList<>();
			for (Language language : languages) {
				CompletableFuture<Void> future = CompletableFuture.runAsync(() -> translate(original, language, originalMessage));
				futures.add(future);
			}
			CompletableFuture<Void> completableFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
			completableFuture.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void translate(Language src, Language dst, String value) {
		String translatedMessage = translationService.translate(value, src.getIsoCode(), dst.getIsoCode());
		System.out.println("Translated message " + dst.getIsoCode() + ":" + translatedMessage);
		if (translatedMessage != null) {
			NewsBoardMessageTranslation.create()
					.setMessage(message)
					.setTranslation(translatedMessage)
					.setLanguage(dst.getIsoCode())
					.save();
		}
	}
}
