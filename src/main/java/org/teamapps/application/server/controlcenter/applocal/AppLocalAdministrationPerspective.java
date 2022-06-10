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
package org.teamapps.application.server.controlcenter.applocal;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.perspective.PerspectiveMenuPanel;
import org.teamapps.application.server.controlcenter.accesscontrol.AccessControlAppRolePerspectiveBuilder;
import org.teamapps.application.server.controlcenter.accesscontrol.AccessControlPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.applications.ApplicationUpdatesPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.database.DataBasePerspectiveBuilder;
import org.teamapps.application.server.controlcenter.roles.RolesPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.roles.UserRoleAssignmentPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.systemlog.SystemLogPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.systenconfig.ApplicationConfigurationPerspectiveBuilder;
import org.teamapps.application.server.controlcenter.translations.TranslationsPerspectiveBuilder;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.databinding.MutableValue;

public class AppLocalAdministrationPerspective extends AbstractManagedApplicationPerspective {

	public AppLocalAdministrationPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		createUi();
	}

	private void createUi() {
		TranslationsPerspectiveBuilder translationsPerspectiveBuilder = new TranslationsPerspectiveBuilder();
		PerspectiveMenuPanel menuPanel = PerspectiveMenuPanel.createMenuPanel(getApplicationInstanceData(),
				translationsPerspectiveBuilder,
				new UserRoleAssignmentPerspectiveBuilder(),
				new RolesPerspectiveBuilder(),
				new AccessControlPerspectiveBuilder(),
				new AccessControlAppRolePerspectiveBuilder(),
				new ApplicationUpdatesPerspectiveBuilder(),
				new SystemLogPerspectiveBuilder(),
				new ApplicationConfigurationPerspectiveBuilder(),
				new DataBasePerspectiveBuilder()
		);
		setPerspectiveMenuPanel(menuPanel.getComponent(), menuPanel.getButtonMenu());
		onPerspectiveInitialized.addListener(() -> menuPanel.openPerspective(translationsPerspectiveBuilder));
	}


}
