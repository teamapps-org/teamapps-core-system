/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
 * ---
 * Copyright (C) 2020 - 2023 TeamApps.org
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
package org.teamapps.application.server.system.machinetranslation;

import com.deepl.api.*;

import java.util.*;

public class DeepL2Translation {
	public static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Arrays.asList("bg", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "hu", "id", "it", "ja", "ko", "lt", "lv", "nb", "nl", "pl", "pt", "ro", "ru", "sk", "sl", "sv", "tr", "uk", "zh"));
	private final String apiKey;

	public DeepL2Translation(String apiKey) {
		this.apiKey = apiKey;
	}

	public void printUsage() {
		try {
			Translator translator = new Translator(apiKey);
			Usage usage = translator.getUsage();
			System.out.println(usage);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String translate(String text, String sourceLang, String targetLang) throws Exception {
		TextTranslationOptions options = createDefaultXmlEnabledOptions();
		return translate(text, sourceLang, targetLang, options);
	}

	public String translate(String text, String sourceLang, String targetLang, TextTranslationOptions options) throws Exception {
		targetLang = fixTargetLang(targetLang);
		Translator translator = new Translator(apiKey);
		TextResult result = translator.translateText(text, sourceLang, targetLang, options);
		return result.getText();
	}

	public List<TextResult> translate(List<String> values, String sourceLang, String targetLang, TextTranslationOptions options) throws Exception {
		targetLang = fixTargetLang(targetLang);
		Translator translator = new Translator(apiKey);
		return translator.translateText(values, sourceLang, targetLang, options);
	}

	private String fixTargetLang(String targetLang) {
		if (targetLang.equalsIgnoreCase("en")) {
			targetLang = "en-US";
		}
		if (targetLang.equalsIgnoreCase("pt")) {
			targetLang = "pt-PT";
		}
		return targetLang;
	}

	public static TextTranslationOptions createDefaultXmlEnabledOptions() {
		TextTranslationOptions options = new TextTranslationOptions();
		options.setSentenceSplittingMode(SentenceSplittingMode.NoNewlines);
		options.setPreserveFormatting(true);
		options.setTagHandling("xml");
		options.setOutlineDetection(true);
		options.setFormality(Formality.PreferLess);
		return options;
	}

	public static TextTranslationOptions createDefaultPlainTextOptions() {
		TextTranslationOptions options = new TextTranslationOptions();
		options.setSentenceSplittingMode(SentenceSplittingMode.NoNewlines);
		options.setPreserveFormatting(true);
		options.setFormality(Formality.PreferLess);
		return options;
	}

}
