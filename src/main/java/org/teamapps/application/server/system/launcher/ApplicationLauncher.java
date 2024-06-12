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
package org.teamapps.application.server.system.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.BaseApplicationBuilder;
import org.teamapps.application.api.application.UserProfileApplicationBuilder;
import org.teamapps.application.api.application.perspective.PerspectiveBuilder;
import org.teamapps.application.api.application.theme.ApplicationTheme;
import org.teamapps.application.api.application.theme.CustomApplicationTheme;
import org.teamapps.application.api.desktop.ApplicationDesktop;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.localization.Language;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.DatabaseLogAppender;
import org.teamapps.application.server.PublicLinkResourceProvider;
import org.teamapps.application.server.system.auth.LoginHandler;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.bootstrap.LogoutHandler;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.config.ThemingConfig;
import org.teamapps.application.server.system.session.ManagedApplicationSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.common.format.Color;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.protocol.system.LoginData;
import org.teamapps.universaldb.UniversalDB;
import org.teamapps.ux.application.ResponsiveApplication;
import org.teamapps.ux.application.assembler.DesktopApplicationAssembler;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.perspective.Perspective;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.absolutelayout.Length;
import org.teamapps.ux.component.dialogue.FormDialogue;
import org.teamapps.ux.component.field.FieldEditingMode;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.TextField;
import org.teamapps.ux.component.itemview.SimpleItem;
import org.teamapps.ux.component.itemview.SimpleItemGroup;
import org.teamapps.ux.component.itemview.SimpleItemView;
import org.teamapps.ux.component.panel.Panel;
import org.teamapps.ux.component.tabpanel.Tab;
import org.teamapps.ux.component.tabpanel.TabPanel;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbutton.ToolButton;
import org.teamapps.ux.component.workspacelayout.SplitDirection;
import org.teamapps.ux.component.workspacelayout.definition.SplitPaneDefinition;
import org.teamapps.ux.component.workspacelayout.definition.SplitSize;
import org.teamapps.ux.component.workspacelayout.definition.ViewGroupDefinition;
import org.teamapps.ux.resource.ByteArrayResource;
import org.teamapps.ux.session.ClientInfo;
import org.teamapps.ux.session.SessionConfiguration;
import org.teamapps.ux.session.SessionContext;
import org.teamapps.ux.session.StylingTheme;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ApplicationLauncher {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final UserSessionData userSessionData;
	private final LogoutHandler logoutHandler;
	private final SystemRegistry registry;
	private final boolean mobileView;
	private final Map<String, String> registeredPublicBackgroundImageMap = new ConcurrentHashMap<>();
	private List<ApplicationGroupData> sortedApplicationGroups;
	private Component applicationLauncher;
	private Set<ApplicationData> openedApplications = new HashSet<>();
	private Map<ApplicationData, Tab> tabByApplicationData = new HashMap<>();
	private Map<Tab, ApplicationData> applicationDataByTab = new HashMap<>();
	private Map<Tab, Runnable> themeRunnableByDesktopTab = new HashMap<>();
	private Map<ApplicationData, Component> mobilAppByApplicationData = new HashMap<>();
	private Map<ApplicationData, ApplicationInstance> openedApplicationInstanceByApplicationData = new HashMap<>();
	private TabPanel applicationsTabPanel;
	private TwoWayBindableValue<ManagedApplication> selectedApplication = TwoWayBindableValue.create();
	private TwoWayBindableValue<ManagedApplicationPerspective> selectedPerspective = TwoWayBindableValue.create();
	private Tab applicationLauncherTab;
	private ApplicationData userProfileApp;
	private View launcherView;

	public ApplicationLauncher(UserSessionData userSessionData, LogoutHandler logoutHandler) {
		this.userSessionData = userSessionData;
		userSessionData.setApplicationLauncher(this);
		this.registry = userSessionData.getRegistry();
		ClientInfo clientInfo = userSessionData.getContext().getClientInfo();
		this.mobileView = clientInfo.isMobileDevice();
		this.logoutHandler = logoutHandler;
		User user = userSessionData.getUser();
		userSessionData.setLoginData(createLoginData(user.getId(), clientInfo));

		userSessionData.getContext().addExecutionDecorator(runnable -> {
			try {
				UniversalDB.setUserId(user.getId());
				DatabaseLogAppender.THREAD_LOCAL_MANAGED_APPLICATION.set(selectedApplication.get() != null ? selectedApplication.get().getId() : 0);
				DatabaseLogAppender.THREAD_LOCAL_APPLICATION_VERSION.set(getCurrentApplicationVersion(selectedApplication.get()));
				DatabaseLogAppender.THREAD_LOCAL_MANAGED_PERSPECTIVE.set(selectedPerspective.get() != null ? selectedPerspective.get().getId() : 0);
				userSessionData.addActivity();
				runnable.run();
			} catch (Throwable e) {
				LOGGER.error("Application crash", e);
				handleSessionException(e);
			} finally {
				UniversalDB.setUserId(0);
				DatabaseLogAppender.THREAD_LOCAL_MANAGED_APPLICATION.set(null);
				DatabaseLogAppender.THREAD_LOCAL_APPLICATION_VERSION.set(null);
				DatabaseLogAppender.THREAD_LOCAL_MANAGED_PERSPECTIVE.set(null);
			}
		}, false);
		selectedApplication.onChanged().addListener(this::handleApplicationSelection);
		selectedPerspective.onChanged().addListener(this::handlePerspectiveSelection);
		UserLoginStats loginStats = user.getLoginStats();
		if (loginStats == null) {
			loginStats = UserLoginStats.create()
					.setFirstLogin(Instant.now())
					.setUser(user)
					.save();
		}
		loginStats
				.setLastLogin(Instant.now())
				.setLoginCount(loginStats.getLoginCount() + 1)
				.setLastLoginIpAddress(userSessionData.getContext().getClientInfo().getIp())
				.save();
		setLauncherTheme();
		userSessionData.setApplicationDesktopSupplier(this::createApplicationDesktop);
		initApplicationData();
		createApplicationLauncher();
		createMainView();
	}

	private String getCurrentApplicationVersion(ManagedApplication managedApplication) {
		if (managedApplication != null && managedApplication.getMainApplication() != null && managedApplication.getMainApplication().getInstalledVersion() != null) {
			return managedApplication.getMainApplication().getInstalledVersion().getVersion();
		} else {
			return null;
		}
	}

	private LoginData createLoginData(int userId, ClientInfo clientInfo) {
		return new LoginData()
				.setUserId(userId)
				.setLoginTimestamp((int) Instant.now().getEpochSecond())
				.setIp(clientInfo.getIp())
				.setUserAgent(clientInfo.getUserAgent())
				.setMobileDevice(clientInfo.isMobileDevice())
				.setScreenWidth(clientInfo.getScreenWidth())
				.setScreenHeight(clientInfo.getScreenHeight());
	}

	private void handleSessionException(Throwable e) {
		ManagedApplication managedApplication = selectedApplication.get();
		ManagedApplicationPerspective perspective = selectedPerspective.get();
		closeApplication(managedApplication);
		FormDialogue dialogue = FormDialogue.create(ApplicationIcons.SIGN_WARNING, getLocalized(Dictionary.ERROR), getLocalized(Dictionary.SENTENCE_ERROR_THE_ACTIVE_APPLICATION_CAUSED__));
		TemplateField<ManagedApplication> managedApplicationField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createManagedApplicationPropertyProvider(userSessionData));
		TemplateField<ManagedApplicationPerspective> perspectiveField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.createManagedApplicationPerspectivePropertyProvider(userSessionData));
		TextField errorField = new TextField();
		errorField.setEditingMode(FieldEditingMode.READONLY);
		managedApplicationField.setValue(managedApplication);
		perspectiveField.setValue(perspective);
		errorField.setValue(e.getMessage());
		dialogue.addField(null, getLocalized(Dictionary.APPLICATION), managedApplicationField);
		dialogue.addField(null, getLocalized(Dictionary.APPLICATION_PERSPECTIVE), perspectiveField);
		dialogue.addField(null, getLocalized(Dictionary.ERROR), errorField);
		dialogue.addOkButton(getLocalized(Dictionary.O_K));
		dialogue.setCloseOnEscape(true);
		dialogue.setAutoCloseOnOk(true);
		dialogue.setCloseable(true);
		dialogue.enableAutoHeight();
		dialogue.show();
	}

	private void handleApplicationSelection(ManagedApplication application) {
		if (application == null) {
			setLauncherTheme();
			return;
		}
		DatabaseLogAppender.THREAD_LOCAL_MANAGED_APPLICATION.set(application.getId());
		DatabaseLogAppender.THREAD_LOCAL_APPLICATION_VERSION.set(getCurrentApplicationVersion(application));
		DatabaseLogAppender.THREAD_LOCAL_MANAGED_PERSPECTIVE.set(null);
		setApplicationTheme(application);
	}

	private void handlePerspectiveSelection(ManagedApplicationPerspective perspective) {
		ManagedApplication application = perspective.getManagedApplication();
		DatabaseLogAppender.THREAD_LOCAL_MANAGED_APPLICATION.set(application != null ? application.getId() : 0);
		DatabaseLogAppender.THREAD_LOCAL_APPLICATION_VERSION.set(getCurrentApplicationVersion(application));
		DatabaseLogAppender.THREAD_LOCAL_MANAGED_PERSPECTIVE.set(perspective.getId());
	}

	private void registerApplicationTheme(LoadedApplication loadedApplication) {
		if (loadedApplication != null && loadedApplication.getBaseApplicationBuilder().getApplicationTheme() != null) {
			String applicationName = loadedApplication.getApplication().getName();
			ApplicationTheme theme = loadedApplication.getBaseApplicationBuilder().getApplicationTheme();
			if (theme.getBrightTheme() != null) {
				CustomApplicationTheme customTheme = theme.getBrightTheme();
				if (customTheme.getBackgroundImage() != null) {
					registerBackgroundImage(applicationName, customTheme.getBackgroundImage(), false);
				}
				if (customTheme.getCustomCss() != null) {
					//todo differentiate between dark and bright theme!
					userSessionData.getRootPanel().setApplicationStyles(loadedApplication.getBaseApplicationBuilder().getApplicationName(), customTheme.getCustomCss());
				}
			}
			if (theme.getDarkTheme() != null) {
				CustomApplicationTheme customTheme = theme.getDarkTheme();
				if (customTheme.getBackgroundImage() != null) {
					registerBackgroundImage(applicationName, customTheme.getBackgroundImage(), true);
				}
			}
		}
	}

	private void registerBackgroundImage(String applicationName, byte[] bytes, boolean darkTheme) {
		String key = applicationName + "-" + (darkTheme ? "dark" : "bright");
		String link = registeredPublicBackgroundImageMap.get(key);
		if (link == null) {
			link = PublicLinkResourceProvider.getInstance().createStaticResource(key + "-" + bytes.length, new ByteArrayResource(bytes, key + ".jpg"));
			registeredPublicBackgroundImageMap.put(key, link);
		}
		userSessionData.getContext().registerBackgroundImage(key, link, link);
	}

	private void setApplicationTheme(ManagedApplication application) {
		boolean darkTheme = application.isDarkTheme() || userSessionData.getUser().isDarkTheme();
		setApplicationTheme(application, darkTheme);
	}

	private void setApplicationTheme(ManagedApplication application, boolean darkTheme) {
		if (application == null) {
			return;
		}
		LoadedApplication loadedApplication = registry.getLoadedApplication(application.getMainApplication());
		String applicationName = loadedApplication.getApplication().getName();
		String key = applicationName + "-" + (darkTheme ? "dark" : "bright");
		if (registeredPublicBackgroundImageMap.containsKey(key)) {
			userSessionData.getContext().setBackgroundImage(key, 1_000);
		} else {
			userSessionData.getContext().setBackgroundImage(darkTheme ? "defaultDarkBackground" : "defaultBackground", 500);
		}
		SessionConfiguration configuration = SessionContext.current().getConfiguration();
		configuration.setTheme(darkTheme ? StylingTheme.DARK : StylingTheme.DEFAULT);
		SessionContext.current().setConfiguration(configuration);
	}

	private void setLauncherTheme() {
		boolean darkTheme = userSessionData.getUser().isDarkTheme();
		userSessionData.getContext().setBackgroundImage(darkTheme ? "defaultDarkBackground" : "defaultBackground", 500);
		SessionConfiguration configuration = SessionContext.current().getConfiguration();
		configuration.setTheme(darkTheme ? StylingTheme.DARK : StylingTheme.DEFAULT);
		SessionContext.current().setConfiguration(configuration);
	}

	private void handleApplicationLauncherSelection() {
		selectedApplication.set(null);
		if (userSessionData.getOnlineUsersView() != null) {
			userSessionData.getOnlineUsersView().refresh();
		}
	}

	private void initApplicationData() {
		List<ApplicationGroupData> applicationGroups = new ArrayList<>();
		for (ManagedApplicationGroup applicationGroup : ManagedApplicationGroup.getAll()) {
			ApplicationGroupData applicationGroupData = new ApplicationGroupData(applicationGroup, userSessionData);
			applicationGroups.add(applicationGroupData);
			for (ManagedApplication managedApplication : applicationGroup.getApplications()) {
				Application application = managedApplication.getMainApplication();
				LoadedApplication loadedApplication = registry.getLoadedApplication(application);
				if (loadedApplication != null && loadedApplication.getBaseApplicationBuilder().isApplicationAccessible(userSessionData.getApplicationPrivilegeProvider(managedApplication))) {
					ManagedApplicationSessionData applicationSessionData = userSessionData.createManageApplicationSessionData(managedApplication, new MobileApplicationNavigation());
					ApplicationData applicationData = new ApplicationData(managedApplication, loadedApplication, applicationSessionData);
					applicationGroupData.addApplicationData(applicationData);
					if (applicationData.getLoadedApplication().getBaseApplicationBuilder() instanceof UserProfileApplicationBuilder) {
						userProfileApp = applicationData;
					}
				}
			}
		}
		sortedApplicationGroups = ApplicationGroupData.getSortedGroups(applicationGroups.stream().filter(group -> !group.getSortedApplications().isEmpty()).collect(Collectors.toList()));
	}

	private void createApplicationLauncher() {
		SimpleItemView<ApplicationData> itemView = new SimpleItemView<>();
		for (ApplicationGroupData applicationGroup : sortedApplicationGroups) {
			SimpleItemGroup<ApplicationData> itemGroup = new SimpleItemGroup<>(applicationGroup.getIcon(), applicationGroup.getTitle(), BaseTemplate.LIST_ITEM_EXTRA_VERY_LARGE_ICON_TWO_LINES);
			itemView.addGroup(itemGroup);
			itemGroup.setButtonWidth(220);
			for (ApplicationData applicationData : applicationGroup.getSortedApplications()) {
				SimpleItem<ApplicationData> item = itemGroup.addItem(applicationData.getIcon(), applicationData.getTitle(), applicationData.getDescription());
				item.onClick.addListener(() -> openApplication(applicationData));
			}
		}
		if (mobileView) {
			SimpleItemGroup<ApplicationData> itemGroup = new SimpleItemGroup<>(ApplicationIcons.LOG_OUT, getLocalized(Dictionary.LOGOUT), BaseTemplate.LIST_ITEM_EXTRA_VERY_LARGE_ICON_TWO_LINES);
			itemView.addGroup(itemGroup);
			itemGroup.setButtonWidth(220);
			SimpleItem<ApplicationData> item = itemGroup.addItem(ApplicationIcons.LOG_OUT, getLocalized(Dictionary.LOGOUT), getLocalized(Dictionary.LOGOUT));
			item.onClick.addListener(this::logout);
		}
		createLauncherView(itemView, mobileView);
	}

	public void reloadUserPrivileges() {
		openedApplications.clear();
		tabByApplicationData.clear();
		applicationDataByTab.clear();
		initApplicationData();
		createApplicationLauncher();
		createMainView();
	}

	private void logout() {
		User user = userSessionData.getUser();
		LOGGER.info("User logout: {}, {} {}", user.getId(), user.getFirstName(), user.getLastName());
		registry.getBootstrapSessionHandler().onUserLogout.fireIgnoringExceptions(userSessionData.getContext());
		userSessionData.invalidate();
		userSessionData.getRegistry().removeActiveUser(userSessionData);
		SessionContext.current().clearExecutionDecorators();
		LoginHandler loginHandler = new LoginHandler(registry, logoutHandler, userSessionData);
		loginHandler.createLoginView(userSessionData.getContext(), userSessionData.getRootPanel());
	}

	private void createMainView() {
		ThemingConfig themingConfig = registry.getSystemConfig().getThemingConfig();
		setLauncherTheme();

		for (LoadedApplication loadedApplication : registry.getLoadedApplications()) {
			registerApplicationTheme(loadedApplication);
		}
		if (themingConfig.getBaseStyles() != null) {
			userSessionData.getRootPanel().setBaseStyles(themingConfig.getBaseStyles());
		}

		if (mobileView) {
			userSessionData.setRootComponent(applicationLauncher);
		} else {
			applicationsTabPanel = new TabPanel();
			applicationsTabPanel.onTabSelected.addListener(tab -> {
				ApplicationData applicationData = applicationDataByTab.get(tab);
				if (applicationData != null) {
					selectedApplication.set(applicationData.getManagedApplication());
				}
				if (themeRunnableByDesktopTab.containsKey(tab)) {
					themeRunnableByDesktopTab.get(tab).run();
				}
				if (tab.equals(applicationLauncherTab)) {
					handleApplicationLauncherSelection();
				}
			});
			ResponsiveApplication application = ResponsiveApplication.createApplication();

			Perspective perspective = Perspective.createPerspective(new SplitPaneDefinition("split", SplitDirection.VERTICAL, SplitSize.lastFixed(200f), new ViewGroupDefinition(StandardLayout.CENTER, true), new ViewGroupDefinition(StandardLayout.RIGHT, true)));
			application.showPerspective(perspective);
			perspective.addView(launcherView);
			if (userSessionData.getOnlineUsersView() != null) {
				View onlineUsers = View.createView(StandardLayout.RIGHT, ApplicationIcons.USERS_CROWD, "Online Users", userSessionData.getOnlineUsersView().getComponent());
				onlineUsers.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.74f));
				perspective.addView(onlineUsers);
			}
			applicationLauncherTab = new Tab(ApplicationIcons.HOME, getLocalized(Dictionary.APPLICATIONS), application.getUi());
			applicationsTabPanel.addTab(applicationLauncherTab, true);
			applicationsTabPanel.setTabBarHeight(Length.ofPixels(24));

			Tab logoutTab = new Tab(ApplicationIcons.LOG_OUT, getLocalized(Dictionary.LOGOUT), null);
			logoutTab.setLazyLoading(true);
			logoutTab.setRightSide(true);
			Language language = Language.getLanguageByIsoCode(userSessionData.getSessionUser().getLanguage());
			ToolButton profileButton = new ToolButton(language.getIcon());
			profileButton.setCaption(PropertyProviders.getUserCaptionWithTranslation(userSessionData.getUser()));
			SimpleItemView<?> itemView = new SimpleItemView<>();
			if (userProfileApp != null) {
				BaseApplicationBuilder baseApplicationBuilder = userProfileApp.getLoadedApplication().getBaseApplicationBuilder();
				UserProfileApplicationBuilder builder = (UserProfileApplicationBuilder) baseApplicationBuilder;
				SimpleItemGroup<?> itemGroup = itemView.addSingleColumnGroup(baseApplicationBuilder.getApplicationIcon(), getLocalized(baseApplicationBuilder.getApplicationTitleKey()));
				for (PerspectiveBuilder perspectiveBuilder : builder.getUserProfilePerspectiveBuilders()) {
					itemGroup.addItem(perspectiveBuilder.getIcon(), getLocalized(perspectiveBuilder.getTitleKey()), getLocalized(perspectiveBuilder.getDescriptionKey())).onClick.addListener(() -> {
						openApplicationWithPerspective(userProfileApp, perspectiveBuilder);
					});
				}
			}
			SimpleItemGroup<?> itemGroup = itemView.addSingleColumnGroup(ApplicationIcons.LOG_OUT, getLocalized(Dictionary.LOGOUT));
			itemGroup.addItem(ApplicationIcons.LOG_OUT, getLocalized(Dictionary.LOGOUT), "Vom System abmelden").onClick.addListener(this::logout); //todo dictionary entry
			profileButton.setDropDownComponent(itemView);
			profileButton.setMinDropDownWidth(250);
			profileButton.setIconSize(20);
			applicationsTabPanel.addToolButton(profileButton);
			userSessionData.setRootComponent(applicationsTabPanel);
		}

		ApplicationData autoStartApp = getAllApplications().stream().filter(applicationData -> applicationData.getManagedApplication().isStartOnLogin()).findFirst().orElse(null);
		if (autoStartApp != null) {
			this.openApplication(autoStartApp);
		}
	}

	private void closeApplication(ManagedApplication managedApplication) {
		if (managedApplication == null) {
			return;
		}
		ApplicationData runningApplication = null;
		for (ApplicationData applicationData : openedApplications) {
			if (applicationData.getManagedApplication().equals(managedApplication)) {
				runningApplication = applicationData;
			}
		}
		if (runningApplication != null) {
			if (mobileView) {
				//todo
			} else {
				Tab tab = tabByApplicationData.get(runningApplication);
				if (tab != null) {
					//todo get from app data all app created tabs and close them as well
					applicationsTabPanel.removeTab(tab);
					tabByApplicationData.remove(runningApplication);
					openedApplications.remove(runningApplication);
					runningApplication.reloadApplicationData(userSessionData);
					applicationDataByTab.remove(tab);
				}
			}
		}
	}

	public ApplicationInstanceData createApplicationInstanceData(String appName) {
		ApplicationData applicationData = sortedApplicationGroups.stream().flatMap(group -> group.getSortedApplications().stream()).filter(app -> app.getLoadedApplication().getApplication().getName().equals(appName)).findFirst().orElse(null);
		ApplicationInstance applicationInstance = new ApplicationInstance(userSessionData, applicationData, applicationLauncher, selectedPerspective);
		ManagedApplicationPerspective managedApplicationPerspective = applicationData.getManagedApplication().getPerspectives().stream().filter(p -> p.getApplicationPerspective().getApplication().getName().equals(appName)).findFirst().orElse(null);
		return applicationInstance.createPerspectiveSessionData(managedApplicationPerspective);
	}

	private void openApplicationWithPerspective(ApplicationData applicationData, PerspectiveBuilder perspectiveBuilder) {
		openApplication(applicationData);
		openedApplicationInstanceByApplicationData.get(applicationData).showApplicationPerspective(perspectiveBuilder.getName());
	}

	private void openApplication(ApplicationData applicationData) {
		selectedApplication.set(applicationData.getManagedApplication());
		userSessionData.addOpenApplicationsCount();
		LOGGER.info("Open app: " + applicationData.getManagedApplication().getQualifiedName()); //+ (selectedPerspective.get() != null ? selectedPerspective.get().getApplicationPerspective().getQualifiedName() : null));
		if (openedApplications.contains(applicationData)) {
			userSessionData.setDarkTheme(applicationData.getManagedApplication().isDarkTheme());
			if (mobileView) {
				Component component = mobilAppByApplicationData.get(applicationData);
				userSessionData.setRootComponent(component);
				applicationData.getApplicationSessionData().getMobileNavigation().onShowStartViewRequest().fire();
			} else {
				Tab tab = tabByApplicationData.get(applicationData);
				tab.select();
			}
		} else {
			ApplicationInstance applicationInstance = new ApplicationInstance(userSessionData, applicationData, applicationLauncher, selectedPerspective);
			openedApplicationInstanceByApplicationData.put(applicationData, applicationInstance);
			userSessionData.setDarkTheme(applicationData.getManagedApplication().isDarkTheme());
			if (mobileView) {
				Component application = applicationInstance.createMobileApplication();
				userSessionData.setRootComponent(application);
				openedApplications.add(applicationData);
				mobilAppByApplicationData.put(applicationData, application);
			} else {
				Component application = applicationInstance.createApplication();
				Tab tab = new Tab(applicationData.getIcon(), applicationData.getTitle(), application);
				tab.setCloseable(true);
				openedApplications.add(applicationData);
				tabByApplicationData.put(applicationData, tab);
				applicationDataByTab.put(tab, applicationData);
				tab.onClosed.addListener(() -> {
					tabByApplicationData.remove(applicationData);
					applicationDataByTab.remove(tab);
					openedApplications.remove(applicationData);
					applicationData.reloadApplicationData(userSessionData);
				});
				applicationsTabPanel.addTab(tab, true);
			}
		}
	}

	public ApplicationDesktop createApplicationDesktop() {
		final ManagedApplication managedApplication = selectedApplication.get();
		ApplicationDesktop applicationDesktop = new ApplicationDesktop() {
			private final ResponsiveApplication application = createResponsiveApplication();
			private Tab tab;
			private boolean darkTheme;

			@Override
			public ResponsiveApplication getApplication() {
				return application;
			}

			@Override
			public void showApplication(Icon icon, String title, boolean select, boolean closable) {
				if (mobileView) {
					userSessionData.setRootComponent(application.getUi());
				} else {
					tab = new Tab(icon, title, application.getUi());
					tab.setCloseable(closable);
					applicationsTabPanel.addTab(tab, select);
					themeRunnableByDesktopTab.put(tab, () -> setApplicationTheme(managedApplication, darkTheme));
					ApplicationData applicationData = ApplicationGroupData.getApplicationData(managedApplication, sortedApplicationGroups);
					if (applicationData != null) {
						applicationDataByTab.put(tab, applicationData);
					}
				}
			}

			@Override
			public void setDarkTheme(boolean darkTheme) {
				setApplicationTheme(managedApplication, darkTheme);
				this.darkTheme = darkTheme;
			}

			@Override
			public void close() {
				if (mobileView) {
					if (userSessionData.getRootPanel().getContent().equals(application.getUi())) {
						userSessionData.setRootComponent(applicationLauncher);
					}
				} else {
					applicationsTabPanel.removeTab(tab);
				}
			}
		};
		return applicationDesktop;
	}

	public ResponsiveApplication createResponsiveApplication() {
		MobileApplicationNavigation mobileNavigation = new MobileApplicationNavigation();
		mobileNavigation.setApplicationLauncher(applicationLauncher);
		return ResponsiveApplication.createApplication(
				SessionContext.current().getClientInfo().isMobileDevice() ?
						new MobileAssembler(mobileNavigation, userSessionData.getLocalizationProvider()) :
						new DesktopApplicationAssembler());
	}

	private void createLauncherView(SimpleItemView<ApplicationData> launcherItemView, boolean mobileView) {
		launcherView = View.createView(StandardLayout.CENTER, ApplicationIcons.HOME, getLocalized(Dictionary.APPLICATIONS), launcherItemView);
		Panel panel = launcherView.getPanel();
		TextField applicationsSearchField = new TextField();
		applicationsSearchField.setShowClearButton(true);
		applicationsSearchField.setEmptyText(getLocalized(Dictionary.SEARCH___));
		applicationsSearchField.onTextInput().addListener(launcherItemView::setFilter);
		panel.setRightHeaderField(applicationsSearchField);
		panel.setContent(launcherItemView);
		panel.setBodyBackgroundColor(userSessionData.isDarkTheme() ? Color.fromRgba(30, 30, 30, .7f) : Color.WHITE.withAlpha(0.7f));
		applicationLauncher = panel;
	}

	private List<ApplicationData> getAllApplications() {
		return sortedApplicationGroups.stream()
				.flatMap(group -> group.getSortedApplications().stream())
				.collect(Collectors.toList());
	}

	public String getLocalized(String key, Object... objects) {
		return userSessionData.getLocalizationProvider().getLocalized(key, objects);
	}
}
