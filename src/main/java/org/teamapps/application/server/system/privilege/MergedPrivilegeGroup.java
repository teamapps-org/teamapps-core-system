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
package org.teamapps.application.server.system.privilege;

import org.teamapps.application.api.privilege.PrivilegeGroupType;
import org.teamapps.icons.Icon;
import org.teamapps.ux.component.template.BaseTemplateTreeNode;

import java.util.HashMap;
import java.util.Map;

public class MergedPrivilegeGroup extends BaseTemplateTreeNode<Object> {

	private final MergedApplicationPrivileges parent;
	private final String name;
	private final PrivilegeGroupType groupType;
	private final Icon icon;
	private final String title;
	private final String description;
	private Map<String, MergedPrivilege> privilegesByName = new HashMap<>();

	public MergedPrivilegeGroup(MergedApplicationPrivileges parent, String name, PrivilegeGroupType groupType, Icon icon, String title, String description) {
		this.parent = parent;
		this.name = name;
		this.groupType = groupType;
		this.icon = icon;
		this.title = title;
		this.description = description;
	}

	public void addPrivilege(String name, Icon icon, String title) {
		if (!privilegesByName.containsKey(name)) {
			privilegesByName.put(name, new MergedPrivilege(this, name, icon, title));
		}
	}

	public String getName() {
		return name;
	}

	public PrivilegeGroupType getGroupType() {
		return groupType;
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

	public Map<String, MergedPrivilege> getPrivilegesByName() {
		return privilegesByName;
	}

	@Override
	public BaseTemplateTreeNode<Object> getParent() {
		return parent;
	}

	@Override
	public String getCaption() {
		return title;
	}

	@Override
	public String getBadge() {
		return privilegesByName.size() > 0 ? privilegesByName.size() + "" : null;
	}
}
