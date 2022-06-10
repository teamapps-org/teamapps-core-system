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
package org.teamapps.application.server.system.privilege;

import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.model.controlcenter.*;

import java.util.Objects;

public class PrivilegeApplicationKey {

	public static PrivilegeApplicationKey create(ManagedApplicationPerspective perspective) {
		OrganizationField organizationField = perspective.getManagedApplication().getOrganizationField();
		OrganizationFieldView organizationFieldView = OrganizationUtils.convert(organizationField);
		Application application = perspective.getApplicationPerspective().getApplication();
		if (application == null) {
			return null;
		}
		return new PrivilegeApplicationKey(application, organizationFieldView);
	}

	public static PrivilegeApplicationKey createUnmanagedKey(ManagedApplication managedApplication) {
		return new PrivilegeApplicationKey(managedApplication.getMainApplication(), null);
	}

	public static PrivilegeApplicationKey create(ManagedApplication managedApplication) {
		return new PrivilegeApplicationKey(managedApplication.getMainApplication(), null);
	}


	public static PrivilegeApplicationKey create(RoleApplicationRoleAssignment roleApplicationRoleAssignment) {
		return new PrivilegeApplicationKey(roleApplicationRoleAssignment.getApplication(), OrganizationUtils.convert(roleApplicationRoleAssignment.getOrganizationFieldFilter()));
	}

	public static PrivilegeApplicationKey create(RolePrivilegeAssignment privilegeAssignment) {
		return new PrivilegeApplicationKey(privilegeAssignment.getApplication(), OrganizationUtils.convert(privilegeAssignment.getOrganizationFieldFilter()));
	}

	public static PrivilegeApplicationKey create(Application application, OrganizationField organizationField) {
		return new PrivilegeApplicationKey(application, OrganizationUtils.convert(organizationField));
	}

	private final String key;
	private final Application application;
	private final OrganizationFieldView organizationFieldView;

	private PrivilegeApplicationKey(Application application, OrganizationFieldView organizationFieldView) {
		this.application = application;
		this.organizationFieldView = organizationFieldView;
		this.key = organizationFieldView != null ? application.getId() + "-" + organizationFieldView.getId() : application.getId() + "";
	}

	public Application getApplication() {
		return application;
	}

	public OrganizationFieldView getOrganizationFieldView() {
		return organizationFieldView;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PrivilegeApplicationKey that = (PrivilegeApplicationKey) o;
		return key.equals(that.key);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key);
	}
}
