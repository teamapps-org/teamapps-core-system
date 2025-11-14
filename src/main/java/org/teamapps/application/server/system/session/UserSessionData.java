/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
 * ---
 * Copyright (C) 2020 - 2024 TeamApps.org
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
package org.teamapps.application.server.system.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.desktop.ApplicationDesktop;
import org.teamapps.application.api.event.TwoWayBindableValueFireAlways;
import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.application.api.privilege.ApplicationPrivilegeProvider;
import org.teamapps.application.api.search.UserSearch;
import org.teamapps.application.api.state.ReplicatedStateMachine;
import org.teamapps.application.api.user.SessionUser;
import org.teamapps.application.api.user.UserAppConferenceHandler;
import org.teamapps.application.server.ServerData;
import org.teamapps.application.server.ServerMode;
import org.teamapps.application.server.system.bootstrap.ApplicationRootPanel;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.launcher.ApplicationLauncher;
import org.teamapps.application.server.system.launcher.MobileApplicationNavigation;
import org.teamapps.application.server.system.launcher.OnlineUsersView;
import org.teamapps.application.server.system.localization.SessionApplicationLocalizationProvider;
import org.teamapps.application.server.system.privilege.PrivilegeApplicationKey;
import org.teamapps.application.server.system.privilege.UserPrivileges;
import org.teamapps.application.server.system.search.UserSearchImpl;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.event.Event;
import org.teamapps.icons.Icon;
import org.teamapps.icons.SessionIconProvider;
import org.teamapps.model.controlcenter.*;
import org.teamapps.protocol.system.LoginData;
import org.teamapps.uisession.statistics.SumStats;
import org.teamapps.uisession.statistics.UiSessionStats;
import org.teamapps.ux.application.perspective.Perspective;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.session.SessionContext;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class UserSessionData {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public Event<Void> onLauncherSelection = new Event<>();

	private final User user;
	private final SessionContext context;
	private final SystemRegistry registry;
	private final ApplicationRootPanel rootPanel;
	private final Role authenticatedUserRole;
	private final boolean appLogin;
	private final UserAppConferenceHandler userAppConferenceHandler;
	private final SessionUser sessionUser;
	private final SessionIconProvider iconProvider;
	private final List<String> localizationRankedLanguages;
	private final Map<Application, ApplicationLocalizationProvider> localizationProviderByApplication = new HashMap<>();
	private final ApplicationLocalizationProvider localizationProvider;
	private final Map<String, Event<?>> userSessionEventByName = new ConcurrentHashMap<>();
	private final Map<String, TwoWayBindableValue<?>> userSessionBindableValueByName = new ConcurrentHashMap<>();
	private final Map<String, ReplicatedStateMachine> replicatedStateMachineMap = new HashMap<>();
	private final Map<String, Object> customObjectsMap = new HashMap<>();
	private UserPrivileges userPrivileges;
	private Supplier<ApplicationDesktop> applicationDesktopSupplier;
	private Function<Component, Component> rootWrapperComponentFunction;
	private boolean darkTheme;
	private LoginData loginData;
	private OnlineUsersView onlineUsersView;
	private ApplicationLauncher applicationLauncher;
	private Perspective desktopLauncherPerspective;

	private Set<Integer> groupIds = new HashSet<>();
	private Set<Integer> groupModerationIds = new HashSet<>();

	public UserSessionData(User user, SessionContext context, SystemRegistry registry, ApplicationRootPanel rootPanel, Role authenticatedUserRole) {
		this(user, context, registry, rootPanel, authenticatedUserRole, false, null);
	}

	public UserSessionData(User user, SessionContext context, SystemRegistry registry, ApplicationRootPanel rootPanel, Role authenticatedUserRole, boolean appLogin, UserAppConferenceHandler userAppConferenceHandler) {
		this.user = user;
		this.context = context;
		this.registry = registry;
		this.rootPanel = rootPanel;
		this.authenticatedUserRole = authenticatedUserRole;
		this.appLogin = appLogin;
		this.userAppConferenceHandler = userAppConferenceHandler;
		this.userPrivileges = new UserPrivileges(user, registry, authenticatedUserRole);
		this.sessionUser = new SessionUserImpl(this);
		this.localizationRankedLanguages = createLocalizationRankedLanguages();
		this.iconProvider = context.getIconProvider();
		this.localizationProvider = new SessionApplicationLocalizationProvider(null, localizationRankedLanguages, registry.getGlobalLocalizationProvider());
		context.onDestroyed.addListener(this::invalidate);
	}

	public boolean hasSkippedMultiFactorRequiringPrivileges() {
		return userPrivileges.hasSkippedMultiFactorRequiringPrivileges();
	}

	public void reloadPrivileges() {
		this.userPrivileges = new UserPrivileges(user, registry, authenticatedUserRole);
		applicationLauncher.reloadUserPrivileges();
	}

	private List<String> createLocalizationRankedLanguages() {
		HashSet<String> languages = new HashSet<>(sessionUser.getRankedLanguages());
		if (!languages.contains("en")) {
			ArrayList<String> rankedLanguages = new ArrayList<>(sessionUser.getRankedLanguages());
			rankedLanguages.add("en");
			return rankedLanguages;
		} else {
			return sessionUser.getRankedLanguages();
		}
	}

	public void updateDisplayLanguage(String language) {
		localizationRankedLanguages.remove(language);
		localizationRankedLanguages.add(0, language);
	}

	public void setLoginData(LoginData loginData) {
		this.loginData = loginData;
	}

	public void addActivity() {
		if (loginData == null) {
			LOGGER.error("Error: missing login data");
			return;
		}
		loginData.setActivityCount(loginData.getActivityCount() + 1);
	}

	public void addOpenApplicationsCount() {
		if (loginData == null) {
			LOGGER.error("Error: missing login data");
			return;
		}
		loginData.setOpenApplicationsCount(loginData.getOpenApplicationsCount() + 1);
	}

	public void addOpenPerspectivesCount() {
		if (loginData == null) {
			LOGGER.error("Error: missing login data");
			return;
		}
		loginData.setOpenPerspectivesCount(loginData.getOpenPerspectivesCount() + 1);
	}

	public ManagedApplicationSessionData createManageApplicationSessionData(ManagedApplication managedApplication, MobileApplicationNavigation mobileNavigation) {
		return new ManagedApplicationSessionData(this, managedApplication, mobileNavigation);
	}

	public ApplicationPrivilegeProvider getApplicationPrivilegeProvider(ManagedApplication managedApplication) {
		return getUserPrivileges().getApplicationPrivilegeProvider(PrivilegeApplicationKey.create(managedApplication));
	}

	public ApplicationPrivilegeProvider getApplicationPrivilegeProviderWithOrgField(ManagedApplication managedApplication) {
		return getUserPrivileges().getApplicationPrivilegeProvider(PrivilegeApplicationKey.create(managedApplication.getMainApplication(), managedApplication.getOrganizationField() != null ? OrganizationField.getById(managedApplication.getOrganizationField().getId()) : null));
	}

	public ApplicationLocalizationProvider getApplicationLocalizationProvider(Application application) {
		if (!localizationProviderByApplication.containsKey(application)) {
			ApplicationLocalizationProvider applicationLocalizationProvider = createApplicationLocalizationProvider(application);
			localizationProviderByApplication.put(application, applicationLocalizationProvider);
		}
		return localizationProviderByApplication.get(application);
	}

	public ApplicationLocalizationProvider createApplicationLocalizationProvider(Application application) {
		return new SessionApplicationLocalizationProvider(application, localizationRankedLanguages, registry.getGlobalLocalizationProvider());
	}

	public Icon<?, ?> decodeIcon(String name) {
		if (name == null) {
			return null;
		}
		try {
			return context.getIconProvider().decodeIcon(name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public User getUser() {
		return user;
	}

	public SessionContext getContext() {
		return context;
	}

	public SystemRegistry getRegistry() {
		return registry;
	}

	public ApplicationRootPanel getRootPanel() {
		return rootPanel;
	}

	public ApplicationLauncher getApplicationLauncher() {
		return applicationLauncher;
	}

	public void setApplicationLauncher(ApplicationLauncher applicationLauncher) {
		this.applicationLauncher = applicationLauncher;
	}

	public Perspective getDesktopLauncherPerspective() {
		return desktopLauncherPerspective;
	}

	public void setDesktopLauncherPerspective(Perspective desktopLauncherPerspective) {
		this.desktopLauncherPerspective = desktopLauncherPerspective;
	}

	public void setRooWrapperComponentFunction(Function<Component, Component> rootWrapperComponentFunction) {
		this.rootWrapperComponentFunction = rootWrapperComponentFunction;
		rootPanel.setContent(rootWrapperComponentFunction.apply(rootPanel.getContent()));
	}

	public void setRootComponent(Component component) {
		if (rootWrapperComponentFunction != null) {
			rootPanel.setContent(rootWrapperComponentFunction.apply(component));
		} else {
			rootPanel.setContent(component);
		}
	}

	public SessionUser getSessionUser() {
		return sessionUser;
	}

	public SessionIconProvider getIconProvider() {
		return iconProvider;
	}

	public ApplicationLocalizationProvider getLocalizationProvider() {
		return localizationProvider;
	}

	public UserPrivileges getUserPrivileges() {
		return userPrivileges;
	}

	public boolean isAppLogin() {
		return appLogin;
	}

	public UserAppConferenceHandler getUserAppConferenceHandler() {
		return userAppConferenceHandler;
	}

	public void invalidate() {
		sessionUser.onUserLogout().fireIgnoringExceptions(null);
		userPrivileges = null;
		if (loginData != null) {
			UiSessionStats sessionStats = sessionUser.getSessionContext().getUiSessionStats();
			loginData.setLogoutTimestamp((int) Instant.now().getEpochSecond());

			String sessionId = sessionStats.getSessionId();
			SumStats receivedDataStats = sessionStats.getReceivedDataStats();
			SumStats sentDataStats = sessionStats.getSentDataStats();

			//todo store data...


			loginData = null;
		}
	}

	public List<String> getRankedLanguages() {
		return sessionUser.getRankedLanguages();
	}

	public Supplier<ApplicationDesktop> getApplicationDesktopSupplier() {
		return applicationDesktopSupplier;
	}

	public void setApplicationDesktopSupplier(Supplier<ApplicationDesktop> applicationDesktopSupplier) {
		this.applicationDesktopSupplier = applicationDesktopSupplier;
	}

	public boolean isDarkTheme() {
		return darkTheme;
	}

	public void setDarkTheme(boolean darkTheme) {
		this.darkTheme = darkTheme;
	}

	public <TYPE> Event<TYPE> getUserSessionEvent(String name) {
		return (Event<TYPE>) userSessionEventByName.computeIfAbsent(name, s -> new Event<>());
	}

	public <TYPE> TwoWayBindableValue<TYPE> getBindableValue(String name) {
		return getBindableValue(name, true);
	}

	public <TYPE> TwoWayBindableValue<TYPE> getBindableValue(String name, boolean fireAlways) {
		return (TwoWayBindableValue<TYPE>) userSessionBindableValueByName.computeIfAbsent(name, s -> fireAlways ? TwoWayBindableValueFireAlways.create() : TwoWayBindableValue.create());
	}

	public synchronized ReplicatedStateMachine getReplicatedStateMachine(String name) {
		if (replicatedStateMachineMap.containsKey(name)) {
			return replicatedStateMachineMap.get(name);
		} else {
			ReplicatedStateMachine replicatedStateMachine = registry.getReplicatedStateMachine(name, SessionContext.current());
			replicatedStateMachineMap.put(name, replicatedStateMachine);
			return replicatedStateMachine;
		}
	}

	public OnlineUsersView getOnlineUsersView() {
		return onlineUsersView;
	}

	public void setOnlineUsersView(OnlineUsersView onlineUsersView) {
		this.onlineUsersView = onlineUsersView;
	}

	public Map<String, Object> getCustomObjectsMap() {
		return customObjectsMap;
	}

	public UserSearch createUserSearch(String authCode, ApplicationInstanceData applicationInstanceData) {
		if ("9FG7JBZHI3MBN".equals(authCode)) { //todo move to config!
			return new UserSearchImpl(authCode, applicationInstanceData);
		}
		return null;
	}

	public ServerMode getServerMode() {
		return ServerData.getServerMode();
	}

	public Set<Integer> getGroupIds() {
		return groupIds;
	}

	public void setGroupIds(Set<Integer> groupIds) {
		this.groupIds = groupIds;
	}

	public Set<Integer> getGroupModerationIds() {
		return groupModerationIds;
	}

	public void setGroupModerationIds(Set<Integer> groupModerationIds) {
		this.groupModerationIds = groupModerationIds;
	}
}
