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
package org.teamapps.application.server.system.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocalizationConfig {

	private List<String> allowedSourceLanguages = new ArrayList<>(Arrays.asList("en", "de", "fr", "es", "pt", "nl", "it", "pl", "ru", "da", "et", "fi", "cs"));
	private List<String> requiredLanguages = new ArrayList<>(Arrays.asList("bg", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "hu", "it", "ja", "lt", "lv", "nl", "pl", "pt", "ro", "ru", "sk", "sl", "sv", "zh"));

	public List<String> getAllowedSourceLanguages() {
		return allowedSourceLanguages;
	}

	public void setAllowedSourceLanguages(List<String> allowedSourceLanguages) {
		this.allowedSourceLanguages = allowedSourceLanguages;
	}

	public List<String> getRequiredLanguages() {
		return requiredLanguages;
	}

	public void setRequiredLanguages(List<String> requiredLanguages) {
		this.requiredLanguages = requiredLanguages;
	}
}
