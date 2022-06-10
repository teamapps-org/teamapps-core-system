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
package org.teamapps.application.server.controlcenter.monitoring;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.config.MonitoringDashboardConfig;
import org.teamapps.application.server.system.config.MonitoringLink;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.databinding.MutableValue;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.iframe.IFrame;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.tree.Tree;
import org.teamapps.ux.component.tree.TreeNodeInfo;
import org.teamapps.ux.model.ListTreeModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitoringPerspective extends AbstractManagedApplicationPerspective {

	private final PerspectiveSessionData perspectiveSessionData;
	private final UserSessionData userSessionData;
	private final MonitoringDashboardConfig config;

	public MonitoringPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		config = userSessionData.getRegistry().getSystemConfig().getServerMonitoringDashboardConfig();
		createUi();
	}

	private void createUi() {
		Tree<MonitoringLink> perspectiveMenu = createPerspectiveMenu();
		setPerspectiveMenuPanel(perspectiveMenu, null);
		View centerView = getPerspective().addView(View.createView(StandardLayout.CENTER, ApplicationIcons.CHART_LINE, getLocalized("monitoring.title"), null));
		IFrame iFrame = new IFrame();
		centerView.setComponent(iFrame);

		perspectiveMenu.onNodeSelected.addListener(node -> {
			centerView.setTitle(node.getTitle());
			iFrame.setUrl(node.getLink());
		});

		if (!config.getMonitoringLinks().isEmpty()) {
			iFrame.setUrl(config.getMonitoringLinks().get(0).getLink());
		}
	}

	private Tree<MonitoringLink> createPerspectiveMenu() {
		List<MonitoringLink> links = new ArrayList<>();
		for (MonitoringLink monitoringLink : config.getMonitoringLinks()) {
			getAllLinks(monitoringLink, links);
		}
		ListTreeModel<MonitoringLink> treeModel = new ListTreeModel<>(links);
		treeModel.setTreeNodeInfoFunction(monitoringLink -> new TreeNodeInfo() {
			@Override
			public Object getParent() {
				return MonitoringPerspective.this.getParent(monitoringLink);
			}

			@Override
			public boolean isExpanded() {
				return false;
			}

			@Override
			public boolean isLazyChildren() {
				return false;
			}
		});
		Tree<MonitoringLink> tree = new Tree<>(treeModel);
		tree.setPropertyProvider((monitoringLink, s) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, ApplicationIcons.CHART_LINE);
			map.put(BaseTemplate.PROPERTY_CAPTION, monitoringLink.getTitle());
			return map;
		});
		tree.setEntryTemplate(BaseTemplate.LIST_ITEM_LARGE_ICON_SINGLE_LINE);
		tree.setOpenOnSelection(true);
		tree.setEnforceSingleExpandedPath(true);

		return tree;
	}


	private void getAllLinks(MonitoringLink link, List<MonitoringLink> links) {
		links.add(link);
		for (MonitoringLink childLink : link.getChildLinks()) {
			getAllLinks(childLink, links);
		}
	}

	private MonitoringLink getParent(MonitoringLink link) {
		for (MonitoringLink monitoringLink : config.getMonitoringLinks()) {
			for (MonitoringLink childLink : monitoringLink.getChildLinks()) {
				MonitoringLink result = getParentFromChild(childLink, link, monitoringLink);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	private MonitoringLink getParentFromChild(MonitoringLink child, MonitoringLink search, MonitoringLink parent) {
		if (search.equals(child)) {
			return parent;
		}
		for (MonitoringLink childLink : child.getChildLinks()) {
			MonitoringLink link = getParentFromChild(childLink, search, child);
			if (link != null) {
				return link;
			}
		}
		return null;
	}
}
