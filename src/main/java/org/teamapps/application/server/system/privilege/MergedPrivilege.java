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
package org.teamapps.application.server.system.privilege;

import org.teamapps.icons.Icon;
import org.teamapps.ux.component.template.BaseTemplateTreeNode;

public class MergedPrivilege extends BaseTemplateTreeNode<Object> {

	private final MergedPrivilegeGroup parent;
	private final String name;
	private final Icon icon;
	private final String title;

	public MergedPrivilege(MergedPrivilegeGroup parent, String name, Icon icon, String title) {
		this.parent = parent;
		this.name = name;
		this.icon = icon;
		this.title = title;
	}

	public String getName() {
		return name;
	}

	public Icon getIcon() {
		return icon;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public BaseTemplateTreeNode<Object> getParent() {
		return parent;
	}

	@Override
	public String getCaption() {
		return title;
	}
}
