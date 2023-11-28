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

import com.deepl.api.TextResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class PropertyFileTranslation {

	private final DeepL2Translation translation;

	public PropertyFileTranslation(DeepL2Translation translation) {
		this.translation = translation;
	}

	public PropertyFileTranslation(String key) {
		this.translation = new DeepL2Translation(key);
	}

	public void printUsage() {
		translation.printUsage();
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

	public void updateTranslationFiles(File path, String sourceLang, File modificationsFolder) throws Exception {
		translation.printUsage();
		File sourceFile = getSourceFile(path, sourceLang);
		String baseName = getBaseName(sourceFile);

		TreeMap<String, String> sourceMap = readPropertyFile(sourceFile);
		Set<String> modifiedKeys = readModifiedKeys(sourceFile);

		for (String targetLang : DeepL2Translation.SUPPORTED_LANGUAGES) {
			if (targetLang.equals(sourceLang)) {
				continue;
			}
			File targetFile = new File(path, baseName + "_" + targetLang + ".properties.txt");
			updateTranslationFile(targetFile, sourceLang, targetLang, sourceMap, modifiedKeys, baseName, modificationsFolder);
		}
		if (sourceLang.equals("de")) {
			File translatedFile = new File(path, baseName + "_en.properties");
			updateOriginalFile(sourceFile, translatedFile, sourceMap);
		}
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
		File modificationFile = new File(modificationsFolder, targetLang + "/" + baseName + "_" + targetLang + ".properties");
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
			return s.substring(24, s.indexOf('.', 25));
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
				String topic = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
				if (key.startsWith("org.teamapps.dictionary.")) {
					int endIndex = key.indexOf('.', 25);
					topic = key.substring(24, endIndex < 0 ? key.length() : endIndex);
				}
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
}
