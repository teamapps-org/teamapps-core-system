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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.annotation.TeamAppsBootableClass;
import org.teamapps.application.api.application.ApplicationBuilder;
import org.teamapps.application.api.config.ApplicationConfig;
import org.teamapps.application.api.password.SecurePasswordHash;
import org.teamapps.application.server.ApplicationServerConfig;
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
import org.teamapps.model.ControlCenterModel;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.DatabaseManager;
import org.teamapps.universaldb.UniversalDB;
import org.teamapps.universaldb.UniversalDbBuilder;
import org.teamapps.universaldb.index.file.FileValue;
import org.teamapps.universaldb.index.file.store.LocalDatabaseFileStore;
import org.teamapps.universaldb.index.translation.TranslatableText;
import org.teamapps.universaldb.model.DatabaseModel;
import org.teamapps.universaldb.schema.ModelProvider;
import org.teamapps.ux.component.template.BaseTemplateRecord;
import org.teamapps.ux.session.SessionContext;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
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
		getSystemApplications().forEach(app -> systemRegistry.installAndLoadApplication(app));

		if (User.getCount() == 0) {
			createInitialUser();
		}

		for (Application application : Application.getAll()) {
			if (application.isUninstalled()) {
				LOGGER.info("Skipping uninstalled app:" + application.getName());
				continue;
			}
			LOGGER.info("Loading app:" + application.getName());
			ApplicationVersion installedVersion = application.getInstalledVersion();
			if (installedVersion == null) {
				LOGGER.warn("ERROR: app has no installed version:" + application);
				continue;
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
			}
		}

		handleSystemStarted();

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

	@Override
	public void handleSessionStart(SessionContext context) {
		registerIconProviders(context);
		if (sessionRegistryHandler != null) {
			sessionRegistryHandler.handleNewSession(context);
		}


		new LoginHandler(systemRegistry, this).handleNewSession(context);
	}

	private void registerIconProviders(SessionContext context) {
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
