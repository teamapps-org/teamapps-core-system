package org.teamapps.application.server.system.machinetranslation;

import com.deepl.api.*;

import java.util.*;

public class DeepL2Translation {
	public static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Arrays.asList("BG", "CS", "DA", "DE", "EL", "EN", "ES", "ET", "FI", "FR", "HU", "ID", "IT", "JA", "LT", "LV", "NL", "PL", "PT", "RO", "RU", "SK", "SL", "SV", "TR", "UK", "ZH"));

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
