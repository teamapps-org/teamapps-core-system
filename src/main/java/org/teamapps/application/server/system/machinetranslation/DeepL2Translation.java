package org.teamapps.application.server.system.machinetranslation;

import com.deepl.api.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DeepL2Translation {

	private final String apiKey;

	public DeepL2Translation(String apiKey) {
		this.apiKey = apiKey;
	}

	public String translate(String text, String sourceLang, String targetLang) throws Exception {
		targetLang = fixTargetLang(targetLang);
		Translator translator = new Translator(apiKey);
		TextTranslationOptions options = createOptions();
		TextResult result = translator.translateText(text, sourceLang, targetLang, options);
		return result.getText();
	}

	private static String fixTargetLang(String targetLang) {
		if (targetLang.equals("en")) {
			targetLang = "en-US";
		}
		if (targetLang.equals("pt")) {
			targetLang = "pt-PT";
		}
		return targetLang;
	}

	private TextTranslationOptions createOptions() {
		TextTranslationOptions options = new TextTranslationOptions();
		options.setSentenceSplittingMode(SentenceSplittingMode.NoNewlines);
		options.setPreserveFormatting(true);
		options.setTagHandling("xml");
		options.setOutlineDetection(true);
		options.setFormality(Formality.PreferLess);
		return options;
	}

	public void translatePropertyFile(File inputFile, File outputFile, String sourceLang, String targetLang) throws Exception {
		long time = System.currentTimeMillis();
		targetLang = fixTargetLang(targetLang);
		Translator translator = new Translator(apiKey);
		TextTranslationOptions options = new TextTranslationOptions();
		options.setSentenceSplittingMode(SentenceSplittingMode.NoNewlines);
		options.setPreserveFormatting(true);
		options.setFormality(Formality.Less);

		List<String> lines = Files.readAllLines(inputFile.toPath(), StandardCharsets.UTF_8);
		List<String> keys = new ArrayList<>();
		List<String> values = new ArrayList<>();
		for (String line : lines) {
			if (!line.isBlank() && !line.startsWith("#") && line.contains("=")) {
				String[] parts = line.split("=");
				keys.add(parts[0]);
				values.add(parts[1]);
			}
		}
		List<TextResult> results = translator.translateText(values, sourceLang, targetLang, options);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < keys.size(); i++) {
			String text = results.get(i).getText();
			String key = keys.get(i);
			sb.append(key).append("=").append(text).append("\n");
		}
		Files.writeString(outputFile.toPath(), sb.toString(),StandardCharsets.UTF_8);
		System.out.println("TIME:" + (System.currentTimeMillis() - time));
	}

}
