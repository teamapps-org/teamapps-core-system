/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
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
package org.teamapps.application.server.system.postaladdress;

import com.ibm.icu.text.Transliterator;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Country;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.icons.Icon;
import org.teamapps.icons.composite.CompositeIcon;
import org.teamapps.ux.component.field.Label;
import org.teamapps.ux.component.field.TextField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.form.ResponsiveFormLayout;

import java.util.*;

public class PostalAddressForm {

	private static Transliterator TRANSLITERATOR = Transliterator.getInstance("Any-Latin; nfd; [:nonspacing mark:] remove; nfc");
	private final boolean withName; //title, first, last
	private final boolean withOrganization;
	private final String initialCountry;
	private Map<String, PostalAddressFormat> formatMap;
	private List<Label> labels = new ArrayList<>();
	private List<Label> translatedLabels = new ArrayList<>();
	private List<TextField> textFields = new ArrayList<>();
	private List<TextField> translatedTextFields = new ArrayList<>();
	private Map<PostalAddressElementType, Integer> currentTypeLineMap;

	public PostalAddressForm(boolean withName, boolean withOrganization, String initialCountry) {
		this.withName = withName;
		this.withOrganization = withOrganization;
		this.initialCountry = initialCountry;
		this.formatMap = PostalAddressData.createFormatMap();
	}

	public void createFormSection(ResponsiveFormLayout formLayout, ApplicationInstanceData applicationInstanceData) {
		createFormSection(formLayout, applicationInstanceData, ApplicationIcons.FORM, applicationInstanceData.getLocalized(Dictionary.ADDRESS), CompositeIcon.of(ApplicationIcons.FORM, ApplicationIcons.FIND_TEXT), applicationInstanceData.getLocalized(Dictionary.TRANSLATION));
	}

	public void createFormSection(ResponsiveFormLayout formLayout, ApplicationInstanceData applicationInstanceData, Icon icon, String title, Icon translatedAddressIcon, String translatedAddressTitle) {
		formLayout.addSection(icon, title);
		ComboBox<Country> countryComboBox = Country.createComboBox(applicationInstanceData);
		countryComboBox.setValue(Country.getCountryByIsoCode(initialCountry));
		formLayout.addLabelAndField(null, applicationInstanceData.getLocalized(Dictionary.COUNTRY), countryComboBox);
		for (int i = 0; i < 9; i++) {
			TextField textField = new TextField();
			textField.setVisible(false);
			Label label = (Label) formLayout.addLabelAndField(null, "none", textField).label.getField();
			labels.add(label);
			textFields.add(textField);
		}

		formLayout.addSection(translatedAddressIcon, translatedAddressTitle).setHideWhenNoVisibleFields(true);
		for (int i = 0; i < 9; i++) {
			TextField textField = new TextField();
			textField.setVisible(false);
			Label label = (Label) formLayout.addLabelAndField(null, "none", textField).label.getField();
			translatedLabels.add(label);
			translatedTextFields.add(textField);
		}

		for (int i = 0; i < 9; i++) {
			createTextFieldHandler(textFields.get(i), translatedTextFields.get(i));
		}

		countryComboBox.onValueChanged.addListener(value -> {
			String isoCode = value.getIsoCode();
			if (isoCode == null) return;
			updateCountry(isoCode);
		});
		updateCountry(initialCountry);
	}

	public void updateCountry(String country) {
		PostalAddressFormat addressFormat = formatMap.get(country);
		if (addressFormat == null) {
			addressFormat = formatMap.get("ZZ");
		}


		Map<PostalAddressElementType, Integer> typeLineMap = new HashMap<>();
		Set<Integer> usedLines = new HashSet<>();
		int line = 0;
		for (PostalAddressElement element : addressFormat.getElements()) {
			textFields.get(line).setVisible(true);
			typeLineMap.put(element.getType(), line);
			usedLines.add(line);
			String required = element.isRequired() ? "*" : "";
			switch (element.getType()) {
				case FIRST_NAME -> {
					if (withName) {
						labels.get(line).setCaption("First name" + required);
						translatedLabels.get(line).setCaption("First name" + required);
					}
				}
				case LAST_NAME -> {
					if (withName) {
						labels.get(line).setCaption("Last name" + required);
						translatedLabels.get(line).setCaption("Last name" + required);
					}
				}
				case ORGANIZATION -> {
					if (withOrganization) {
						labels.get(line).setCaption("Organization/Company" + required);
						translatedLabels.get(line).setCaption("Organization/Company" + required);
					}
				}
				case ADDRESS -> {
					labels.get(line).setCaption("Street" + required);
					translatedLabels.get(line).setCaption("Street" + required);
				}
				case DEPENDENT_LOCALITY -> {
					labels.get(line).setCaption("Dependent locality" + required);
					translatedLabels.get(line).setCaption("Dependent locality" + required);
				}
				case CITY -> {
					labels.get(line).setCaption("City" + required);
					translatedLabels.get(line).setCaption("City" + required);
				}
				case STATE -> {
					labels.get(line).setCaption("State" + required);
					translatedLabels.get(line).setCaption("State" + required);
				}
				case ZIP -> {
					labels.get(line).setCaption("ZIP" + required);
					translatedLabels.get(line).setCaption("ZIP" + required);
				}
				case SORTING_CODE -> {
					labels.get(line).setCaption("Sorting code" + required);
					translatedLabels.get(line).setCaption("Sorting code" + required);
				}
			}
			line++;
		}


		if (currentTypeLineMap != null) {
			Set<Integer> visibleTranslatedLines = new HashSet<>();
			Map<PostalAddressElementType, String> valueMap = new HashMap<>();
			Map<PostalAddressElementType, String> translatedValueMap = new HashMap<>();
			for (PostalAddressElementType type : addressFormat.getTypes()) {
				if (currentTypeLineMap.containsKey(type)) {
					int previousLine = currentTypeLineMap.get(type);
					valueMap.put(type, textFields.get(previousLine).getValue());
					translatedValueMap.put(type, translatedTextFields.get(previousLine).getValue());
				}
			}
			for (PostalAddressElementType type : addressFormat.getTypes()) {
				int currentLine = typeLineMap.get(type);
				textFields.get(currentLine).setValue(valueMap.get(type));
				String translatedValue = translatedValueMap.get(type);
				if (translatedValue != null) {
					translatedTextFields.get(currentLine).setValue(translatedValue);
					translatedTextFields.get(currentLine).setVisible(true);
					visibleTranslatedLines.add(currentLine);
				}
			}
			for (int i = 0; i < 9; i++) {
				if (!visibleTranslatedLines.contains(i)) {
					translatedTextFields.get(i).setVisible(false);
				}
			}
		}


		for (int i = 0; i < 9; i++) {
			if (!usedLines.contains(i)) {
				textFields.get(i).setVisible(false);
				translatedTextFields.get(i).setVisible(false);
			}
		}

		currentTypeLineMap = typeLineMap;
	}

	private String getLastVale(PostalAddressElementType type, boolean translation) {
		if (currentTypeLineMap == null) {
			return null;
		}
		if (translation) {
			TextField textField = translatedTextFields.get(currentTypeLineMap.get(type));
			return textField.getValue();
		} else {
			TextField textField = textFields.get(currentTypeLineMap.get(type));
			return textField.getValue();
		}
	}


	private void createTextFieldHandler(TextField textField, TextField translatedTextField) {
		textField.onTextInput.addListener(s -> {
			if (isNonLatin(s)) {
				String transliteratedText = TRANSLITERATOR.transliterate(s);
				translatedTextField.setValue(transliteratedText);
				translatedTextField.setVisible(true);
			} else {
				translatedTextField.setVisible(false);
				if (translatedTextField.getValue() != null) {
					translatedTextField.setValue(null);
				}
			}
		});
	}

	private static boolean isNonLatin(String s) {
		if (s == null) {
			return false;
		}
		int n = s.codePointCount(0, s.length());
		for (int i = 0; i < n; i = s.offsetByCodePoints(i, 1)) {
			Character.UnicodeScript script = Character.UnicodeScript.of(s.codePointAt(i));
			if (script != Character.UnicodeScript.COMMON && script != Character.UnicodeScript.LATIN) {
				return true;
			}
		}
		return false;
	}

}
