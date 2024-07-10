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
import org.teamapps.application.api.application.perspective.PerspectiveBuilder;
import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.session.ManagedApplicationSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.ManagedApplication;
import org.teamapps.model.controlcenter.ManagedApplicationPerspective;
import org.teamapps.model.controlcenter.OrganizationField;

import java.lang.invoke.MethodHandles;

public class ApplicationData {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final ManagedApplication managedApplication;
	private final Icon icon;
	private final String title;
	private final String description;
	private final int applicationPosition;
	private LoadedApplication loadedApplication;
	private ManagedApplicationSessionData applicationSessionData;
	private String applicationBadgeCount = null;

	public ApplicationData(ManagedApplication managedApplication, LoadedApplication loadedApplication, ManagedApplicationSessionData applicationSessionData) {
		this.managedApplication = managedApplication;
		this.loadedApplication = loadedApplication;
		this.applicationSessionData = applicationSessionData;
		this.icon = managedApplication.getIcon() != null ? IconUtils.decodeIcon(managedApplication.getIcon()) : loadedApplication.getBaseApplicationBuilder().getApplicationIcon();
		ApplicationLocalizationProvider localizationProvider = applicationSessionData.getMainApplicationLocalizationProvider();
		this.title = managedApplication.getTitleKey() != null ? localizationProvider.getLocalized(managedApplication.getTitleKey()) : localizationProvider.getLocalized(loadedApplication.getBaseApplicationBuilder().getApplicationTitleKey());
		this.description = managedApplication.getDescriptionKey() != null ? localizationProvider.getLocalized(managedApplication.getDescriptionKey()) : localizationProvider.getLocalized(loadedApplication.getBaseApplicationBuilder().getApplicationDescriptionKey());
		this.applicationPosition = managedApplication.getListingPosition();
		init();
	}

	private void init() {
		int userId = applicationSessionData.getUserSessionData().getUser().getId();
		int count = 0;
		for (ManagedApplicationPerspective managedApplicationPerspective : managedApplication.getPerspectives()) {
			OrganizationField organizationField = managedApplicationPerspective.getManagedApplication().getOrganizationField();
			if (managedApplicationPerspective.getApplicationPerspective() == null) {
				LOGGER.warn("ERROR missing application perspective for app:{}, managed-perspective:{}", loadedApplication.getApplication().getName(), managedApplicationPerspective.getId());
				continue;
			}
			LoadedApplication application = applicationSessionData.getUserSessionData().getRegistry().getLoadedApplication(managedApplicationPerspective.getApplicationPerspective().getApplication());
			if (application != null) {
				PerspectiveBuilder perspectiveBuilder = application.getPerspectiveBuilder(managedApplicationPerspective.getApplicationPerspective().getName());
				if (perspectiveBuilder != null) {
					count += perspectiveBuilder.getApplicationBadgeCount(userId, OrganizationUtils.convert(organizationField));
				}
			}
		}
		if (count > 0) {
			applicationBadgeCount = "" + count;
		}
	}

	public void reloadApplicationData(UserSessionData userSessionData) {
		Application application = managedApplication.getMainApplication();
		loadedApplication = userSessionData.getRegistry().getLoadedApplication(application);
		applicationSessionData = userSessionData.createManageApplicationSessionData(managedApplication, new MobileApplicationNavigation());
	}

	public ManagedApplication getManagedApplication() {
		return managedApplication;
	}

	public LoadedApplication getLoadedApplication() {
		return loadedApplication;
	}

	public ManagedApplicationSessionData getApplicationSessionData() {
		return applicationSessionData;
	}

	public Icon getIcon() {
		return icon;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public Integer getApplicationPosition() {
		return applicationPosition;
	}

	public String getApplicationBadgeCount() {
		return applicationBadgeCount;
	}
}
