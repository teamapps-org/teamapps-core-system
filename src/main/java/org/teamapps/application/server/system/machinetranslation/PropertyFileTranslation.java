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

import com.deepl.api.TextResult;
import com.ibm.icu.text.Transliterator;
import org.apache.commons.text.WordUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class PropertyFileTranslation {

	private static final Transliterator TRANSLITERATOR = Transliterator.getInstance("Any-Latin; nfd; [:nonspacing mark:] remove; nfc");

	private final DeepL2Translation translation;

	public PropertyFileTranslation(DeepL2Translation translation) {
		this.translation = translation;
	}

	public PropertyFileTranslation(String key) {
		this.translation = new DeepL2Translation(key);
	}

	public static void main(String[] args) throws Exception {
		if (args == null || args.length < 2) {
			System.out.println("Error: missing key!");
			return;
		}
		String key = args[0];
		File path = new File(args[1]);
		PropertyFileTranslation propertyFileTranslation = new PropertyFileTranslation(key);
		propertyFileTranslation.getTranslation().printUsage();
		propertyFileTranslation.translateFiles(path, DeepL2Translation.SUPPORTED_LANGUAGES);
	}

	private static String getBaseName(File sourceFile) {
		return sourceFile.getName().substring(0, sourceFile.getName().indexOf('_'));
	}

	private static File getSourceFile(File path, String sourceLang) {
		File sourceFile = Arrays.stream(path.listFiles())
				.filter(f -> f.getName().endsWith(".properties"))
				.filter(f -> f.getName().contains("_" + sourceLang + "."))
				.findFirst().orElseThrow();
		return sourceFile;
	}

	public void printUsage() {
		translation.printUsage();
	}

	public DeepL2Translation getTranslation() {
		return translation;
	}

	public void exportTranslationFiles(File path, String sourceLang, File modificationsFolder) throws Exception {
		File sourceFile = getSourceFile(path, sourceLang);
		String baseName = getBaseName(sourceFile);
		for (String targetLang : DeepL2Translation.SUPPORTED_LANGUAGES) {
			File file = new File(path, baseName + "_" + targetLang + ".properties");
			File destFile = new File(modificationsFolder, targetLang + "/" + baseName + "_" + targetLang + ".properties.txt");
			destFile.getParentFile().mkdir();
			Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public void importTranslationFiles(File path, String sourceLang, File inputFolder) throws IOException {
		File sourceFile = getSourceFile(path, sourceLang);
		String baseName = getBaseName(sourceFile);
		for (String targetLang : DeepL2Translation.SUPPORTED_LANGUAGES) {
			File inputFile = new File(inputFolder, targetLang + "/" + baseName + "_" + targetLang + ".properties.txt");
			File file = new File(path, baseName + "_" + targetLang + ".properties");
			if (inputFile.exists() && inputFile.length() > 0) {
				Files.copy(inputFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	public void updateTranslationFiles(File path, String sourceLang, File modificationsFolder) throws Exception {
		translation.printUsage();
		File sourceFile = getSourceFile(path, sourceLang);
		String baseName = getBaseName(sourceFile);
		System.out.println("Translations for: " + baseName);

		TreeMap<String, String> sourceMap = readPropertyFile(sourceFile);
		Set<String> modifiedKeys = readModifiedKeys(sourceFile);

		for (String targetLang : DeepL2Translation.SUPPORTED_LANGUAGES) {
			if (targetLang.equals(sourceLang)) {
				continue;
			}
			File targetFile = new File(path, baseName + "_" + targetLang + ".properties");
			updateTranslationFile(targetFile, sourceLang, targetLang, sourceMap, modifiedKeys, baseName, modificationsFolder);
		}
		if (sourceLang.equals("de")) {
			File translatedFile = new File(path, baseName + "_en.properties");
			updateOriginalFile(sourceFile, translatedFile, sourceMap);
		} else if (sourceLang.equals("en")) {
			File translatedFile = new File(path, baseName + "_de.properties");
			updateOriginalFile(sourceFile, translatedFile, sourceMap);
		}
	}

	public String createDictionary(File path, String sourceLang) throws Exception {
		File sourceFile = getSourceFile(path, sourceLang);
		String baseName = getBaseName(sourceFile);
		TreeMap<String, String> sourceMap = readPropertyFile(sourceFile);

		StringBuilder sb = new StringBuilder();
		String prefix = "org.teamapps.dictionary.";
		for (String key : sourceMap.keySet()) {
			String baseKey = key.substring(prefix.length());
			String[] parts = baseKey.split("\\.");
			if (parts.length == 1) {
				sb.append("public static final String ").append(createConstant(baseKey)).append(" = \"").append(key).append("\";\n");
			} else {
				String newKey = Arrays.stream(parts).map(this::createConstant).collect(Collectors.joining("_"));
				sb.append("public static final String ").append(newKey).append(" = \"").append(key).append("\";\n");
			}
		}

		return sb.toString();
	}

	private void updateOriginalFile(File sourcFile, File translatedFile, TreeMap<String, String> sourceMap) throws IOException {
		TreeMap<String, String> translatedMap = readPropertyFile(translatedFile);
		StringBuilder sb = new StringBuilder();
		String previousTopic = "--";
		for (String key : sourceMap.keySet()) {
			String sourceText = sourceMap.get(key);
			String translatedText = translatedMap.get(key);
			String topic = getTopic(key);
			if (!topic.equals(previousTopic)) {
				sb.append("\n\n");
				previousTopic = topic;
			}
			sb.append("# ").append(translatedText).append("\n");
			sb.append("# original=").append(sourceText).append("\n");
			sb.append(key).append("=").append(sourceText).append("\n\n");
		}
		Files.writeString(sourcFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
	}

	private void updateTranslationFile(File targetFile, String sourceLang, String targetLang, TreeMap<String, String> sourceMap, Set<String> modifiedKeys, String baseName, File modificationsFolder) throws Exception {
		long time = System.currentTimeMillis();
		TreeMap<String, String> targetMap = readPropertyFile(targetFile);
		List<String> keys = new ArrayList<>();
		List<String> values = new ArrayList<>();
		for (String key : sourceMap.keySet()) {
			if (!targetMap.containsKey(key) || modifiedKeys.contains(key)) {
				keys.add(key);
				values.add(sourceMap.get(key));
			}
		}
		File modificationFile = new File(modificationsFolder, targetLang + "/" + baseName + "_" + targetLang + ".properties.txt");
		modificationFile.getParentFile().mkdir();
		if (keys.isEmpty()) {
			System.out.println("Skip language:" + targetLang + ", nothing to translate");
		} else {
			System.out.println("Translating " + keys.size() + " entries for " + targetLang);
			List<TextResult> results = translation.translate(values, sourceLang, targetLang, DeepL2Translation.createDefaultPlainTextOptions());
			if (results.size() != keys.size()) {
				System.out.println("Error: translation result different size:" + results.size() + ", " + keys.size());
				return;
			}
			for (int i = 0; i < keys.size(); i++) {
				String text = results.get(i).getText();
				String key = keys.get(i);
				targetMap.put(key, text);
			}

			StringBuilder sb = new StringBuilder();
			StringBuilder sbMod = new StringBuilder();
			String previousTopic = "--";
			for (String key : sourceMap.keySet()) {
				String sourceText = sourceMap.get(key);
				String translatedText = targetMap.get(key);
				String topic = getTopic(key);
				if (!topic.equals(previousTopic)) {
					sb.append("\n\n");
					previousTopic = topic;
				}
				sb.append("# ").append(sourceText).append("\n");
				sb.append(key).append("=").append(translatedText).append("\n\n");
				if (keys.contains(key)) {
					sbMod.append("# ").append(sourceText).append("\n");
					sbMod.append(key).append("=").append(translatedText).append("\n\n");
				}
			}

			HashSet<String> unusedKeys = new HashSet<>(targetMap.keySet());
			unusedKeys.removeAll(sourceMap.keySet());
			if (!unusedKeys.isEmpty()) {
				System.out.println("Unused keys: " + unusedKeys.size());
				sb.append("\n\n");
				sb.append("# ------------------- UNUSED TRANSLATION KEYS: ------------------------\n");

			}
			for (String key : targetMap.keySet()) {
				if (unusedKeys.contains(key)) {
					String value = targetMap.get(key);
					sb.append(key).append("=").append(value).append("\n");
				}
			}
			Files.writeString(targetFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
			if (!sbMod.isEmpty()) {
				Files.writeString(modificationFile.toPath(), sbMod.toString(), StandardCharsets.UTF_8);
			}
			System.out.println("Translated: " + targetLang + ", time: " + (System.currentTimeMillis() - time));
		}
	}

	private TreeMap<String, String> readPropertyFile(File file) throws IOException {
		TreeMap<String, String> map = new TreeMap<>();
		if (!file.exists()) {
			return map;
		}
		List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
		for (String line : lines) {
			if (!line.isBlank() && !line.startsWith("#") && line.contains("=")) {
				int pos = line.indexOf('=');
				String key = line.substring(0, pos);
				String value = line.substring(pos + 1);
				map.put(key, value);
			}
		}
		return map;
	}

	private Set<String> readModifiedKeys(File file) throws IOException {
		Set<String> modifiedKeys = new HashSet<>();
		List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
		String originalValue = null;
		for (String line : lines) {
			if (!line.isBlank() && line.contains("=")) {
				if (line.startsWith("# original=")) {
					originalValue = line.substring("# original=".length());
				} else {
					if (originalValue != null) {
						int pos = line.indexOf('=');
						String key = line.substring(0, pos);
						String value = line.substring(pos + 1);
						if (!originalValue.equals(value)) {
							modifiedKeys.add(key);
						}
					}
					originalValue = null;
				}
			} else {
				originalValue = null;
			}
		}
		return modifiedKeys;
	}

	private String getTopic(String s) {
		if (s == null || s.isBlank() || !s.contains(".")) {
			return s;
		}
		if (s.startsWith("org.teamapps.dictionary.")) {
			int borderPos = s.indexOf("=");
			if (borderPos < 0) return s;
			int pos = s.substring(0, borderPos).lastIndexOf('.');
			if (pos < 0) {
				return "dictionary";
			} else {
				return s.substring(24, s.indexOf('.', 25));
			}
		} else {
			return s.substring(0, s.indexOf('.'));
		}
	}

	private void translateFiles(File path, Collection<String> targetLanguages) throws Exception {
		File propertyFile = Arrays.stream(path.listFiles()).filter(f -> f.getName().endsWith(".properties")).findFirst().orElse(null);
		String fileName = propertyFile.getName();
		String baseName = fileName.substring(0, fileName.indexOf('_'));
		String sourceLang = fileName.substring(baseName.length() + 1, fileName.indexOf('.'));
		for (String targetLang : targetLanguages) {
			targetLang = targetLang.toLowerCase();
			File outPutFile = new File(path, baseName + "_" + targetLang + ".properties");
			translatePropertyFile(propertyFile, outPutFile, sourceLang, targetLang);
		}

	}

	public void translatePropertyFile(File inputFile, File outputFile, String sourceLang, String targetLang) throws Exception {
		if (sourceLang.equalsIgnoreCase(targetLang)) {
			System.out.println("Same language:" + sourceLang + "->" + targetLang);
			return;
		}
		if (outputFile.exists()) {
			System.out.println("Outputfile exists:" + outputFile.getName());
			return;
		}
		long time = System.currentTimeMillis();
		List<String> lines = Files.readAllLines(inputFile.toPath(), StandardCharsets.UTF_8);
		List<String> keys = new ArrayList<>();
		List<String> values = new ArrayList<>();
		List<String> topics = new ArrayList<>();
		for (String line : lines) {
			if (!line.isBlank() && !line.startsWith("#") && line.contains("=")) {
				int pos = line.indexOf('=');
				String key = line.substring(0, pos);
				String value = line.substring(pos + 1);
				String topic = getTopic(key);
				keys.add(key);
				values.add(value);
				topics.add(topic);
			}
		}
		List<TextResult> results = translation.translate(values, sourceLang, targetLang, DeepL2Translation.createDefaultPlainTextOptions());
		if (results.size() != keys.size()) {
			System.out.println("Error: translation result different size:" + results.size() + ", " + keys.size());
		}
		StringBuilder sb = new StringBuilder();
		String previousTopic = "--";
		for (int i = 0; i < keys.size(); i++) {
			String text = results.get(i).getText();
			String key = keys.get(i);
			String topic = topics.get(i);
			if (topic.equals(previousTopic)) {
				sb.append("\n");
				previousTopic = topic;
			}
			sb.append("# ").append(values.get(i)).append("\n");
			sb.append(key).append("=").append(text).append("\n");
		}
		Files.writeString(outputFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
		System.out.println("TIME:" + (System.currentTimeMillis() - time) + ", languages:" + sourceLang + "->" + targetLang);
	}


	private String cleanValue(String value) {
		value = TRANSLITERATOR.transliterate(value);
		value = WordUtils.capitalize(value);
		value = value.replaceAll("\\.\\.\\.", "___");
		value = removeNonAnsi(value);
		value = value.replaceAll(" ", "");
		value = value.substring(0, 1).toLowerCase() + value.substring(1);
		return value;
	}

	private String createConstant(String value) {
		if (value.contains("_")) {
			return value;
		}
		value = cleanValue(value);
		value = value.replaceAll("(.)(\\p{Upper})", "$1_$2").toUpperCase();
		if (value.length() > 36) {
			value = "SENTENCE_" + value.substring(0, 35) + "__";
		}
		return value;
	}

	public String removeNonAnsi(String s) {
		if (s == null) return null;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int code = c;
			if (c == '_' || c == ' ') {
				sb.append(c);
			} else if (code > 64 && code < 91) {
				sb.append(c);
			} else if (code > 96 && code < 123) {
				sb.append(c);
			} else if (code > 47 && code < 58) {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
