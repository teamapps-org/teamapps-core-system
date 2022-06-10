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
package org.teamapps.application.server.system.localization;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.localization.Language;
import org.teamapps.application.api.localization.LocalizationData;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.config.LocalizationConfig;
import org.teamapps.application.server.system.machinetranslation.TranslationService;
import org.teamapps.application.tools.ChangeCounter;
import org.teamapps.application.tools.KeyCompare;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.enumeration.EnumFilterType;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.universaldb.index.text.TextFilter;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class LocalizationUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static void synchronizeLocalizationData(LocalizationData localizationData, Application application, LocalizationKeyType localizationKeyType, LocalizationConfig localizationConfig) {
		Map<String, Map<String, String>> localizationMapByKey = localizationData.createLocalizationMapByKey();
		Set<String> machineTranslatedLanguages = localizationData.getMachineTranslatedLanguages();
		int appIdFilter = 0;
		if (application != null) {
			appIdFilter = application.getId();
		}
		List<LocalizationKey> localizationKeys = LocalizationKey.filter().application(NumericFilter.equalsFilter(appIdFilter)).execute();
		KeyCompare<String, LocalizationKey> keyCompare = new KeyCompare<>(localizationMapByKey.keySet(), localizationKeys, s -> s, LocalizationKey::getKey);
		List<String> newKeys = keyCompare.getAEntriesNotInB();

		LocalizationTopic topic = getTopic(localizationKeyType, application);
		for (String key : newKeys) {
			LocalizationKey localizationKey = LocalizationKey.create()
					.setApplication(application)
					.setLocalizationKeyType(localizationKeyType)
					.setTopics(topic)
					.setUsed(true)
					.setKey(key)
					.save();
			Map<String, String> translations = localizationMapByKey.get(key);
			for (Map.Entry<String, String> entry : translations.entrySet()) {
				String language = entry.getKey();
				String value = entry.getValue();
				if (machineTranslatedLanguages.contains(language)) {
					LocalizationValue.create()
							.setLocalizationKey(localizationKey)
							.setLanguage(language)
							.setMachineTranslation(value)
							.setCurrentDisplayValue(value)
							.setMachineTranslationState(MachineTranslationState.OK)
							.setTranslationState(TranslationState.TRANSLATION_REQUESTED)
							.setTranslationVerificationState(TranslationVerificationState.NOT_YET_TRANSLATED)
							.save();
				} else {
					LocalizationValue.create()
							.setLocalizationKey(localizationKey)
							.setLanguage(language)
							.setOriginal(value)
							.setCurrentDisplayValue(value)
							.setMachineTranslationState(MachineTranslationState.NOT_NECESSARY)
							.setTranslationState(TranslationState.NOT_NECESSARY)
							.setTranslationVerificationState(TranslationVerificationState.NOT_NECESSARY)
							.save();
				}
			}
		}

		List<LocalizationKey> removedKeys = keyCompare.getBEntriesNotInA();
		removedKeys.forEach(key -> key.setUsed(false).save());

		List<String> existingKeys = keyCompare.getAEntriesInB();
		for (String key : existingKeys) {
			Map<String, String> translations = localizationMapByKey.get(key);
			LocalizationKey localizationKey = keyCompare.getB(key);

			if (!localizationKey.isUsed()) {
				localizationKey.setUsed(true).save();
			}

			KeyCompare<String, LocalizationValue> languageCompare = new KeyCompare<>(translations.keySet(), localizationKey.getLocalizationValues(), s -> s, LocalizationValue::getLanguage);
			if (languageCompare.isDifferent()) {
				List<String> newLanguages = languageCompare.getAEntriesNotInB();
				newLanguages.forEach(language -> {
					String value = translations.get(language);
					if (machineTranslatedLanguages.contains(language)) {
						LocalizationValue.create()
								.setLocalizationKey(localizationKey)
								.setLanguage(language)
								.setMachineTranslation(value)
								.setCurrentDisplayValue(value)
								.setMachineTranslationState(MachineTranslationState.OK)
								.setTranslationState(TranslationState.TRANSLATION_REQUESTED)
								.setTranslationVerificationState(TranslationVerificationState.NOT_YET_TRANSLATED)
								.save();
					} else {
						LocalizationValue.create()
								.setLocalizationKey(localizationKey)
								.setLanguage(language)
								.setOriginal(value)
								.setCurrentDisplayValue(value)
								.setMachineTranslationState(MachineTranslationState.NOT_NECESSARY)
								.setTranslationState(TranslationState.NOT_NECESSARY)
								.setTranslationVerificationState(TranslationVerificationState.NOT_NECESSARY)
								.save();
					}
				});
			}
			//checking all existing values if the original changed
			for (LocalizationValue localizationValue : languageCompare.getBEntriesInA()) {
				String language = localizationValue.getLanguage();
				if (machineTranslatedLanguages.contains(language)) {
					String applicationTranslation = translations.get(language);
					String machineTranslation = localizationValue.getMachineTranslation();
					if (applicationTranslation != null &&
							!applicationTranslation.isBlank() &&
							machineTranslation != null &&
							!applicationTranslation.equals(machineTranslation)) {
						localizationValue
								.setMachineTranslation(applicationTranslation)
								.setMachineTranslationState(MachineTranslationState.OK);
						if (localizationValue.getTranslation() == null) {
							localizationValue.setCurrentDisplayValue(applicationTranslation);
						}
						localizationValue.save();
						LOGGER.info("Update machine translation, key: {}, old: {}, new: {}", localizationValue.getLocalizationKey().getKey(), machineTranslation, applicationTranslation);
					}

				} else {
					String original = translations.get(language);
					if (original != null &&
							!original.isBlank() &&
							localizationValue.getOriginal() != null &&
							!original.equals(localizationValue.getOriginal())
					) {
						LOGGER.info("Update original localization, key: {}, old: {}, new: {}", localizationValue.getLocalizationKey().getKey(), localizationValue.getOriginal(), original);
						localizationValue.setOriginal(original).setCurrentDisplayValue(original).save();
						localizationValue.getLocalizationKey().getLocalizationValues().stream()
								.filter(value -> !value.equals(localizationValue))
								.filter(value -> value.getOriginal() == null)
								.filter(value -> value.getAdminLocalOverride() == null)
								.filter(value -> value.getAdminKeyOverride() == null)
								.forEach(value -> value
										.setMachineTranslationState(MachineTranslationState.TRANSLATION_REQUESTED)
										.setTranslationState(TranslationState.TRANSLATION_REQUESTED)
										.setTranslationVerificationState(TranslationVerificationState.NOT_YET_TRANSLATED)
										.save());
					}
				}
			}
		}

		createRequiredLanguageValues(localizationKeys, localizationConfig);
	}

	public static int createRequiredLanguageValues(List<LocalizationKey> localizationKeys, LocalizationConfig localizationConfig) {
		//create translation requests
		List<String> requiredLanguages = localizationConfig.getRequiredLanguages();
		int countNewEntries = 0;
		for (LocalizationKey key : localizationKeys) {
			Map<String, LocalizationValue> valueByLanguage = key.getLocalizationValues().stream().collect(Collectors.toMap(LocalizationValue::getLanguage, v -> v));
			for (String language : requiredLanguages) {
				if (!valueByLanguage.containsKey(language)) {
					LocalizationValue.create()
							.setLocalizationKey(key)
							.setLanguage(language)
							.setMachineTranslationState(MachineTranslationState.TRANSLATION_REQUESTED)
							.setTranslationState(TranslationState.TRANSLATION_REQUESTED)
							.setTranslationVerificationState(TranslationVerificationState.NOT_YET_TRANSLATED)
							.save();
					countNewEntries++;
				}
			}
		}
		return countNewEntries;
	}

	public static void translateAllApplicationValues(TranslationService translationService, Application application, LocalizationConfig localizationConfig) {
		if (translationService == null || localizationConfig == null) {
			return;
		}
		Set<String> allowedSourceTranslationLanguages = new HashSet<>(localizationConfig.getAllowedSourceLanguages());
		List<LocalizationValue> translationRequests = LocalizationValue
				.filter()
				.machineTranslationState(EnumFilterType.EQUALS, MachineTranslationState.TRANSLATION_REQUESTED)
				.original(TextFilter.emptyFilter())
				.machineTranslation(TextFilter.emptyFilter())
				.execute().stream()
				.filter(value -> value.getLocalizationKey().getApplication() != null && value.getLocalizationKey().getApplication().equals(application))
				.collect(Collectors.toList());
		LOGGER.info("Application translation requests:" + translationRequests.size() + ", app:" + application.getName());
		ExecutorService executor = Executors.newFixedThreadPool(10);
		translationRequests.forEach(localizationValue -> executor.submit(() -> translateLocalizationValue(localizationValue, translationService, allowedSourceTranslationLanguages)));
		executor.shutdown();
	}

	public static void translateAllDictionaryValues(TranslationService translationService, LocalizationConfig localizationConfig) {
		if (translationService == null || localizationConfig == null) {
			return;
		}
		Set<String> allowedSourceTranslationLanguages = new HashSet<>(localizationConfig.getAllowedSourceLanguages());
		List<LocalizationValue> translationRequests = LocalizationValue
				.filter()
				.machineTranslationState(EnumFilterType.EQUALS, MachineTranslationState.TRANSLATION_REQUESTED)
				.original(TextFilter.emptyFilter())
				.machineTranslation(TextFilter.emptyFilter())
				.execute().stream()
				.filter(value -> value.getLocalizationKey().getLocalizationKeyType() == LocalizationKeyType.DICTIONARY_KEY)
				.collect(Collectors.toList());
		ExecutorService executor = Executors.newFixedThreadPool(10);
		translationRequests.forEach(localizationValue -> executor.submit(() -> translateLocalizationValue(localizationValue, translationService, allowedSourceTranslationLanguages)));
		executor.shutdown();
	}

	public static int translateAllValues(TranslationService translationService, LocalizationConfig localizationConfig) {
		if (translationService == null || localizationConfig == null) {
			return -1;
		}
		Set<String> allowedSourceTranslationLanguages = new HashSet<>(localizationConfig.getAllowedSourceLanguages());
		List<LocalizationValue> translationRequests = LocalizationValue.filter()
				.machineTranslationState(EnumFilterType.EQUALS, MachineTranslationState.TRANSLATION_REQUESTED)
				.original(TextFilter.emptyFilter())
				.machineTranslation(TextFilter.emptyFilter())
				.execute();
		ExecutorService executor = Executors.newFixedThreadPool(10);
		translationRequests.forEach(localizationValue -> executor.submit(() -> translateLocalizationValue(localizationValue, translationService, allowedSourceTranslationLanguages)));
		executor.shutdown();
		return translationRequests.size();
	}

	public static void translateLocalizationValue(LocalizationValue missingTranslationValue, TranslationService translationService, Set<String> allowedSourceTranslationLanguages) {
		LocalizationValue adminValue = missingTranslationValue.getLocalizationKey().getLocalizationValues().stream()
				.filter(value -> value.getAdminKeyOverride() != null)
				.filter(value -> allowedSourceTranslationLanguages.contains(value.getLanguage()))
				.findFirst()
				.orElse(null);
		if (adminValue != null && translationService.canTranslate(adminValue.getLanguage(), missingTranslationValue.getLanguage())) {
			String translation = translationService.translate(adminValue.getAdminKeyOverride(), adminValue.getLanguage(), missingTranslationValue.getLanguage());
			if (translation != null) {
				translation = firstUpperIfSourceUpper(adminValue.getAdminKeyOverride(), translation);
				LOGGER.info("Translate admin key (" + adminValue.getLanguage() + "->" + missingTranslationValue.getLanguage() + "): " + adminValue.getAdminKeyOverride() + " -> " + translation);
				missingTranslationValue
						.setMachineTranslation(translation)
						.setMachineTranslationState(MachineTranslationState.OK)
						.setCurrentDisplayValue(getDisplayValue(missingTranslationValue))
						.save();
			} else {
				LOGGER.warn("Missing translation admin key result (" + adminValue.getLanguage() + "->" + missingTranslationValue.getLanguage() + "): " + adminValue.getAdminKeyOverride() + " -> " + translation);
			}
			return;
		}

		Map<String, LocalizationValue> localizationValueByLanguage = missingTranslationValue.getLocalizationKey().getLocalizationValues().stream()
				.filter(value -> !value.equals(missingTranslationValue))
				.filter(value -> value.getOriginal() != null)
				.collect(Collectors.toMap(LocalizationValue::getLanguage, v -> v));

		for (String language : allowedSourceTranslationLanguages) {
			LocalizationValue sourceValue = localizationValueByLanguage.get(language);
			if (sourceValue != null && translationService.canTranslate(language, missingTranslationValue.getLanguage())) {
				String translationSourceText = getTranslationSourceText(sourceValue);
				if (translationSourceText != null && !translationSourceText.isBlank()) {
					String translation = translationService.translate(translationSourceText, language, missingTranslationValue.getLanguage());
					if (translation != null) {
						translation = firstUpperIfSourceUpper(translationSourceText, translation);
						LOGGER.info("Translate (" + language + "->" + missingTranslationValue.getLanguage() + "): " + translationSourceText + " -> " + translation);
						missingTranslationValue
								.setMachineTranslation(translation)
								.setMachineTranslationState(MachineTranslationState.OK)
								.setCurrentDisplayValue(getDisplayValue(missingTranslationValue))
								.save();
					} else {
						LOGGER.warn("Missing translation result (" + language + "->" + missingTranslationValue.getLanguage() + "): " + translationSourceText + " -> " + translation);
					}
					break;
				}
			}
		}
	}

	private static String getTranslationSourceText(LocalizationValue localizationValue) {
		String value = localizationValue.getAdminKeyOverride();
		if (value == null) {
			value = localizationValue.getAdminLocalOverride();
		}
		if (value == null) {
			value = localizationValue.getOriginal();
		}
		return value;
	}

	private static String getDisplayValue(LocalizationValue localizationValue) {
		String value = localizationValue.getAdminKeyOverride();
		if (value == null) {
			value = localizationValue.getAdminLocalOverride();
		}
		if (value == null) {
			value = localizationValue.getOriginal();
		}
		if (value == null) {
			value = localizationValue.getTranslation();
		}
		if (value == null) {
			value = localizationValue.getMachineTranslation();
		}
		return value;
	}

	private static String firstUpperIfSourceUpper(String source, String text) {
		if (source == null || text == null || source.isEmpty() || text.isEmpty()) {
			return text;
		}
		char c = source.substring(0, 1).charAt(0);
		if (Character.isUpperCase(c)) {
			return text.substring(0, 1).toUpperCase() + text.substring(1);
		} else {
			return text;
		}
	}

	private static LocalizationTopic getTopic(LocalizationKeyType keyType, Application application) {
		return switch (keyType) {
			case APPLICATION_RESOURCE_KEY -> getOrCreateTopic(application.getName(), application.getIcon(), application);
			case DICTIONARY_KEY -> getOrCreateTopic("Dictionary", IconUtils.encodeNoStyle(ApplicationIcons.DICTIONARY), application);
			case SYSTEM_KEY -> getOrCreateTopic("System", IconUtils.encodeNoStyle(ApplicationIcons.SYSTEM), application);
			case REPORTING_KEY -> getOrCreateTopic("Reporting", IconUtils.encodeNoStyle(ApplicationIcons.FORM), application);
		};
	}

	private static LocalizationTopic getOrCreateTopic(String name, String icon, Application application) {
		LocalizationTopic topic = LocalizationTopic.filter().title(TextFilter.textEqualsFilter(name)).executeExpectSingleton();
		if (topic == null) {
			topic = LocalizationTopic.create()
					.setTitle(name)
					.setApplication(application)
					.setIcon(icon)
					.save();
		}
		return topic;
	}

	public static File createTranslationResourceFiles() throws IOException {
		Map<String, List<LocalizationValue>> valuesByDomain = LocalizationValue.getAll().stream()
				.filter(value -> value.getLocalizationKey().getKey() != null)
				.filter(value -> value.getCurrentDisplayValue() != null)
				.collect(Collectors.groupingBy(value -> {
					if (value.getLocalizationKey().getApplication() != null) {
						return value.getLocalizationKey().getApplication().getName();
					} else {
						return value.getLocalizationKey().getLocalizationKeyType().name();
					}
				}));

		File zipFile = File.createTempFile("temp", ".zip");
		FileOutputStream fos = new FileOutputStream(zipFile);
		ZipOutputStream zos = new ZipOutputStream(fos);

		for (Map.Entry<String, List<LocalizationValue>> domainEntry : valuesByDomain.entrySet()) {
			String applicationOrType = domainEntry.getKey();
			zos.putNextEntry(new ZipEntry(applicationOrType + "/"));
			zos.closeEntry();

			Map<String, List<LocalizationValue>> valueMap = domainEntry.getValue().stream().collect(Collectors.groupingBy(LocalizationValue::getLanguage));
			for (Map.Entry<String, List<LocalizationValue>> entry : valueMap.entrySet()) {
				String language = entry.getKey();
				String fileName = applicationOrType;
				if (applicationOrType.equals("DICTIONARY_KEY")) {
					fileName = "dictionary";
				}
				zos.putNextEntry(new ZipEntry(applicationOrType + "/" + fileName + "_" + language + ".properties"));
				List<LocalizationValue> values = entry.getValue().stream().sorted(Comparator.comparing(o -> o.getLocalizationKey().getKey())).collect(Collectors.toList());
				StringBuilder sb = new StringBuilder();
				for (LocalizationValue value : values) {
					sb.append(value.getLocalizationKey().getKey())
							.append("=")
							.append(value.getCurrentDisplayValue() != null ? value.getCurrentDisplayValue().replace("\r", "").replace("\n", "\\n") : null)
							.append("\n");
				}
				zos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
			}
		}
		zos.close();
		fos.close();
		return zipFile;
	}

	public static File createTranslationExport(Application application) throws IOException {
		Stream<LocalizationKey> keyStream = LocalizationKey.getAll().stream()
				.filter(key -> key.getKey() != null);
		if (application != null) {
			keyStream = keyStream
					.filter(key -> key.getApplication() != null)
					.filter(key -> key.getApplication().equals(application));
		}
		keyStream = keyStream.sorted(Comparator.comparing(LocalizationKey::getKey));
		List<LocalizationKey> keys = keyStream.collect(Collectors.toList());

		File zipFile = File.createTempFile("temp", ".zip");
		FileOutputStream fos = new FileOutputStream(zipFile);
		ZipOutputStream zos = new ZipOutputStream(fos);

		zos.putNextEntry(new ZipEntry("LocalizationKey.csv"));
		CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(zos), CSVFormat
				.DEFAULT
				.withHeader("key", "application", "localizationKeyFormat", "localizationKeyType", "comments"));
		for (LocalizationKey key : keys) {
			printer.printRecord(
					key.getKey(),
					key.getApplication() != null ? key.getApplication().getName() : null,
					key.getLocalizationKeyFormat() != null ? key.getLocalizationKeyFormat().name() : null,
					key.getLocalizationKeyType() != null ? key.getLocalizationKeyType().name() : null,
					key.getComments()
			);
		}
		printer.flush();
		zos.closeEntry();
		zos.putNextEntry(new ZipEntry("LocalizationValue.csv"));

		printer = new CSVPrinter(new OutputStreamWriter(zos),
				CSVFormat
						.DEFAULT
						.withHeader("key", "application", "language", "original", "machineTranslation", "translation", "adminLocalOverride", "adminKeyOverride", "currentDisplayValue", "notes", "machineTranslationState", "translationState", "translationVerificationState")
		);
		for (LocalizationKey key : keys) {
			for (LocalizationValue value : key.getLocalizationValues()) {
				printer.printRecord(
						value.getLocalizationKey().getKey(),
						value.getLocalizationKey().getApplication() != null ? value.getLocalizationKey().getApplication().getName() : null,
						value.getLanguage(),
						value.getOriginal(),
						value.getMachineTranslation(),
						value.getTranslation(),
						value.getAdminLocalOverride(),
						value.getAdminKeyOverride(),
						value.getCurrentDisplayValue(),
						value.getNotes(),
						value.getMachineTranslationState() != null ? value.getMachineTranslationState().name() : null,
						value.getTranslationState() != null ? value.getTranslationState().name() : null,
						value.getTranslationVerificationState() != null ? value.getTranslationVerificationState().name() : null
				);
			}
		}
		printer.flush();
		zos.closeEntry();
		zos.close();
		fos.close();
		LOGGER.info("User exported translation data: " + keys.size() + " keys.");
		return zipFile;
	}

	public static void importTranslationExport(File file, Application application) throws IOException {
		ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
		ZipEntry zipEntry = zis.getNextEntry();
		Map<String, LocalizationKey> keyApplicationMap = LocalizationKey.getAll().stream().collect(Collectors.toMap(key -> key.getKey() + ":" + (key.getApplication() != null ? key.getApplication().getName() : ""), key -> key));
		Map<String, Application> applicationMap = Application.getAll().stream().collect(Collectors.toMap(Application::getName, app -> app));
		Map<String, LocalizationKeyFormat> localizationKeyFormatMap = Arrays.stream(LocalizationKeyFormat.values()).collect(Collectors.toMap(Enum::name, e -> e));
		Map<String, LocalizationKeyType> localizationKeyTypeMap = Arrays.stream(LocalizationKeyType.values()).collect(Collectors.toMap(Enum::name, e -> e));
		Map<String, MachineTranslationState> machineTranslationStateMap = Arrays.stream(MachineTranslationState.values()).collect(Collectors.toMap(Enum::name, e -> e));
		Map<String, TranslationState> translationStateMap = Arrays.stream(TranslationState.values()).collect(Collectors.toMap(Enum::name, e -> e));
		Map<String, TranslationVerificationState> translationVerificationStateMap = Arrays.stream(TranslationVerificationState.values()).collect(Collectors.toMap(Enum::name, e -> e));
		Map<String, LocalizationValue> valueByKeyAppAndLanguage = LocalizationValue.getAll().stream().collect(Collectors.toMap(value -> value.getLocalizationKey().getKey() + ":" + (value.getLocalizationKey().getApplication() != null ? value.getLocalizationKey().getApplication().getName() : "") + ":" + value.getLanguage(), value -> value));
		ChangeCounter changeCounter = new ChangeCounter();

		while (zipEntry != null) {
			if (zipEntry.getName().equals("LocalizationKey.csv")) {
				CSVParser parser = CSVFormat.DEFAULT
						.withHeader("key", "application", "localizationKeyFormat", "localizationKeyType", "comments")
						.withFirstRecordAsHeader()
						.parse(new InputStreamReader(zis));
				for (CSVRecord csvRecord : parser) {
					String key = csvRecord.get("key");
					Application app = applicationMap.get(csvRecord.get("application"));
					String keyApp = key + ":" + (app != null ? app.getName() : "");
					LocalizationKeyFormat localizationKeyFormat = localizationKeyFormatMap.get(csvRecord.get("localizationKeyFormat"));
					LocalizationKeyType localizationKeyType = localizationKeyTypeMap.get(csvRecord.get("localizationKeyType"));
					String comments = csvRecord.get("comments");
					if (application != null && !application.equals(app)) {
						changeCounter.error("key");
						continue;
					}
					changeCounter.updateOrCreate("key", keyApplicationMap.containsKey(keyApp));
					LocalizationKey localizationKey = keyApplicationMap.computeIfAbsent(keyApp, keyValue -> LocalizationKey.create());
					localizationKey
							.setApplication(app)
							.setLocalizationKeyFormat(localizationKeyFormat)
							.setLocalizationKeyType(localizationKeyType)
							.setComments(comments)
							.setUsed(true)
							.save();
				}
			} else if (zipEntry.getName().equals("LocalizationValue.csv")) {
				CSVParser parser = CSVFormat.DEFAULT
						.withHeader("key", "application", "language", "original", "machineTranslation", "translation", "adminLocalOverride", "adminKeyOverride", "currentDisplayValue", "notes", "machineTranslationState", "translationState", "translationVerificationState")
						.withFirstRecordAsHeader()
						.parse(new InputStreamReader(zis));
				for (CSVRecord csvRecord : parser) {
					LocalizationKey key = keyApplicationMap.get(csvRecord.get("key") + ":" + csvRecord.get("application"));
					String language = csvRecord.get("language");
					String original = csvRecord.get("original");
					String machineTranslation = csvRecord.get("machineTranslation");
					String translation = csvRecord.get("translation");
					String adminLocalOverride = csvRecord.get("adminLocalOverride");
					String adminKeyOverride = csvRecord.get("adminKeyOverride");
					String currentDisplayValue = csvRecord.get("currentDisplayValue");
					String notes = csvRecord.get("notes");
					MachineTranslationState machineTranslationState = machineTranslationStateMap.getOrDefault(csvRecord.get("machineTranslationState"), MachineTranslationState.TRANSLATION_REQUESTED);
					TranslationState translationState = translationStateMap.getOrDefault(csvRecord.get("translationState"), TranslationState.TRANSLATION_REQUESTED);
					TranslationVerificationState translationVerificationState = translationVerificationStateMap.getOrDefault(csvRecord.get("translationVerificationState"), TranslationVerificationState.NOT_YET_TRANSLATED);
					if (key == null || language == null) {
						changeCounter.error("value");
						continue;
					}
					String keyAppLangKey = key.getKey() + ":" + (key.getApplication() != null ? key.getApplication().getName() : null) + ":" + language;
					changeCounter.updateOrCreate("value", valueByKeyAppAndLanguage.containsKey(keyAppLangKey));
					LocalizationValue localizationValue = valueByKeyAppAndLanguage.computeIfAbsent(keyAppLangKey, s -> LocalizationValue.create());
					localizationValue
							.setLocalizationKey(key)
							.setLanguage(language)
							.setOriginal(original)
							.setMachineTranslation(machineTranslation)
							.setTranslation(translation)
							.setAdminLocalOverride(adminLocalOverride)
							.setAdminKeyOverride(adminKeyOverride)
							.setCurrentDisplayValue(currentDisplayValue)
							.setNotes(notes)
							.setMachineTranslationState(machineTranslationState)
							.setTranslationState(translationState)
							.setTranslationVerificationState(translationVerificationState)
							.save();
				}
			}
			zipEntry = zis.getNextEntry();
		}
		zis.closeEntry();
		zis.close();
		LOGGER.info("User imported localization data: " + changeCounter.getResults());
	}


	public static File createTranslationTemplateFile() throws IOException {
		File csvFile = File.createTempFile("temp", ".csv");
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(csvFile));
		List<String> languages = LocalizationValue.getAll().stream().map(LocalizationValue::getLanguage).collect(Collectors.toSet()).stream().sorted().collect(Collectors.toList());

		//todo use apache csv
		StringBuilder header = new StringBuilder();
		header.append("Key,Application,Type");
		for (String language : languages) {
			header.append(",").append(StringEscapeUtils.escapeCsv(language));
		}
		header.append("\n");
		bos.write(header.toString().getBytes(StandardCharsets.UTF_8));

		for (LocalizationKey key : LocalizationKey.getAll()) {
			Map<String, LocalizationValue> valueByLanguage = key.getLocalizationValues().stream().collect(Collectors.toMap(LocalizationValue::getLanguage, v -> v));
			StringBuilder sb = new StringBuilder();
			String app = key.getApplication() != null ? key.getApplication().getName() : null;
			sb.append(StringEscapeUtils.escapeCsv(key.getKey())).append(",");
			sb.append(StringEscapeUtils.escapeCsv(app)).append(",");
			sb.append(StringEscapeUtils.escapeCsv(key.getLocalizationKeyType().name())).append(",");
			for (String language : languages) {
				LocalizationValue value = valueByLanguage.get(language);
				String v = value != null ? value.getOriginal() : null;
				sb.append(StringEscapeUtils.escapeCsv(v)).append(",");
			}
			sb.append("\n");
			bos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
		}
		bos.close();
		return csvFile;
	}

	public static String importLocalizationKeyFile(File xlsxFile, Application application, LocalizationConfig localizationConfig) throws IOException {
		if (application == null) {
			return "ERROR no application selected!";
		}
		XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(xlsxFile));
		XSSFSheet sheet = workbook.getSheetAt(0);
		StringBuilder errors = new StringBuilder();
		Map<String, LocalizationKey> keyMap = LocalizationKey.filter().application(NumericFilter.equalsFilter(application.getId())).execute().stream().collect(Collectors.toMap(LocalizationKey::getKey, key -> key));
		int countCreatedKeys = 0;
		int countUpdatedKeys = 0;
		int countCreatedValues = 0;
		int countUpdatedValues = 0;

		Iterator<Row> rowIterator = sheet.iterator();
		if (rowIterator.hasNext()) {
			//header row
			rowIterator.next();
		}
		List<LocalizationKey> createdKeys = new ArrayList<>();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			String key = getCellText(row.getCell(0));
			String language = getCellText(row.getCell(1));
			int translationMode = getCellIntValue(row.getCell(2));
			String text = getCellText(row.getCell(3));
			if (key != null && !key.isBlank() && text != null && !text.isBlank()) {
				key = key.trim();
				Language languageByIsoCode = Language.getLanguageByIsoCode(language);
				if (languageByIsoCode == null) {
					errors.append("Error for key: ").append(key).append(" unknown language code:").append(language).append("\n");
					continue;
				}
				if (translationMode < 0 || translationMode > 3) {
					errors.append("Error for key: ").append(key).append(" unknown translation mode:").append(translationMode).append("\n");
					continue;
				}

				LocalizationKey localizationKey = keyMap.get(key);
				if (localizationKey == null) {
					localizationKey = LocalizationKey.create();
					localizationKey
							.setApplication(application)
							.setKey(key)
							.setLocalizationKeyType(LocalizationKeyType.REPORTING_KEY)
							.setUsed(true)
							.save();
					createdKeys.add(localizationKey);
					keyMap.put(key, localizationKey);
					countCreatedKeys++;
				} else {
					countUpdatedKeys++;
				}
				LocalizationValue localizationValue = localizationKey.getLocalizationValues().stream().filter(value -> value.getLanguage().equals(language)).findFirst().orElse(null);
				if (localizationValue == null) {
					localizationValue = LocalizationValue.create()
							.setLocalizationKey(localizationKey)
							.setLanguage(language)
							.save();
					countCreatedValues++;
				} else {
					countUpdatedValues++;
				}
				switch (translationMode) {
					//original
					case 0 -> {
						localizationValue
								.setOriginal(text)
								.setMachineTranslationState(MachineTranslationState.NOT_NECESSARY)
								.setTranslationState(TranslationState.NOT_NECESSARY)
								.setTranslationVerificationState(TranslationVerificationState.NOT_NECESSARY)
								.setCurrentDisplayValue(text)
								.save();
					}
					//translation
					case 1 -> {
						localizationValue
								.setTranslation(text)
								.setMachineTranslationState(MachineTranslationState.NOT_NECESSARY)
								.setTranslationState(TranslationState.OK)
								.setTranslationVerificationState(TranslationVerificationState.VERIFICATION_REQUESTED)
								.setCurrentDisplayValue(text)
								.save();
					}
					//verified translation
					case 2 -> {
						localizationValue
								.setTranslation(text)
								.setMachineTranslationState(MachineTranslationState.NOT_NECESSARY)
								.setTranslationState(TranslationState.OK)
								.setTranslationVerificationState(TranslationVerificationState.OK)
								.setCurrentDisplayValue(text)
								.save();
					}
					//admin local override
					case 3 -> {
						localizationValue
								.setAdminLocalOverride(text)
								.setCurrentDisplayValue(text);
						if (localizationValue.getMachineTranslationState() == null) {
							localizationValue.setMachineTranslationState(MachineTranslationState.NOT_NECESSARY);
						}
						if (localizationValue.getTranslationState() == null) {
							localizationValue.setTranslationState(TranslationState.NOT_NECESSARY);
						}
						if (localizationValue.getTranslationVerificationState() == null) {
							localizationValue.setTranslationVerificationState(TranslationVerificationState.NOT_NECESSARY);
						}
						localizationValue.save();
					}
				}
			}
		}
		createRequiredLanguageValues(createdKeys, localizationConfig);
		workbook.close();
		return "Localization keys updates:\nCreated keys: " + countCreatedKeys + "\nUpdated keys: " + countUpdatedKeys + "\nCreated values: " + countCreatedValues + "\nUpdated values: " + countUpdatedValues + "\n\nError messages:\n" + errors;
	}

	private static String getCellText(Cell cell) {
		if (cell != null && cell.getCellType() == CellType.STRING) {
			return cell.getStringCellValue();
		} else {
			return null;
		}
	}

	private static int getCellIntValue(Cell cell) {
		if (cell != null && cell.getCellType() == CellType.NUMERIC) {
			double cellValue = cell.getNumericCellValue();
			return (int) cellValue;
		} else {
			return -1;
		}
	}


}
