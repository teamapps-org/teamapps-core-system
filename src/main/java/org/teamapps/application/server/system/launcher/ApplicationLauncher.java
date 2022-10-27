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
package org.teamapps.application.server.system.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.application.BaseApplicationBuilder;
import org.teamapps.application.api.application.UserProfileApplicationBuilder;
import org.teamapps.application.api.application.perspective.PerspectiveBuilder;
import org.teamapps.application.api.desktop.ApplicationDesktop;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.localization.Language;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.DatabaseLogAppender;
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
import org.teamapps.universaldb.UniversalDB;
import org.teamapps.ux.application.ResponsiveApplication;
import org.teamapps.ux.application.assembler.DesktopApplicationAssembler;
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
import org.teamapps.ux.session.ClientInfo;
import org.teamapps.ux.session.SessionConfiguration;
import org.teamapps.ux.session.SessionContext;
import org.teamapps.ux.session.StylingTheme;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationLauncher {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final UserSessionData userSessionData;
	private final LogoutHandler logoutHandler;
	private final SystemRegistry registry;
	private final boolean mobileView;
	private List<ApplicationGroupData> sortedApplicationGroups;
	private Component applicationLauncher;
	private Set<ApplicationData> openedApplications = new HashSet<>();
	private Map<ApplicationData, Tab> tabByApplicationData = new HashMap<>();
	private Map<ApplicationData, Component> mobilAppByApplicationData = new HashMap<>();
	private TabPanel applicationsTabPanel;
	private TwoWayBindableValue<ManagedApplication> selectedApplication = TwoWayBindableValue.create();
	private TwoWayBindableValue<ManagedApplicationPerspective> selectedPerspective = TwoWayBindableValue.create();
	private boolean selectedThemeIsDarkTheme;
	private int activityCount;
	private int applicationOpenCount;
	private final Login loginData;
	private Tab applicationLauncherTab;
	private ApplicationData userProfileApp;

	public ApplicationLauncher(UserSessionData userSessionData, LogoutHandler logoutHandler) {
		this.userSessionData = userSessionData;
		this.registry = userSessionData.getRegistry();
		ClientInfo clientInfo = userSessionData.getContext().getClientInfo();
		this.mobileView = clientInfo.isMobileDevice();
		this.logoutHandler = logoutHandler;
		loginData = createLoginData(userSessionData, clientInfo);

		userSessionData.getContext().onDestroyed.addListener(() -> loginData
				.setActivityCount(activityCount)
				.setApplicationOpenCount(applicationOpenCount)
				.setDateLogout(Instant.now())
				.save());

		userSessionData.getContext().addExecutionDecorator(runnable -> {
			try {
				UniversalDB.setUserId(userSessionData.getUser().getId());
				DatabaseLogAppender.THREAD_LOCAL_MANAGED_APPLICATION.set(selectedApplication.get() != null ? selectedApplication.get().getId() : 0);
				DatabaseLogAppender.THREAD_LOCAL_APPLICATION_VERSION.set(getCurrentApplicationVersion(selectedApplication.get()));
				DatabaseLogAppender.THREAD_LOCAL_MANAGED_PERSPECTIVE.set(selectedPerspective.get() != null ? selectedPerspective.get().getId() : 0);
				activityCount++;
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
		userSessionData.getUser().setLastLogin(Instant.now()).save();
		setTheme(userSessionData.isDarkTheme());
		userSessionData.setApplicationDesktopSupplier(this::createApplicationDesktop);
		initApplicationData();
		createApplicationLauncher();
		createMainView();
	}

	private String getCurrentApplicationVersion(ManagedApplication managedApplication) {
		if (managedApplication != null && managedApplication.getMainApplication() != null && managedApplication.getMainApplication().getInstalledVersion() !=  null) {
			return managedApplication.getMainApplication().getInstalledVersion().getVersion();
		} else {
			return null;
		}
	}

	private Login createLoginData(UserSessionData userSessionData, ClientInfo clientInfo) {
		return Login.create()
				.setUser(userSessionData.getUser())
				.setDateLogin(Instant.now())
				.setIp(clientInfo.getIp())
				.setUserAgent(clientInfo.getUserAgent())
				.setMobileDevice(clientInfo.isMobileDevice())
				.setScreenSize(clientInfo.getScreenWidth() + "x" + clientInfo.getScreenHeight())
				.save();
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
		dialogue.show();
	}

	private void handleApplicationSelection(ManagedApplication application) {
		if (application == null) {
			return;
		}
		DatabaseLogAppender.THREAD_LOCAL_MANAGED_APPLICATION.set(application.getId());
		DatabaseLogAppender.THREAD_LOCAL_APPLICATION_VERSION.set(getCurrentApplicationVersion(application));
		DatabaseLogAppender.THREAD_LOCAL_MANAGED_PERSPECTIVE.set(null);
		setTheme(application.isDarkTheme() || userSessionData.isDarkTheme());
		selectedThemeIsDarkTheme = application.getDarkTheme();
	}

	private void handlePerspectiveSelection(ManagedApplicationPerspective perspective) {
		ManagedApplication application = perspective.getManagedApplication();
		DatabaseLogAppender.THREAD_LOCAL_MANAGED_APPLICATION.set(application != null ? application.getId() : 0);
		DatabaseLogAppender.THREAD_LOCAL_APPLICATION_VERSION.set(getCurrentApplicationVersion(application));
		DatabaseLogAppender.THREAD_LOCAL_MANAGED_PERSPECTIVE.set(perspective.getId());
	}

	private void setTheme(boolean dark) {
		userSessionData.getContext().setBackgroundImage(dark ? "defaultDarkBackground" : "defaultBackground", 0);
		SessionConfiguration configuration = SessionContext.current().getConfiguration();
		configuration.setTheme(dark ? StylingTheme.DARK : StylingTheme.DEFAULT);
		SessionContext.current().setConfiguration(configuration);
	}

	private void handleApplicationLauncherSelection() {
		selectedApplication.set(null);
		if (selectedThemeIsDarkTheme) {
			setTheme(true);
			selectedThemeIsDarkTheme = false;
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
		applicationLauncher = createLauncherView(itemView, mobileView);
	}

	public void updateApplicationLauncher() {
		initApplicationData();
		createApplicationLauncher();
		if (applicationLauncherTab != null) {
			applicationLauncherTab.setContent(applicationLauncher);
		}
	}

	private void logout() {
		User user = userSessionData.getUser();
		LOGGER.info("User logout: {}, {} {}", user.getId(), user.getFirstName(), user.getLastName());
		registry.getBootstrapSessionHandler().onUserLogout.fire(userSessionData.getContext());
		userSessionData.invalidate();
		SessionContext.current().clearExecutionDecorators();
		LoginHandler loginHandler = new LoginHandler(registry, logoutHandler, userSessionData);
		loginHandler.createLoginView(userSessionData.getContext(), userSessionData.getRootPanel());
	}

	private void createMainView() {
		ThemingConfig themingConfig = registry.getSystemConfig().getThemingConfig();
		userSessionData.getContext().registerBackgroundImage("defaultBackground", themingConfig.getApplicationBackgroundUrl(), themingConfig.getApplicationSecondaryBackgroundUrl());
		userSessionData.getContext().registerBackgroundImage("defaultDarkBackground", themingConfig.getApplicationDarkBackgroundUrl(), themingConfig.getApplicationDarkSecondaryBackgroundUrl());
		if (themingConfig.isDarkTheme()) {
			setTheme(true);
		}
		if (themingConfig.getBaseStyles() != null) {
			userSessionData.getRootPanel().setBaseStyles(themingConfig.getBaseStyles());
		}
		userSessionData.getContext().setBackgroundImage(userSessionData.isDarkTheme() ? "defaultDarkBackground" : "defaultBackground", 0);

		if (mobileView) {
			userSessionData.setRootComponent(applicationLauncher);
		} else {
			applicationsTabPanel = new TabPanel();
			applicationsTabPanel.onTabSelected.addListener(tab -> {
				tabByApplicationData.entrySet()
						.stream()
						.filter(entry -> entry.getValue().equals(tab))
						.map(Map.Entry::getKey).findAny()
						.ifPresent(applicationData -> selectedApplication.set(applicationData.getManagedApplication()));
				if (tab.equals(applicationLauncherTab)) {
					handleApplicationLauncherSelection();
				}
			});
			applicationLauncherTab = new Tab(ApplicationIcons.HOME, getLocalized(Dictionary.APPLICATIONS), applicationLauncher);
			applicationsTabPanel.addTab(applicationLauncherTab, true);

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
						//todo open with perspective
						openApplication(userProfileApp);
					});
				}
			}
			SimpleItemGroup<?> itemGroup = itemView.addSingleColumnGroup(ApplicationIcons.LOG_OUT, getLocalized(Dictionary.LOGOUT));
			itemGroup.addItem(ApplicationIcons.LOG_OUT, getLocalized(Dictionary.LOGOUT), "Vom System abmelden").onClick.addListener(this::logout); //todo dictionary entry
			profileButton.setDropDownComponent(itemView);
			profileButton.setMinDropDownWidth(200);
			profileButton.setIconSize(16);
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
				}
			}
		}
	}

	private void openApplication(ApplicationData applicationData) {
		selectedApplication.set(applicationData.getManagedApplication());
		applicationOpenCount++;
		LOGGER.info("Open app: " + (selectedPerspective.get() != null ? selectedPerspective.get().getApplicationPerspective().getQualifiedName() : null));
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
				tab.onClosed.addListener(() -> {
					tabByApplicationData.remove(applicationData);
					openedApplications.remove(applicationData);
					applicationData.reloadApplicationData(userSessionData);
				});
				applicationsTabPanel.addTab(tab, true);
			}
		}
	}

	public ApplicationDesktop createApplicationDesktop() {
		return new ApplicationDesktop() {
			private ResponsiveApplication application = createResponsiveApplication();
			private Tab tab;

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
				}
			}

			@Override
			public void close() {
				if (mobileView) {
					userSessionData.setRootComponent(applicationLauncher);
				} else {
					applicationsTabPanel.removeTab(tab);
				}
			}
		};
	}

	public ResponsiveApplication createResponsiveApplication() {
		MobileApplicationNavigation mobileNavigation = new MobileApplicationNavigation();
		mobileNavigation.setApplicationLauncher(applicationLauncher);
		return ResponsiveApplication.createApplication(
				SessionContext.current().getClientInfo().isMobileDevice() ?
						new MobileAssembler(mobileNavigation, userSessionData.getLocalizationProvider()) :
						new DesktopApplicationAssembler());
	}

	private Component createLauncherView(SimpleItemView<ApplicationData> applicationLauncher, boolean mobileView) {
		Panel panel = new Panel(ApplicationIcons.HOME, getLocalized(Dictionary.APPLICATIONS));
		TextField applicationsSearchField = new TextField();
		applicationsSearchField.setShowClearButton(true);
		applicationsSearchField.setEmptyText(getLocalized(Dictionary.SEARCH___));
		applicationsSearchField.onTextInput().addListener(applicationLauncher::setFilter);
		panel.setRightHeaderField(applicationsSearchField);
		panel.setContent(applicationLauncher);
		panel.setBodyBackgroundColor(userSessionData.isDarkTheme() ? Color.fromRgba(30, 30, 30, .7f) : Color.WHITE.withAlpha(0.7f));
		if (mobileView) {
			return panel;
		}
		Panel framePanel = new Panel();
		framePanel.setHideTitleBar(true);
		framePanel.setPadding(5);
		framePanel.setContent(panel);
		framePanel.setBodyBackgroundColor(userSessionData.isDarkTheme() ? Color.BLACK.withAlpha(0.001f) : Color.WHITE.withAlpha(0.001f));
		return framePanel;
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
