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

import org.teamapps.model.controlcenter.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TranslationUtils {

	public static Predicate<LocalizationKey> getFilterPredicate(TranslationWorkState workState, String language, LocalizationTopic topic) {
		if (workState == null || language == null) return null;
		return switch (workState) {
			case ALL ->  createFilterPredicate(language, topic, null, null, null);
			case CORRECTIONS_REQUIRED -> createFilterPredicate(language, topic, null, createTranslationStates(TranslationState.TRANSLATION_REQUESTED), createVerificationStates(TranslationVerificationState.CORRECTIONS_REQUIRED));
			case TRANSLATION_REQUIRED -> createFilterPredicate(language, topic, null, createTranslationStates(TranslationState.TRANSLATION_REQUESTED), null);
			case VERIFICATION_REQUIRED -> createFilterPredicate(language, topic, null, createTranslationStates(TranslationState.OK), createVerificationStates(TranslationVerificationState.VERIFICATION_REQUESTED));
			case VERIFIED -> createFilterPredicate(language, topic, null, null, createVerificationStates(TranslationVerificationState.OK));
			case UNCLEAR -> createFilterPredicate(language, topic, null, createTranslationStates(TranslationState.UNCLEAR), null);
			case TRANSLATION_NOT_NECESSARY -> createFilterPredicate(language, topic, null, createTranslationStates(TranslationState.NOT_NECESSARY), null);
		};
	}

	public static Predicate<LocalizationKey> createFilterPredicate(String language, LocalizationTopic topic, Set<MachineTranslationState> machineTranslationStates, Set<TranslationState> translationStates, Set<TranslationVerificationState> verificationStates) {
		return key -> {
			if (topic != null && !key.getTopics().contains(topic)) {
				return false;
			}
			LocalizationValue value = getValue(key, language);
			if (value == null) {
				return false;
			}
			if (machineTranslationStates != null && !machineTranslationStates.contains(value.getMachineTranslationState())) {
				return false;
			}
			if (translationStates != null && !translationStates.contains(value.getTranslationState())) {
				return false;
			}
			if (verificationStates != null && !verificationStates.contains(value.getTranslationVerificationState())) {
				return false;
			}
			return true;
		};
	}

	public static Set<MachineTranslationState> createMachineStates(MachineTranslationState... states) {
		if (states == null || states.length == 0) {
			return null;
		} else {
			return new HashSet<>(Arrays.asList(states));
		}
	}

	public static Set<TranslationState> createTranslationStates(TranslationState... states) {
		if (states == null || states.length == 0) {
			return null;
		} else {
			return new HashSet<>(Arrays.asList(states));
		}
	}

	public static Set<TranslationVerificationState> createVerificationStates(TranslationVerificationState... states) {
		if (states == null || states.length == 0) {
			return null;
		} else {
			return new HashSet<>(Arrays.asList(states));
		}
	}

	public static LocalizationValue getValue(LocalizationKey key, String language) {
		if (key == null) return null;
		return key.getLocalizationValues().stream()
				.filter(value -> language.equals(value.getLanguage()))
				.findAny()
				.orElse(null);
	}

	public static String getDisplayValue(LocalizationKey key, String language) {
		LocalizationValue value = getValue(key, language);
		return value != null ? value.getCurrentDisplayValue() : null;
	}

	public static String getDisplayValueNonNull(LocalizationKey key, String language) {
		String value = getDisplayValue(key, language);
		return value != null ? value : "";
	}

	public static Map<String, LocalizationValue> getValueMap(LocalizationKey key) {
		return key.getLocalizationValues().stream().collect(Collectors.toMap(LocalizationValue::getLanguage, k -> k));
	}
}
