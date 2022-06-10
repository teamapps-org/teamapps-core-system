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
package org.teamapps.application.server.ui.localize;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.ui.TranslationKeyField;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.event.Event;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.ux.component.field.AbstractField;
import org.teamapps.ux.component.field.FieldEditingMode;
import org.teamapps.ux.component.field.TextField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.linkbutton.LinkButton;
import org.teamapps.ux.component.template.BaseTemplate;

import java.util.function.Supplier;

public class LocalizationTranslationKeyField implements TranslationKeyField {

	public final Event<String> onValueChanged = new Event<>();
	private final ComboBox<String> localizationKeyCombo;
	private final TextField keyTextField;
	private final LinkButton linkButton;

	public LocalizationTranslationKeyField(String linkButtonCaption, ApplicationInstanceData applicationInstanceData, SystemRegistry systemRegistry, Supplier<Application> applicationSupplier) {
		this(linkButtonCaption, applicationInstanceData, systemRegistry, applicationSupplier, false, false);
	}

	public LocalizationTranslationKeyField(String linkButtonCaption, ApplicationInstanceData applicationInstanceData, SystemRegistry systemRegistry, Supplier<Application> applicationSupplier, boolean allowMultiLine, boolean selectionFieldWithKey) {
		BaseTemplate template = selectionFieldWithKey ? BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES : BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE;
		localizationKeyCombo = LocalizationUiUtils.createLocalizationKeyCombo(template, applicationInstanceData, applicationSupplier);
		localizationKeyCombo.setDropDownTemplate(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES);
		localizationKeyCombo.setShowClearButton(true);
		keyTextField = new TextField();
		keyTextField.setEditingMode(FieldEditingMode.READONLY);

		linkButton = new LinkButton(linkButtonCaption);
		localizationKeyCombo.onValueChanged.addListener(value -> {
			keyTextField.setValue(value);
			onValueChanged.fire(value);
		});
		LocalizationKeyWindow localizationKeyWindow = new LocalizationKeyWindow(applicationInstanceData, systemRegistry, applicationSupplier);
		localizationKeyWindow.onNewKey.addListener(value -> {
			localizationKeyCombo.setValue(value);
			keyTextField.setValue(value);
			localizationKeyCombo.onValueChanged.fire(value);
			onValueChanged.fire(value);
		});
		linkButton.onClicked.addListener(() -> {
			localizationKeyWindow.resetUi();
			localizationKeyWindow.show();
		});
	}

	@Override
	public Event<String> getOnValueChanged() {
		return onValueChanged;
	}

	@Override
	public AbstractField<String> getSelectionField() {
		return localizationKeyCombo;
	}

	@Override
	public AbstractField<String> getKeyDisplayField() {
		return keyTextField;
	}

	@Override
	public LinkButton getKeyLinkButton() {
		return linkButton;
	}

	@Override
	public void setKey(String key) {
		localizationKeyCombo.setValue(key);
		keyTextField.setValue(key);
	}

	@Override
	public String getKey() {
		return localizationKeyCombo.getValue();
	}


}
