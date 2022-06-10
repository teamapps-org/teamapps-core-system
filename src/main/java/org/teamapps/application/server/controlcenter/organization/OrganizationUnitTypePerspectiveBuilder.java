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
package org.teamapps.application.server.controlcenter.organization;

import org.teamapps.application.api.application.perspective.AbstractPerspectiveBuilder;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.perspective.ApplicationPerspective;
import org.teamapps.application.api.privilege.ApplicationPrivilegeProvider;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.databinding.MutableValue;

public class OrganizationUnitTypePerspectiveBuilder extends AbstractPerspectiveBuilder {

	public OrganizationUnitTypePerspectiveBuilder() {
		super("organizationUnitTypePerspective", ApplicationIcons.ELEMENTS_CASCADE, "organizationUnitType.title", "organizationUnitType.desc");
	}

	@Override
	public boolean isPerspectiveAccessible(ApplicationPrivilegeProvider applicationPrivilegeProvider) {
		return applicationPrivilegeProvider.isReadAccess(Privileges.ORGANIZATION_UNIT_TYPE_PERSPECTIVE);
	}

	@Override
	public ApplicationPerspective build(ApplicationInstanceData applicationInstanceData, MutableValue<String> mutableValue) {
		return new OrganizationUnitTypePerspective(applicationInstanceData, mutableValue);
	}
}
