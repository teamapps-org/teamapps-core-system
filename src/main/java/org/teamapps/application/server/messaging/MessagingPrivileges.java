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
package org.teamapps.application.server.messaging;

import org.teamapps.application.api.privilege.*;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.icons.composite.CompositeIcon;

import java.util.List;

public class MessagingPrivileges {

	private final static ApplicationPrivilegeBuilder PRIVILEGE_BUILDER = new ApplicationPrivilegeBuilder();
	private final static ApplicationRoleBuilder ROLE_BUILDER = new ApplicationRoleBuilder();

	public final static SimplePrivilege LAUNCH_APPLICATION = PRIVILEGE_BUILDER.LAUNCH_APPLICATION;

	public final static SimplePrivilege LAUNCH_PERSPECTIVE_NEWS_BOARD = PRIVILEGE_BUILDER.addSimplePrivilege("launchPerspectiveNewsBoard", CompositeIcon.of(ApplicationIcons.KEY, ApplicationIcons.WORKER), "newsBoard.launchNewsBoardPerspective", "newsBoard.launchNewsBoardPerspective");
	public final static SimplePrivilege NEWS_BOARD_ADMIN_ACCESS = PRIVILEGE_BUILDER.addSimplePrivilege("newsBoardAdminAccess", ApplicationIcons.PILOT, "newsBoard.newsBoardAdmin", "newsBoard.newsBoardAdmin");


	public static List<ApplicationRole> getRoles() {
		return ROLE_BUILDER.getRoles();
	}

	public static List<PrivilegeGroup> getPrivileges() {
		return PRIVILEGE_BUILDER.getPrivileges();
	}
}
