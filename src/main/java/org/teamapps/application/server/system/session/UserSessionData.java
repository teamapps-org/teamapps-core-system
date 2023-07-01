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
package org.teamapps.application.server.system.session;

import org.teamapps.application.api.desktop.ApplicationDesktop;
import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.application.api.privilege.ApplicationPrivilegeProvider;
import org.teamapps.application.api.state.ReplicatedStateMachine;
import org.teamapps.application.api.user.SessionUser;
import org.teamapps.application.server.system.bootstrap.ApplicationRootPanel;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.launcher.MobileApplicationNavigation;
import org.teamapps.application.server.system.launcher.OnlineUsersView;
import org.teamapps.application.server.system.localization.SessionApplicationLocalizationProvider;
import org.teamapps.application.server.system.privilege.PrivilegeApplicationKey;
import org.teamapps.application.server.system.privilege.UserPrivileges;
import org.teamapps.event.Event;
import org.teamapps.icons.Icon;
import org.teamapps.icons.SessionIconProvider;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.ManagedApplication;
import org.teamapps.model.controlcenter.Role;
import org.teamapps.model.controlcenter.User;
import org.teamapps.protocol.system.LoginData;
import org.teamapps.uisession.statistics.SumStats;
import org.teamapps.uisession.statistics.UiSessionStats;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.session.SessionContext;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class UserSessionData {

	private final User user;
	private final SessionContext context;
	private final SystemRegistry registry;
	private final ApplicationRootPanel rootPanel;
	private UserPrivileges userPrivileges;
	private final SessionUser sessionUser;
	private final SessionIconProvider iconProvider;
	private final List<String> localizationRankedLanguages;
	private final Map<Application, ApplicationLocalizationProvider> localizationProviderByApplication = new HashMap<>();
	private Supplier<ApplicationDesktop> applicationDesktopSupplier;
	private Function<Component, Component> rootWrapperComponentFunction;
	private final ApplicationLocalizationProvider localizationProvider;
	private boolean darkTheme;
	private LoginData loginData;
	private final Map<String, Event<?>> userSessionEventByName = new ConcurrentHashMap<>();
	private final Map<String, ReplicatedStateMachine> replicatedStateMachineMap = new HashMap<>();
	private OnlineUsersView onlineUsersView;
	private final Map<String, Object> customObjectsMap = new HashMap<>();

	public UserSessionData(User user, SessionContext context, SystemRegistry registry, ApplicationRootPanel rootPanel, Role authenticatedUserRole) {
		this.user = user;
		this.context = context;
		this.registry = registry;
		this.rootPanel = rootPanel;
		this.userPrivileges = new UserPrivileges(user, registry, authenticatedUserRole);
		this.sessionUser = new SessionUserImpl(this);
		this.localizationRankedLanguages = createLocalizationRankedLanguages();
		this.iconProvider = context.getIconProvider();
		this.localizationProvider = new SessionApplicationLocalizationProvider(null, localizationRankedLanguages, registry.getGlobalLocalizationProvider());
		context.onDestroyed.addListener(this::invalidate);
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

	public void setLoginData(LoginData loginData) {
		this.loginData = loginData;
	}

	public void addActivity() {
		loginData.setActivityCount(loginData.getActivityCount() + 1);
	}

	public void addOpenApplicationsCount() {
		loginData.setOpenApplicationsCount(loginData.getOpenApplicationsCount() + 1);
	}

	public void addOpenPerspectivesCount() {
		loginData.setOpenPerspectivesCount(loginData.getOpenPerspectivesCount() + 1);
	}

	public ManagedApplicationSessionData createManageApplicationSessionData(ManagedApplication managedApplication, MobileApplicationNavigation mobileNavigation) {
		return new ManagedApplicationSessionData(this, managedApplication, mobileNavigation);
	}

	public ApplicationPrivilegeProvider getApplicationPrivilegeProvider(ManagedApplication managedApplication) {
		return getUserPrivileges().getApplicationPrivilegeProvider(PrivilegeApplicationKey.create(managedApplication));
	}

	public ApplicationLocalizationProvider getApplicationLocalizationProvider(Application application) {
		if (!localizationProviderByApplication.containsKey(application)) {
			ApplicationLocalizationProvider applicationLocalizationProvider = createApplicationLocalizationProvider(application);
			localizationProviderByApplication.put(application, applicationLocalizationProvider);
		}
		return localizationProviderByApplication.get(application);
	}

	private ApplicationLocalizationProvider createApplicationLocalizationProvider(Application application) {
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

	public void invalidate() {
		sessionUser.onUserLogout().fire();
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
}
