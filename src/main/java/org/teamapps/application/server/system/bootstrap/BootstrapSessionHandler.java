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
package org.teamapps.application.server.system.bootstrap;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.annotation.TeamAppsBootableClass;
import org.teamapps.application.api.application.ApplicationBuilder;
import org.teamapps.application.api.config.ApplicationConfig;
import org.teamapps.application.api.password.SecurePasswordHash;
import org.teamapps.application.server.ServerRegistry;
import org.teamapps.application.server.SessionHandler;
import org.teamapps.application.server.SessionManager;
import org.teamapps.application.server.controlcenter.ControlCenterAppBuilder;
import org.teamapps.application.server.settings.UserSettingsApp;
import org.teamapps.application.server.system.auth.LoginHandler;
import org.teamapps.application.server.system.bootstrap.installer.ApplicationInstaller;
import org.teamapps.application.server.system.config.SystemConfig;
import org.teamapps.application.server.system.server.SessionRegistryHandler;
import org.teamapps.application.server.system.template.Templates;
import org.teamapps.cluster.core.Cluster;
import org.teamapps.event.Event;
import org.teamapps.icon.antu.AntuIcon;
import org.teamapps.icon.flags.FlagIcon;
import org.teamapps.icon.fontawesome.FontAwesomeIcon;
import org.teamapps.icon.material.MaterialIcon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.file.FileValue;
import org.teamapps.universaldb.index.translation.TranslatableText;
import org.teamapps.universaldb.schema.ModelProvider;
import org.teamapps.ux.component.template.BaseTemplateRecord;
import org.teamapps.ux.session.SessionContext;

import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@TeamAppsBootableClass(priority = 7)
public class BootstrapSessionHandler implements SessionHandler, LogoutHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static Class standardIconClass;

	static {
		try {
			standardIconClass = Class.forName("org.teamapps.icon.standard.StandardIcon");
		} catch (Exception var1) {
		}
	}

	public Event<SessionContext> onUserLogout = new Event<>();

	private SessionRegistryHandler sessionRegistryHandler;
	private ServerRegistry serverRegistry;
	private SystemRegistry systemRegistry;

	public BootstrapSessionHandler() {
	}

	public void setSessionRegistryHandler(SessionRegistryHandler sessionRegistryHandler) {
		this.sessionRegistryHandler = sessionRegistryHandler;
	}

	@Override
	public void init(SessionManager sessionManager, ServerRegistry serverRegistry) {
		try {
			this.serverRegistry = serverRegistry;
			startSystem(sessionManager);
		} catch (Exception e) {
			LOGGER.error("Error initializing system:", e);
		}
	}

	public void startCluster(Cluster cluster) {
		systemRegistry.setCluster(cluster);
	}

	private void startSystem(SessionManager sessionManager) throws Exception {
		ApplicationBuilder controlCenterApp = getControlCenterApplication();
		ApplicationConfig<SystemConfig> applicationConfig = controlCenterApp.getApplicationConfig();
		ModelProvider databaseModel = controlCenterApp.getDatabaseModel();
		SystemRegistry.createDatabaseBuilder(databaseModel.getModel().getName(), serverRegistry)
				.modelProvider(databaseModel)
				.classLoader(this.getClass().getClassLoader())
				.build();
		systemRegistry = new SystemRegistry(this, serverRegistry, sessionManager, applicationConfig);
		systemRegistry.setSessionRegistryHandler(sessionRegistryHandler);
		systemRegistry.installAndLoadApplication(controlCenterApp);

		for (Map.Entry<String, Supplier<InputStream>> entry : getEmbeddedUpdatableApps().entrySet()) {
			String applicationName = entry.getKey();
			if (Application.getAll().stream().noneMatch(app -> applicationName.equals(app.getName()))) {
				LOGGER.info("Install embedded updatable app: {}", applicationName);
				File jarFile = File.createTempFile("temp", ".jar");
				FileUtils.copyToFile(entry.getValue().get(), jarFile);
				ApplicationInstaller jarInstaller = systemRegistry.createJarInstaller(jarFile);
				systemRegistry.installAndLoadApplication(jarInstaller);
			} else {
				LOGGER.info("Load installed embedded app: {}", applicationName);
				Application application = Application.getAll().stream().filter(app -> applicationName.equals(app.getName())).findFirst().orElse(null);
				loadInstalledJarApplication(application);
			}
		}

		getSystemApplications().forEach(app -> systemRegistry.installAndLoadApplication(app));

		if (User.getCount() == 0) {
			createInitialUser();
		}

		for (Application application : Application.getAll()) {
			loadInstalledJarApplication(application);
		}

		handleSystemStarted();

	}

	private void loadInstalledJarApplication(Application application) {
		if (application.isUninstalled()) {
			LOGGER.info("Skipping uninstalled app: {}", application.getName());
			return;
		}
		if (systemRegistry.getLoadedApplication(application) != null) {
			LOGGER.info("Skip loading already loaded app: {}", application.getName());
			return;
		}
		LOGGER.info("Loading app:" + application.getName());
		ApplicationVersion installedVersion = application.getInstalledVersion();
		if (installedVersion == null) {
			LOGGER.warn("ERROR: app has no installed version: {}", application);
			return;
		}
		FileValue binary = installedVersion.getBinary();
		if (binary != null) {
			File jarFile = binary.getAsFile();
			ApplicationInstaller jarInstaller = systemRegistry.createJarInstaller(jarFile);
			try {
				if (jarInstaller.isInstalled()) {
					systemRegistry.loadApplication(jarInstaller);
				}
			} catch (Throwable e) {
				LOGGER.warn("Error while loading application:", e);
				e.printStackTrace();
			}
		} else {
			LOGGER.error("Error loading app: {}, missing jar-file for installed version: {}", application.getName(), installedVersion.getVersion());
		}
	}

	public void handleSystemStarted() {
		LOGGER.info("System started");
	}

	public void createInitialUser() {
		createInitialUser("admin", "teamapps!", "en", false);
	}

	public void createInitialUser(String login, String password, String language, boolean darkTheme) {
		OrganizationUnit existingUnit = OrganizationUnit.getAll().stream().filter(unit -> unit.getType().isAllowUsers()).findFirst().orElse(null);
		OrganizationUnit organizationUnit = existingUnit != null ? existingUnit : OrganizationUnit.create()
				.setName(TranslatableText.create("Organization", "en"))
				.setType(
						OrganizationUnitType.create()
								.setName(TranslatableText.create("Standard", "en"))
								.setAllowUsers(true)
				)
				.save();
		User.create()
				.setFirstName("Super")
				.setLastName("Admin")
				.setDisplayLanguage(language)
				.setLogin(login)
				.setPassword(SecurePasswordHash.createDefault().createSecureHash(password))
				.setUserAccountStatus(UserAccountStatus.SUPER_ADMIN)
				.setOrganizationUnit(organizationUnit)
				.setDarkTheme(darkTheme)
				.save();
	}

	public ApplicationBuilder getControlCenterApplication() {
		return new ControlCenterAppBuilder();
	}

	public List<ApplicationBuilder> getSystemApplications() {
		return Arrays.asList(
				new UserSettingsApp()
		);
	}

	public Map<String, Supplier<InputStream>> getEmbeddedUpdatableApps() {
		return Collections.emptyMap();
	}

	@Override
	public void handleSessionStart(SessionContext context) {
		registerIconProviders(context);
		new LoginHandler(systemRegistry, this).handleNewSession(context);
	}

	protected void registerIconProviders(SessionContext context) {
		if (standardIconClass != null) {
			context.getIconProvider().registerIconLibrary(standardIconClass);
		}
		context.getIconProvider().registerIconLibrary(FlagIcon.class);
		context.getIconProvider().registerIconLibrary(MaterialIcon.class);
		context.getIconProvider().registerIconLibrary(FontAwesomeIcon.class);
		context.getIconProvider().registerIconLibrary(AntuIcon.class);
		context.registerTemplates(Arrays.stream(Templates.values())
				.collect(Collectors.toMap(Enum::name, Templates::getTemplate)));
	}

	@Override
	public List<BaseTemplateRecord<Long>> getActiveUsers() {
		return systemRegistry.getActiveUsers();
	}

	@Override
	public void shutDown() {

	}

	@Override
	public void handleLogout(SessionContext context) {

	}

	public SystemRegistry getSystemRegistry() {
		return systemRegistry;
	}

}
