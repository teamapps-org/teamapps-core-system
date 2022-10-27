package org.teamapps.application.server.system.machinetranslation;

import com.deepl.api.TextResult;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PropertyFileTranslation {

	private final DeepL2Translation translation;

	public PropertyFileTranslation(DeepL2Translation translation) {
		this.translation = translation;
	}

	public PropertyFileTranslation(String key) {
		this.translation = new DeepL2Translation(key);
	}

	public DeepL2Translation getTranslation() {
		return translation;
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
				String topic = key.substring(0, key.indexOf('.'));
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
