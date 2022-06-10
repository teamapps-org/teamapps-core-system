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
import org.teamapps.application.api.application.perspective.PerspectiveBuilder;
import org.teamapps.application.server.system.privilege.ApplicationScopePrivilegeProvider;
import org.teamapps.model.controlcenter.Application;

import java.util.Collections;
import java.util.List;

public class LoadedApplication {

	private final Application application;
	private final BaseApplicationBuilder baseApplicationBuilder;
	private final ClassLoader applicationClassLoader;
	private final boolean unmanagedPerspectives;
	private ApplicationScopePrivilegeProvider applicationScopePrivilegeProvider;

	public LoadedApplication(Application application, BaseApplicationBuilder applicationBuilder, ClassLoader applicationClassLoader, boolean unmanagedPerspectives) {
		this.application = application;
		this.baseApplicationBuilder = applicationBuilder;
		this.applicationClassLoader = applicationClassLoader;
		this.unmanagedPerspectives = unmanagedPerspectives;
	}

	public List<PerspectiveBuilder> getPerspectiveBuilders() {
		if (unmanagedPerspectives) {
			return Collections.emptyList();
		} else {
			ApplicationBuilder applicationPerspectiveBuilder = (ApplicationBuilder) baseApplicationBuilder;
			return applicationPerspectiveBuilder.getPerspectiveBuilders();
		}
	}

	public PerspectiveBuilder getPerspectiveBuilder(String name) {
		return getPerspectiveBuilders().stream()
				.filter(perspective -> perspective.getName().equals(name))
				.findFirst()
				.orElse(null);
	}

	public ApplicationScopePrivilegeProvider getAppPrivilegeProvider() {
		return applicationScopePrivilegeProvider;
	}

	public void setAppPrivilegeProvider(ApplicationScopePrivilegeProvider applicationScopePrivilegeProvider) {
		this.applicationScopePrivilegeProvider = applicationScopePrivilegeProvider;
	}

	public Application getApplication() {
		return application;
	}

	public BaseApplicationBuilder getBaseApplicationBuilder() {
		return baseApplicationBuilder;
	}

	public ClassLoader getApplicationClassLoader() {
		return applicationClassLoader;
	}

	public ClassLoader getApplicationClassLoaderOrDefault() {
		return applicationClassLoader != null ? applicationClassLoader : this.getClass().getClassLoader();
	}

	public boolean isUnmanagedPerspectives() {
		return unmanagedPerspectives;
	}
}
