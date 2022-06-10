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

import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.icons.Icon;

public enum TranslationWorkState {

	ALL,
	CORRECTIONS_REQUIRED,
	TRANSLATION_REQUIRED,
	VERIFICATION_REQUIRED,
	VERIFIED,
	UNCLEAR,
	TRANSLATION_NOT_NECESSARY,
	;

	public Icon getIcon() {
		return switch (this) {
			case ALL -> ApplicationIcons.SHAPE_CIRCLE;
			case CORRECTIONS_REQUIRED -> ApplicationIcons.SIGN_WARNING;
			case TRANSLATION_REQUIRED -> ApplicationIcons.BRIEFCASE;
			case VERIFICATION_REQUIRED -> ApplicationIcons.CHECKS;
			case VERIFIED -> ApplicationIcons.OK;
			case UNCLEAR -> ApplicationIcons.QUESTION;
			case TRANSLATION_NOT_NECESSARY -> ApplicationIcons.INBOX_EMPTY;
		};
	}

	public String getTranslationKey() {
		return switch (this) {
			case ALL -> "translations.workState.all";
			case CORRECTIONS_REQUIRED -> "translations.workState.CorrectionsRequired";
			case TRANSLATION_REQUIRED -> "translations.workState.TranslationRequired";
			case VERIFICATION_REQUIRED -> "translations.workState.VerificationRequired";
			case VERIFIED -> "translations.workState.verified";
			case UNCLEAR -> "translations.workState.unclear";
			case TRANSLATION_NOT_NECESSARY -> "translations.workState.TranslationNotNecessary";
		};
	}

}
