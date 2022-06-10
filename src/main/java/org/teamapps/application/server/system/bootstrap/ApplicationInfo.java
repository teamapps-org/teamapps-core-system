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
package org.teamapps.application.server.system.bootstrap;

import org.teamapps.application.api.application.ApplicationBuilder;
import org.teamapps.application.api.application.BaseApplicationBuilder;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.ApplicationVersion;
import org.teamapps.universaldb.index.text.TextFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ApplicationInfo {


	private BaseApplicationBuilder baseApplicationBuilder;
	private File applicationJar;
	private ClassLoader applicationClassLoader;
	private boolean unmanagedPerspectives;
	private String name;
	private String binaryHash;
	private String version;
	private String releaseNotes;
	private List<String> warnings = new ArrayList<>();
	private List<String> errors = new ArrayList<>();
	private ApplicationInfoDataElement dataModelData;
	private ApplicationInfoDataElement localizationData;
	private ApplicationInfoDataElement privilegeData;
	private ApplicationInfoDataElement perspectiveData;
	private boolean checked;

	private Application application;
	private ApplicationVersion applicationVersion;
	private LoadedApplication loadedApplication;

	public ApplicationInfo(BaseApplicationBuilder baseApplicationBuilder) {
		this.baseApplicationBuilder = baseApplicationBuilder;
		if (!(baseApplicationBuilder instanceof ApplicationBuilder)) {
			unmanagedPerspectives = true;
		}
	}

	public ApplicationInfo(File applicationJar) {
		this.applicationJar = applicationJar;
	}

	public void addWarning(String warning) {
		warnings.add(warning);
	}

	public void addError(String error) {
		errors.add(error);
	}

	public Application getApplication() {
		if (application == null && getName() != null) {
			application = Application.filter()
					.name(TextFilter.textEqualsFilter(getName()))
					.executeExpectSingleton();
		}
		return application;
	}

	public void createLoadedApplication() {
		loadedApplication = new LoadedApplication(getApplication(), baseApplicationBuilder, applicationClassLoader, unmanagedPerspectives);
	}

	public LoadedApplication getLoadedApplication() {
		return loadedApplication;
	}

	public BaseApplicationBuilder getBaseApplicationBuilder() {
		return baseApplicationBuilder;
	}

	public void setBaseApplicationBuilder(BaseApplicationBuilder baseApplicationBuilder) {
		this.baseApplicationBuilder = baseApplicationBuilder;
	}

	public File getApplicationJar() {
		return applicationJar;
	}

	public void setApplicationJar(File applicationJar) {
		this.applicationJar = applicationJar;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBinaryHash() {
		return binaryHash;
	}

	public void setBinaryHash(String binaryHash) {
		this.binaryHash = binaryHash;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getReleaseNotes() {
		return releaseNotes;
	}

	public void setReleaseNotes(String releaseNotes) {
		this.releaseNotes = releaseNotes;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}

	public String getWarningMessage() {
		return warnings.isEmpty() ? null : String.join(", ", warnings);
	}

	public String getErrorMessage() {
		return errors.isEmpty() ? null : String.join(", ", errors);
	}

	public ApplicationInfoDataElement getDataModelData() {
		return dataModelData;
	}

	public void setDataModelData(ApplicationInfoDataElement dataModelData) {
		this.dataModelData = dataModelData;
	}

	public ApplicationInfoDataElement getLocalizationData() {
		return localizationData;
	}

	public void setLocalizationData(ApplicationInfoDataElement localizationData) {
		this.localizationData = localizationData;
	}

	public ApplicationInfoDataElement getPrivilegeData() {
		return privilegeData;
	}

	public void setPrivilegeData(ApplicationInfoDataElement privilegeData) {
		this.privilegeData = privilegeData;
	}

	public ApplicationInfoDataElement getPerspectiveData() {
		return perspectiveData;
	}

	public void setPerspectiveData(ApplicationInfoDataElement perspectiveData) {
		this.perspectiveData = perspectiveData;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	public ClassLoader getApplicationClassLoader() {
		return applicationClassLoader;
	}

	public void setApplicationClassLoader(ClassLoader applicationClassLoader) {
		this.applicationClassLoader = applicationClassLoader;
	}

	public boolean isUnmanagedPerspectives() {
		return unmanagedPerspectives;
	}

	public void setUnmanagedPerspectives(boolean unmanagedPerspectives) {
		this.unmanagedPerspectives = unmanagedPerspectives;
	}

	public ApplicationVersion getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(ApplicationVersion applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	@Override
	public String toString() {
		return "ApplicationInfo{" +
				"applicationBuilder=" + baseApplicationBuilder +
				", applicationJar=" + applicationJar +
				", applicationClassLoader=" + applicationClassLoader +
				", unmanagedPerspectives=" + unmanagedPerspectives +
				", name='" + name + '\'' +
				", binaryHash='" + binaryHash + '\'' +
				", version='" + version + '\'' +
				", releaseNotes='" + releaseNotes + '\'' +
				", warnings=" + warnings +
				", errors=" + errors +
				", dataModelData=" + dataModelData +
				", localizationData=" + localizationData +
				", privilegeData=" + privilegeData +
				", perspectiveData=" + perspectiveData +
				", checked=" + checked +
				", application=" + application +
				", applicationVersion=" + applicationVersion +
				", loadedApplication=" + loadedApplication +
				'}';
	}
}
