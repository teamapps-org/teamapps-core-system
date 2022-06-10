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
package org.teamapps.application.server.system.application;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.perspective.AbstractApplicationPerspective;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.ManagedApplication;
import org.teamapps.model.controlcenter.OrganizationField;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.databinding.MutableValue;

public abstract class AbstractManagedApplicationPerspective extends AbstractApplicationPerspective {

	private ManagedApplication managedApplication;
	private Application mainApplication;
	private OrganizationField organizationField;

	public AbstractManagedApplicationPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		init();
	}

	private void init() {
		managedApplication = ManagedApplication.getById(getApplicationInstanceData().getManagedApplicationId());
		mainApplication = managedApplication.getMainApplication();
		organizationField = OrganizationUtils.convert(getApplicationInstanceData().getOrganizationField());
	}



	public boolean isAppFilter() {
		return isOrgFieldFilterApplied() && getMainApplication() != null;
	}

	public boolean isOrgFieldFilterApplied() {
		return organizationField != null;
	}

	public ManagedApplication getManagedApplication() {
		return managedApplication;
	}

	public Application getMainApplication() {
		return mainApplication;
	}

	public OrganizationField getOrganizationField() {
		return organizationField;
	}
}
