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
package org.teamapps.application.server.system.bootstrap.installer;

import org.teamapps.application.api.application.ApplicationInitializer;
import org.teamapps.application.api.application.BaseApplicationBuilder;
import org.teamapps.application.server.system.bootstrap.ApplicationInfo;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.config.LocalizationConfig;
import org.teamapps.application.server.system.localization.LocalizationUtil;
import org.teamapps.application.server.system.machinetranslation.TranslationService;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.ApplicationVersion;
import org.teamapps.universaldb.DatabaseManager;
import org.teamapps.universaldb.UniversalDB;
import org.teamapps.universaldb.UniversalDbBuilder;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class ApplicationInstaller {

	private final ApplicationInfo applicationInfo;
	private final TranslationService translationService;
	private final LocalizationConfig localizationConfig;

	private final List<ApplicationInstallationPhase> applicationInstallationPhases;


	public static ApplicationInstaller createJarInstaller(File applicationJar, DatabaseManager databaseManager, Function<String, UniversalDbBuilder> dbBuilderFunction, TranslationService translationService, LocalizationConfig localizationConfig) {
		return new ApplicationInstaller(new ApplicationInfo(applicationJar), databaseManager, dbBuilderFunction, translationService, localizationConfig);
	}

	public static ApplicationInstaller createClassInstaller(BaseApplicationBuilder baseApplicationBuilder, DatabaseManager databaseManager, Function<String, UniversalDbBuilder> dbBuilderFunction, TranslationService translationService, LocalizationConfig localizationConfig) {
		return new ApplicationInstaller(new ApplicationInfo(baseApplicationBuilder), databaseManager, dbBuilderFunction, translationService, localizationConfig);
	}

	private ApplicationInstaller(ApplicationInfo applicationInfo, DatabaseManager databaseManager, Function<String, UniversalDbBuilder> dbBuilderFunction, TranslationService translationService, LocalizationConfig localizationConfig) {
		this.applicationInfo = applicationInfo;
		this.translationService = translationService;
		this.localizationConfig = localizationConfig;
		applicationInstallationPhases = Arrays.asList(
				new ApplicationJarInstallationPhase(),
				new ApplicationArtifactInstallationPhase(),
				new DataModelInstallationPhase(databaseManager, dbBuilderFunction),
				new LocalizationDataInstallationPhase(localizationConfig),
				new PrivilegeDataInstallationPhase(),
				new PerspectiveDataInstallationPhase()
		);
	}

	public boolean isInstalled() {
		if (!applicationInfo.isChecked()) {
			checkApplication();
		}
		Application application = applicationInfo.getApplication();
		if (application == null) {
			return false;
		}
		String version = applicationInfo.getBaseApplicationBuilder().getApplicationVersion().getVersion();
		ApplicationVersion matchingVersion = application.getVersions().stream().filter(v -> v.getVersion().equals(version)).findFirst().orElse(null);
		return matchingVersion != null;
	}

	public ApplicationInfo checkApplication() {
		applicationInstallationPhases.forEach(phase -> phase.checkApplication(applicationInfo));
		applicationInfo.setChecked(true);
		return applicationInfo;
	}

	public boolean installApplication() {
		if (applicationInfo.isChecked() && applicationInfo.getErrors().isEmpty()) {
			applicationInstallationPhases.forEach(phase -> phase.installApplication(applicationInfo));
			applicationInfo.getApplication().setInstalledVersion(applicationInfo.getApplicationVersion()).save();
			LocalizationUtil.translateAllApplicationValues(translationService, applicationInfo.getApplication(), localizationConfig);
			applicationInfo.getBaseApplicationBuilder().getOnApplicationInstalled().fire();
			return true;
		} else {
			return false;
		}
	}

	public LoadedApplication loadApplication(File basePath) {
		if (applicationInfo.isChecked() && applicationInfo.getErrors().isEmpty()) {
			applicationInfo.createLoadedApplication(basePath);
			applicationInstallationPhases.forEach(phase -> phase.loadApplication(applicationInfo));
			ClassLoader classLoader = applicationInfo.getApplicationClassLoader();
			if (classLoader == null) {
				classLoader = this.getClass().getClassLoader();
			}
			String applicationConfig = applicationInfo.getBaseApplicationBuilder().getApplicationConfigXml(classLoader);
			Application application = applicationInfo.getApplication();
			if (applicationConfig != null && application.getConfig() != null) {
				try {
					applicationInfo.getBaseApplicationBuilder().updateConfig(application.getConfig(), classLoader);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			ApplicationInitializer applicationInitializer = applicationInfo.getLoadedApplication().getApplicationInitializer();
			applicationInfo.getBaseApplicationBuilder().getOnApplicationLoaded().fire(applicationInitializer);
			return applicationInfo.getLoadedApplication();
		}
		return null;
	}

	public ApplicationInfo getApplicationInfo() {
		return applicationInfo;
	}
}
