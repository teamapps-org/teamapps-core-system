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
package org.teamapps.application.server.system.launcher;

import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.ManagedApplicationGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationGroupData {

	private final Icon icon;
	private final String title;
	private final int groupPosition;
	private final List<ApplicationData> applications = new ArrayList<>();

	public ApplicationGroupData(ManagedApplicationGroup applicationGroup, UserSessionData userSessionData) {
		this.icon = applicationGroup.getIcon() != null ? IconUtils.decodeIcon(applicationGroup.getIcon()) : null;
		this.title = userSessionData.getLocalizationProvider().getLocalized(applicationGroup.getTitleKey());
		this.groupPosition = applicationGroup.getListingPosition();
	}

	public ApplicationGroupData(Icon icon, String title, int groupPosition) {
		this.icon = icon;
		this.title = title;
		this.groupPosition = groupPosition;
	}

	public void addApplicationData(ApplicationData applicationData) {
		applications.add(applicationData);
	}

	public Icon getIcon() {
		return icon;
	}

	public String getTitle() {
		return title;
	}

	public Integer getGroupPosition() {
		return groupPosition;
	}

	public List<ApplicationData> getSortedApplications() {
		return applications.stream()
				.sorted(Comparator.comparing(ApplicationData::getApplicationPosition))
				.collect(Collectors.toList());
	}

	public static List<ApplicationGroupData> getSortedGroups(Collection<ApplicationGroupData> groups) {
		return groups.stream()
				.sorted(Comparator.comparing(ApplicationGroupData::getGroupPosition))
				.collect(Collectors.toList());
	}
}
