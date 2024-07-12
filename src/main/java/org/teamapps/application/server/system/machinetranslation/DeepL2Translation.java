/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
 * ---
 * Copyright (C) 2020 - 2024 TeamApps.org
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
	public static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Arrays.asList("ar", "bg", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "hu", "id", "it", "ja", "ko", "lt", "lv", "no", "nl", "pl", "pt", "ro", "ru", "sk", "sl", "sv", "tr", "uk", "zh"));
	public static final List<String> SUPPORTED_LANGUAGE_LIST = Arrays.asList(
			"en",
			"de",
			"fr",
			"es",
			"it",
			"nl",
			"pt",
			"pl",
			"tr",
			"bg",
			"cs",
			"da",
			"el",
			"et",
			"fi",
			"hu",
			"id",
			"lt",
			"lv",
			"no",
			"ro",
			"sk",
			"sl",
			"sv",
			"ru",
			"uk",
			"ja",
			"ko",
			"ar",
			"zh"
	);

	private final String apiKey;

	public DeepL2Translation(String apiKey) {
		this.apiKey = apiKey;
	}

	public static String getBestSourceLanguage(Collection<String> languageIsoSet) {
		if (languageIsoSet.contains("de")) {
			return "de";
		} else if (languageIsoSet.contains("en")) {
			return "en";
		} else if (languageIsoSet.contains("fr")) {
			return "fr";
		} else if (languageIsoSet.contains("es")) {
			return "es";
		} else if (languageIsoSet.contains("nl")) {
			return "nl";
		} else if (languageIsoSet.contains("it")) {
			return "it";
		} else if (languageIsoSet.contains("ru")) {
			return "ru";
		}
		return languageIsoSet.stream().toList().getFirst();
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

	public List<GlossaryLanguagePair> getGlossaries() {
		try {
			Translator translator = new Translator(apiKey);
			return translator.getGlossaryLanguages();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void printGlossaries() {
		try {
			Translator translator = new Translator(apiKey);
			for (GlossaryInfo glossary : translator.listGlossaries()) {
				System.out.println(glossary.getGlossaryId() + ", " + glossary.getName() + ", " + glossary.getSourceLang() + ", " + glossary.getTargetLang() + ", " + glossary.getEntryCount());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
		if (targetLang.equalsIgnoreCase("no")) {
			targetLang = "nb";
		}
		return targetLang;
	}

}
