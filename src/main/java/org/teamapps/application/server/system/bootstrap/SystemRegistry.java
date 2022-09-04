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
import org.teamapps.application.api.application.BaseApplicationBuilder;
import org.teamapps.application.api.application.entity.EntityUpdate;
import org.teamapps.application.api.config.ApplicationConfig;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.state.MultiStateHandler;
import org.teamapps.application.api.state.ReplicatedStateMachine;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.EntityUpdateEventHandler;
import org.teamapps.application.server.ServerRegistry;
import org.teamapps.application.server.SessionManager;
import org.teamapps.application.server.system.auth.AuthenticationHandler;
import org.teamapps.application.server.system.bootstrap.installer.ApplicationInstaller;
import org.teamapps.application.server.system.config.DocumentConversionConfig;
import org.teamapps.application.server.system.config.MachineTranslationConfig;
import org.teamapps.application.server.system.config.SystemConfig;
import org.teamapps.application.server.system.localization.DictionaryLocalizationProvider;
import org.teamapps.application.server.system.localization.GlobalLocalizationProvider;
import org.teamapps.application.server.system.localization.LocalizationUtil;
import org.teamapps.application.server.system.localization.SystemLocalizationProvider;
import org.teamapps.application.server.system.machinetranslation.MachineTranslation;
import org.teamapps.application.server.system.machinetranslation.TranslationService;
import org.teamapps.application.server.system.server.SessionRegistryHandler;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.cluster.core.Cluster;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.reporting.convert.DocumentConverter;
import org.teamapps.universaldb.UniversalDB;
import org.teamapps.universaldb.record.EntityBuilder;
import org.teamapps.ux.component.template.BaseTemplateRecord;
import org.teamapps.ux.session.SessionContext;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SystemRegistry {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final BootstrapSessionHandler bootstrapSessionHandler;
	private final UniversalDB universalDB;
	private final ApplicationConfig<SystemConfig> applicationConfig;
	private TranslationService translationService;
	private final DictionaryLocalizationProvider dictionary;
	private final SystemLocalizationProvider systemDictionary;
	private final GlobalLocalizationProvider globalLocalizationProvider;
	private final Map<Application, LoadedApplication> loadedApplicationMap = new HashMap<>();
	private final ManagedApplicationGroup unspecifiedApplicationGroup;
	private final BaseResourceLinkProvider baseResourceLinkProvider;
	private SessionRegistryHandler sessionRegistryHandler;
	private DocumentConverter documentConverter;
	private final List<AuthenticationHandler> authenticationHandlers = new ArrayList<>();
	private final EntityUpdateEventHandler entityUpdateEventHandler;
	private Cluster cluster;
	private ServerRegistry serverRegistry;
	private final SessionManager sessionManager;
	private final WeakHashMap<UserSessionData, Long> activeUsersMap = new WeakHashMap<>();
	private Map<String, MultiStateHandler> stateHandlerMap = new HashMap<>();

	public SystemRegistry(BootstrapSessionHandler bootstrapSessionHandler, ServerRegistry serverRegistry, SessionManager sessionManager, ApplicationConfig<SystemConfig> applicationConfig) {
		this.serverRegistry = serverRegistry;
		SystemConfig systemConfig = applicationConfig.getConfig();
		this.bootstrapSessionHandler = bootstrapSessionHandler;
		this.serverRegistry = serverRegistry;
		this.sessionManager = sessionManager;
		this.universalDB = serverRegistry.getUniversalDB();
		this.entityUpdateEventHandler = serverRegistry.getEntityUpdateEventHandler();
		this.applicationConfig = applicationConfig;
		this.systemDictionary = new SystemLocalizationProvider();
		this.dictionary = new DictionaryLocalizationProvider(systemConfig.getLocalizationConfig());
		this.globalLocalizationProvider = new GlobalLocalizationProvider(this);
		this.baseResourceLinkProvider = new BaseResourceLinkProvider();
		this.unspecifiedApplicationGroup = getOrCreateUnspecifiedApplicationGroup();

		applicationConfig.onConfigUpdate.addListener(this::handleConfigUpdate);
		handleConfigUpdate();
	}

	private void handleConfigUpdate() {
		SystemConfig config = applicationConfig.getConfig();
		DocumentConversionConfig documentConversionConfig = config.getDocumentConversionConfig();
		if (documentConversionConfig.isActive()) {
			documentConverter = DocumentConverter.createRemoteConverter(documentConversionConfig.getHost(), documentConversionConfig.getUser(), documentConversionConfig.getPassword());
		}
		MachineTranslationConfig machineTranslationConfig = config.getMachineTranslationConfig();
		if (machineTranslationConfig.isActive()) {
			MachineTranslation machineTranslation = new MachineTranslation();
			machineTranslation.setGoogleTranslationKey(machineTranslationConfig.getGoogleKey());
			machineTranslation.setDeepLKey(machineTranslationConfig.getDeepLKey(), machineTranslationConfig.isDeepLFreeApi());
			if (translationService == null) {
				dictionary.translateDictionary(machineTranslation);
			}
			this.translationService = machineTranslation;
		}
	}

	public void updateGlobalLocalizationProvider() {
		globalLocalizationProvider.updateLocalizationData();
		systemDictionary.update();
	}

	public void machineTranslateMissingEntries() {
		if (translationService != null) {
			LocalizationUtil.translateAllValues(translationService, getSystemConfig().getLocalizationConfig());
		}
	}

	public synchronized void addActiveUser(UserSessionData userSessionData) {
		activeUsersMap.put(userSessionData, System.currentTimeMillis());
	}

	public synchronized List<BaseTemplateRecord<Long>> getActiveUsers() {
		List<BaseTemplateRecord<Long>> activeUserData = new ArrayList<>();
		for (Map.Entry<UserSessionData, Long> entry : activeUsersMap.entrySet()) {
			UserSessionData userSessionData = entry.getKey();
			if (userSessionData != null && !userSessionData.getContext().isDestroyed()) {
				User user = userSessionData.getUser();
				String caption = user.getFirstName() + " " + user.getLastName() + " (" + user.getLogin() + ")";
				String description = user.getOrganizationUnit() != null ? user.getOrganizationUnit().getName().getText() : null;
				Icon icon = userSessionData.getUserPrivileges() != null ? ApplicationIcons.USER : ApplicationIcons.SIGN_FORBIDDEN;
				BaseTemplateRecord<Long> record = new BaseTemplateRecord<>(icon, caption, description, entry.getValue());
				activeUserData.add(record);
			}
		}
		return activeUserData;
	}

	private ManagedApplicationGroup getOrCreateUnspecifiedApplicationGroup() {
		if (ManagedApplicationGroup.getCount() == 0) {
			return ManagedApplicationGroup.create().setIcon(IconUtils.encodeNoStyle(ApplicationIcons.HOME)).setTitleKey(Dictionary.APPLICATIONS).save();
		} else {
			return ManagedApplicationGroup.getAll().get(0);
		}
	}

	public ApplicationInstaller createJarInstaller(File jarFile) {
		return ApplicationInstaller.createJarInstaller(jarFile, universalDB, translationService, getSystemConfig().getLocalizationConfig());
	}

	public boolean installAndLoadApplication(BaseApplicationBuilder baseApplicationBuilder) {
		ApplicationInstaller applicationInstaller = ApplicationInstaller.createClassInstaller(baseApplicationBuilder, universalDB, translationService, getSystemConfig().getLocalizationConfig());
		return installAndLoadApplication(applicationInstaller);
	}

	public boolean installAndLoadApplication(ApplicationInstaller applicationInstaller) {
		if (!applicationInstaller.isInstalled()) {
			if (applicationInstaller.installApplication()) {
				ApplicationInfo applicationInfo = applicationInstaller.getApplicationInfo();
				Application application = applicationInfo.getApplication();
				if (application.getVersions().size() == 1) {
					ManagedApplication managedApplication = ManagedApplication.create()
							.setMainApplication(application)
							.setApplicationGroup(unspecifiedApplicationGroup)
							.setSingleApplication(applicationInfo.isUnmanagedPerspectives())
							.save();
					List<ApplicationPerspective> perspectives = application.getPerspectives().stream().filter(ApplicationPerspective::getAutoProvision).collect(Collectors.toList());
					for (ApplicationPerspective perspective : perspectives) {
						ManagedApplicationPerspective.create()
								.setManagedApplication(managedApplication)
								.setApplicationPerspective(perspective)
								.save();
					}
				}
			} else {
				LOGGER.error("Error installing " + applicationInstaller.getApplicationInfo().getName() + ": " + applicationInstaller.getApplicationInfo().getErrorMessage() + "\n" + "Warnings:" + applicationInstaller.getApplicationInfo().getWarningMessage());
				return false;
			}
		}
		loadApplication(applicationInstaller);
		return true;
	}

	public void loadApplication(ApplicationInstaller applicationInstaller) {
		try {
			LoadedApplication loadedApplication = applicationInstaller.loadApplication();
			updateGlobalLocalizationProvider();
			LOGGER.info("Loaded app:" + applicationInstaller.getApplicationInfo().getName());
			if (applicationInstaller.getApplicationInfo().getErrors().isEmpty()) {
				addLoadedApplication(loadedApplication);
			}
		} catch (Throwable e) {
			LOGGER.error("Error loading application: " + applicationInstaller.getApplicationInfo(), e);
		}
	}

	public void addLoadedApplication(LoadedApplication loadedApplication) {
		loadedApplicationMap.put(loadedApplication.getApplication(), loadedApplication);
	}

	public LoadedApplication getLoadedApplication(Application application) {
		return loadedApplicationMap.get(application);
	}

	public List<LoadedApplication> getLoadedApplications() {
		return new ArrayList<>(loadedApplicationMap.values());
	}

	public DictionaryLocalizationProvider getDictionary() {
		return dictionary;
	}

	public SystemLocalizationProvider getSystemDictionary() {
		return systemDictionary;
	}

	public Supplier<DocumentConverter> getDocumentConverterSupplier() {
		return () -> documentConverter;
	}

	public SystemConfig getSystemConfig() {
		return (SystemConfig) applicationConfig.getConfig();
	}

	public BootstrapSessionHandler getBootstrapSessionHandler() {
		return bootstrapSessionHandler;
	}

	public TranslationService getTranslationService() {
		return translationService;
	}

	public BaseResourceLinkProvider getBaseResourceLinkProvider() {
		return baseResourceLinkProvider;
	}

	public SessionRegistryHandler getSessionRegistryHandler() {
		return sessionRegistryHandler;
	}

	public void setSessionRegistryHandler(SessionRegistryHandler sessionRegistryHandler) {
		this.sessionRegistryHandler = sessionRegistryHandler;
	}

	public GlobalLocalizationProvider getGlobalLocalizationProvider() {
		return globalLocalizationProvider;
	}

	public void addAuthenticationHandler(AuthenticationHandler authenticationHandler) {
		authenticationHandlers.add(authenticationHandler);
	}

	public List<AuthenticationHandler> getAuthenticationHandlers() {
		return authenticationHandlers;
	}

	public UniversalDB getUniversalDB() {
		return universalDB;
	}

	public synchronized <ENTITY> void registerEntity(EntityBuilder<ENTITY> entityBuilder, int userId, Consumer<EntityUpdate<ENTITY>> listener) {
		entityUpdateEventHandler.registerEntity(entityBuilder, userId, listener);
	}

	public Cluster getCluster() {
		return cluster;
	}

	public void setCluster(Cluster cluster) {
		this.cluster = cluster;
	}

	public ServerRegistry getServerRegistry() {
		return serverRegistry;
	}

	public SessionManager getSessionManager() {
		return sessionManager;
	}

	public synchronized ReplicatedStateMachine getReplicatedStateMachine(String name, SessionContext context) {
		if (context == null) {
			return null;
		}
		MultiStateHandler multiStateHandler = stateHandlerMap.get(name);
		if (multiStateHandler == null) {
			multiStateHandler = new MultiStateHandler(name);
			stateHandlerMap.put(name, multiStateHandler);
		}
		ReplicatedStateMachine replicatedStateMachine = new ReplicatedStateMachine(multiStateHandler.getReplicatedState());
		multiStateHandler.addStateHandler(replicatedStateMachine, context);
		return replicatedStateMachine;
	}

}
