/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
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
import org.teamapps.application.server.system.utils.ValueConverterUtils;
import org.teamapps.cluster.core.Cluster;
import org.teamapps.event.Event;
import org.teamapps.icon.antu.AntuIcon;
import org.teamapps.icon.flags.FlagIcon;
import org.teamapps.icon.fontawesome.FontAwesomeIcon;
import org.teamapps.icon.material.MaterialIcon;
import org.teamapps.model.ControlCenterSchema;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.ApplicationVersion;
import org.teamapps.model.controlcenter.User;
import org.teamapps.model.controlcenter.UserAccountStatus;
import org.teamapps.universaldb.UniversalDB;
import org.teamapps.universaldb.index.file.FileValue;
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

	private final SessionRegistryHandler sessionRegistryHandler;
	private ServerRegistry serverRegistry;
	private UniversalDB universalDB;
	private SystemRegistry systemRegistry;

	public BootstrapSessionHandler() {
		this(null);
	}

	public BootstrapSessionHandler(SessionRegistryHandler sessionRegistryHandler) {
		this.sessionRegistryHandler = sessionRegistryHandler;
	}

	@Override
	public void init(SessionManager sessionManager, ServerRegistry serverRegistry) {
		try {
			this.serverRegistry = serverRegistry;
			this.universalDB = serverRegistry.getUniversalDB();
			startSystem(sessionManager);
		} catch (Exception e) {
			LOGGER.error("Error initializing system:", e);
		}
	}

	public void startCluster(Cluster cluster) {
		systemRegistry.setCluster(cluster);
	}

	private void startSystem(SessionManager sessionManager) throws Exception {
		ClassLoader classLoader = this.getClass().getClassLoader();
		ControlCenterSchema schema = new ControlCenterSchema();
		universalDB.addAuxiliaryModel(schema, classLoader);

		ApplicationBuilder controlCenterApp = getControlCenterApplication();
		ApplicationConfig<SystemConfig> applicationConfig = controlCenterApp.getApplicationConfig();
		systemRegistry = new SystemRegistry(this, serverRegistry, sessionManager, applicationConfig);
		systemRegistry.setSessionRegistryHandler(sessionRegistryHandler);
		systemRegistry.installAndLoadApplication(controlCenterApp);
		getSystemApplications().forEach(app -> systemRegistry.installAndLoadApplication(app));

		if (User.getCount() == 0) {
			createInitialUser();
		}

		for (Application application : Application.getAll()) {
			LOGGER.info("Loading app:" + application.getName());
			ApplicationVersion installedVersion = application.getInstalledVersion();
			if (installedVersion == null) {
				LOGGER.warn("ERROR: app has no installed version:" + application);
				continue;
			}
			FileValue binary = installedVersion.getBinary();
			if (binary != null) {
				File jarFile = binary.getFileSupplier().get();
				ApplicationInstaller jarInstaller = ApplicationInstaller.createJarInstaller(jarFile, universalDB, systemRegistry.getTranslationService(), systemRegistry.getSystemConfig().getLocalizationConfig());
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

	}

	public void createInitialUser() {
		User.create()
				.setFirstName("Super")
				.setLastName("Admin")
				.setLogin("admin")
				.setPassword(SecurePasswordHash.createDefault().createSecureHash("teamapps!"))
				.setUserAccountStatus(UserAccountStatus.SUPER_ADMIN)
				.setLanguages(ValueConverterUtils.compressStringList(Arrays.asList("en", "fr", "de")))
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
		if (standardIconClass != null) {
			context.getIconProvider().registerIconLibrary(standardIconClass);
		}
		if (sessionRegistryHandler != null) {
			sessionRegistryHandler.handleNewSession(context);
		}

		context.getIconProvider().registerIconLibrary(FlagIcon.class);
		context.getIconProvider().registerIconLibrary(MaterialIcon.class);
		context.getIconProvider().registerIconLibrary(FontAwesomeIcon.class);
		context.getIconProvider().registerIconLibrary(AntuIcon.class);
		context.registerTemplates(Arrays.stream(Templates.values())
				.collect(Collectors.toMap(Enum::name, Templates::getTemplate)));

		new LoginHandler(systemRegistry, this).handleNewSession(context);
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

	public UniversalDB getUniversalDB() {
		return universalDB;
	}

	public SystemRegistry getSystemRegistry() {
		return systemRegistry;
	}

}
