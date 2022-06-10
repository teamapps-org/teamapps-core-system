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
package org.teamapps.application.server.system.session;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.perspective.PerspectiveBuilder;
import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.privilege.ApplicationPrivilegeProvider;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.launcher.MobileApplicationNavigation;
import org.teamapps.application.server.system.launcher.MobileAssembler;
import org.teamapps.application.server.system.launcher.PerspectiveByNameLauncher;
import org.teamapps.application.server.system.privilege.AllowAllPrivilegeProvider;
import org.teamapps.application.server.system.privilege.PrivilegeApplicationKey;
import org.teamapps.model.controlcenter.*;
import org.teamapps.ux.application.ResponsiveApplication;
import org.teamapps.ux.application.assembler.DesktopApplicationAssembler;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;
import org.teamapps.ux.session.SessionContext;

public class ManagedApplicationSessionData {

	private final UserSessionData userSessionData;
	private final ManagedApplication managedApplication;
	private final OrganizationFieldView organizationFieldView;
	private final MobileApplicationNavigation mobileNavigation;

	private final ResponsiveApplication responsiveApplication;
	private final SystemRegistry registry;
	private final ApplicationLocalizationProvider mainApplicationLocalizationProvider;
	private ToolbarButton applicationMenuToolbarButton;
	private ToolbarButton perspectiveMenuToolbarButton;

	public ManagedApplicationSessionData(UserSessionData userSessionData, ManagedApplication managedApplication, MobileApplicationNavigation mobileNavigation) {
		this.userSessionData = userSessionData;
		this.managedApplication = managedApplication;
		this.organizationFieldView = managedApplication.getOrganizationField() != null ? OrganizationFieldView.getById(managedApplication.getOrganizationField().getId()) : null;
		this.mobileNavigation = mobileNavigation;

		boolean mobileDevice = SessionContext.current().getClientInfo().isMobileDevice();
		ApplicationLocalizationProvider dictionary = userSessionData.getLocalizationProvider();
		this.responsiveApplication = ResponsiveApplication.createApplication(
				mobileDevice ?
						new MobileAssembler(mobileNavigation, dictionary) :
						new DesktopApplicationAssembler());
		if (!mobileDevice) {
			ToolbarButtonGroup buttonGroup = responsiveApplication.addApplicationButtonGroup(new ToolbarButtonGroup());
			applicationMenuToolbarButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.WINDOWS, dictionary.getLocalized(Dictionary.PERSPECTIVE), dictionary.getLocalized(Dictionary.SELECT_APPLICATION_PERSPECTIVE)));
			applicationMenuToolbarButton.setDroDownPanelWidth(350);
			applicationMenuToolbarButton.setVisible(false);
			perspectiveMenuToolbarButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.WINDOW_EXPLORER, dictionary.getLocalized(Dictionary.VIEWS), dictionary.getLocalized(Dictionary.VIEWS)));
			perspectiveMenuToolbarButton.setDroDownPanelWidth(350);
			perspectiveMenuToolbarButton.setVisible(false);
		}
		registry = userSessionData.getRegistry();
		this.mainApplicationLocalizationProvider = userSessionData.getApplicationLocalizationProvider(managedApplication.getMainApplication());
	}

	public PerspectiveSessionData createPerspectiveSessionData(ManagedApplicationPerspective managedApplicationPerspective, PerspectiveByNameLauncher perspectiveByNameLauncher) {
		LoadedApplication loadedApplication = registry.getLoadedApplication(managedApplicationPerspective.getApplicationPerspective().getApplication());
		if (loadedApplication == null) {
			return null;
		}
		ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(managedApplicationPerspective.getApplicationPerspective().getApplication());
		ApplicationPrivilegeProvider privilegeProvider = userSessionData.getUserPrivileges().getApplicationPrivilegeProvider(PrivilegeApplicationKey.create(managedApplicationPerspective));
		PerspectiveBuilder perspectiveBuilder = loadedApplication.getPerspectiveBuilder(managedApplicationPerspective.getApplicationPerspective().getName());
		if (userSessionData.getUser().getUserAccountStatus() == UserAccountStatus.SUPER_ADMIN) {
			privilegeProvider = new AllowAllPrivilegeProvider();
		}
		return new PerspectiveSessionData(this, managedApplication, managedApplicationPerspective, perspectiveBuilder, perspectiveByNameLauncher, privilegeProvider, localizationProvider, registry.getDocumentConverterSupplier());
	}

	public ApplicationInstanceData getUnmanagedApplicationData() {
		ApplicationPrivilegeProvider privilegeProvider = userSessionData.getUserPrivileges().getApplicationPrivilegeProvider(PrivilegeApplicationKey.createUnmanagedKey(managedApplication));
		if (userSessionData.getUser().getUserAccountStatus() == UserAccountStatus.SUPER_ADMIN) {
			privilegeProvider = new AllowAllPrivilegeProvider();
		}
		return new UnmanagedApplicationSessionData(userSessionData, managedApplication, responsiveApplication, privilegeProvider, mainApplicationLocalizationProvider);
	}


	public boolean isUnmanagedApplication() {
		return registry.getLoadedApplication(managedApplication.getMainApplication()).isUnmanagedPerspectives();
	}

	public ApplicationLocalizationProvider getMainApplicationLocalizationProvider() {
		return mainApplicationLocalizationProvider;
	}

	public UserSessionData getUserSessionData() {
		return userSessionData;
	}

	public ManagedApplication getManagedApplication() {
		return managedApplication;
	}

	public MobileApplicationNavigation getMobileNavigation() {
		return mobileNavigation;
	}

	public OrganizationFieldView getOrganizationFieldView() {
		return organizationFieldView;
	}

	public LoadedApplication getMainApplication() {
		return registry.getLoadedApplication(managedApplication.getMainApplication());
	}

	public ResponsiveApplication getResponsiveApplication() {
		return responsiveApplication;
	}

	public void setApplicationToolbarMenuComponent(Component component) {
		if (component == null) {
			applicationMenuToolbarButton.setVisible(false);
		} else {
			applicationMenuToolbarButton.updateDropDownComponent(component);
			applicationMenuToolbarButton.setVisible(true);
		}
	}

	public void setPerspectiveToolbarMenuComponent(Component component) {
		if (component == null) {
			perspectiveMenuToolbarButton.setVisible(false);
		} else {
			perspectiveMenuToolbarButton.updateDropDownComponent(component);
			perspectiveMenuToolbarButton.setVisible(true);
		}
	}
}
