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
package org.teamapps.application.server.system.bootstrap.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.application.BaseApplicationBuilder;
import org.teamapps.application.server.system.bootstrap.ApplicationInfo;
import org.teamapps.application.server.system.bootstrap.ApplicationInfoDataElement;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.ApplicationVersion;
import org.teamapps.model.controlcenter.ApplicationVersionData;

import java.lang.invoke.MethodHandles;

public class ApplicationArtifactInstallationPhase implements ApplicationInstallationPhase {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void checkApplication(ApplicationInfo applicationInfo) {
		try {
			if (!applicationInfo.getErrors().isEmpty()) {
				return;
			}
			if (applicationInfo.getApplicationJar() != null) {
				String binaryHash = applicationInfo.getBinaryHash();
				applicationInfo.setBinaryHash(binaryHash);
			}
			BaseApplicationBuilder baseApplicationBuilder = applicationInfo.getBaseApplicationBuilder();
			String applicationName = baseApplicationBuilder.getApplicationName();
			if (
					applicationName == null ||
							applicationName.isEmpty() ||
							applicationName.contains(" ") ||
							applicationName.contains(".")) {
				applicationInfo.addError("Invalid application name: " + applicationName);
				return;
			}
			org.teamapps.application.api.versioning.ApplicationVersion applicationVersion = baseApplicationBuilder.getApplicationVersion();
			if (applicationVersion == null) {
				applicationInfo.addError("Missing application version");
				return;
			}
			if (baseApplicationBuilder.getApplicationTitleKey() == null) {
				applicationInfo.addError("Missing application title");
				return;
			}
			String versionString = applicationVersion.getVersion();
			applicationInfo.setName(applicationName);
			applicationInfo.setReleaseNotes(applicationInfo.getBaseApplicationBuilder().getReleaseNotes());
			applicationInfo.setVersion(versionString);
			Application application = applicationInfo.getApplication();
			if (application != null) {
				for (ApplicationVersion version : application.getVersions()) {
					String installedVersion = version.getVersion();
					org.teamapps.application.api.versioning.ApplicationVersion installedVersion2 = new org.teamapps.application.api.versioning.ApplicationVersion(installedVersion);
					if (installedVersion.equals(versionString)) {
						applicationInfo.addWarning("This version is already installed: " + versionString);
						applicationInfo.setApplicationVersion(version);
					}
					if (installedVersion2.compareTo(applicationVersion) > 0) {
						applicationInfo.addWarning("A higher version is already installed! This version: " + versionString + ", installed Version: " + installedVersion);
					}
				}
			}
		} catch (Exception e) {
			applicationInfo.addError("Error checking artifact:" + e.getMessage());
			LOGGER.error("Error checking artifact:", e);
		}
	}

	@Override
	public void installApplication(ApplicationInfo applicationInfo) {
		Application application = applicationInfo.getApplication();
		BaseApplicationBuilder baseApplicationBuilder = applicationInfo.getBaseApplicationBuilder();
		if (application == null) {
			application = Application.create()
					.setName(applicationInfo.getName());
		}
		application
				.setIcon(IconUtils.encodeNoStyle(baseApplicationBuilder.getApplicationIcon()))
				.setTitleKey(baseApplicationBuilder.getApplicationTitleKey())
				.setDescriptionKey(baseApplicationBuilder.getApplicationDescriptionKey())
				.setUnmanagedApplication(applicationInfo.isUnmanagedPerspectives())
				.setDarkTheme(baseApplicationBuilder.isDarkTheme())
				.save();
		ApplicationVersion applicationVersion = ApplicationVersion.create()
				.setApplication(application)
				.setVersion(applicationInfo.getVersion())
				.setBinary(applicationInfo.getApplicationJar())
				.setBinaryHash(applicationInfo.getBinaryHash())
				.setReleaseNotes(applicationInfo.getReleaseNotes())
				.setDataModelData(createVersionData(applicationInfo.getDataModelData()))
				.setLocalizationData(createVersionData(applicationInfo.getLocalizationData()))
				.setPrivilegeData(createVersionData(applicationInfo.getPrivilegeData()))
				.setPerspectiveData(createVersionData(applicationInfo.getPerspectiveData()))
				.save();
		applicationInfo.setApplicationVersion(applicationVersion);
	}

	@Override
	public void loadApplication(ApplicationInfo applicationInfo) {

	}

	private ApplicationVersionData createVersionData(ApplicationInfoDataElement dataElement) {
		if (dataElement == null || (dataElement.getData() == null && dataElement.getDataAdded().isEmpty() && dataElement.getDataRemoved().isEmpty())) {
			return null;
		}
		return ApplicationVersionData
				.create()
				.setData(dataElement.getData())
				.setDataRows(dataElement.getData() == null ? 0 : dataElement.getData().split("\n").length)
				.setDataAdded(String.join("\n", dataElement.getDataAdded()))
				.setDataAddedRows(dataElement.getDataAdded().size())
				.setDataRemoved(String.join("\n", dataElement.getDataRemoved()))
				.setDataRemovedRows(dataElement.getDataRemoved().size())
				.save();
	}
}
