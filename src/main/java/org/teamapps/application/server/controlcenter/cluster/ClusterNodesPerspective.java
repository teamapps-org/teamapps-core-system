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
package org.teamapps.application.server.controlcenter.cluster;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.system.utils.RoleUtils;
import org.teamapps.application.tools.EntityListModelBuilder;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.combo.ComboBoxUtils;
import org.teamapps.application.ux.form.FormController;
import org.teamapps.application.ux.form.FormPanel;
import org.teamapps.application.ux.localize.TranslatableField;
import org.teamapps.application.ux.localize.TranslatableTextUtils;
import org.teamapps.application.ux.view.MasterDetailController;
import org.teamapps.cluster.core.Cluster;
import org.teamapps.cluster.core.Node;
import org.teamapps.cluster.core.RemoteNode;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.databinding.MutableValue;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.universaldb.pojo.Query;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.field.CheckBox;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.TextField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.field.combobox.TagBoxWrappingMode;
import org.teamapps.ux.component.field.combobox.TagComboBox;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.table.ListTable;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.table.TableColumn;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ClusterNodesPerspective extends AbstractManagedApplicationPerspective {

	private final UserSessionData userSessionData;
	private final TwoWayBindableValue<RemoteNode> selectedNode = TwoWayBindableValue.create();

	public ClusterNodesPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		PerspectiveSessionData perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		SystemRegistry registry = userSessionData.getRegistry();
		Cluster cluster = registry.getCluster();
		View centerView = getPerspective().addView(View.createView(StandardLayout.CENTER, ApplicationIcons.RACK_SERVER_NETWORK, getLocalized("cluster.clusterNodes"), null));
		View rightView = getPerspective().addView(View.createView(StandardLayout.RIGHT, ApplicationIcons.RACK_SERVER_NETWORK, getLocalized("cluster.clusterNodes"), null));
		if (cluster == null) {
			return;
		}

		ListTable<RemoteNode> table = new ListTable<>(cluster.getRemoteNodes());
		table.setDisplayAsList(true);
		table.setRowHeight(28);
		table.setStripedRows(false);

		table.addColumn("name", "Node ID", new TextField()).setDefaultWidth(100).setValueExtractor(node -> node.getNodeId());
		table.addColumn("host", "Host", new TextField()).setDefaultWidth(70).setValueExtractor(node -> node.getHostAddress().getHost());
		table.addColumn("port", "Port", new TextField()).setDefaultWidth(70).setValueExtractor(node -> node.getHostAddress().getPort() + "");
		table.addColumn("sentBytes", "Bytes sent", new TextField()).setDefaultWidth(70).setValueExtractor(node -> readableFileSize(node.getSentBytes()));
		table.addColumn("receivedBytes", "Bytes received", new TextField()).setDefaultWidth(70).setValueExtractor(node -> readableFileSize(node.getReceivedBytes()));
		table.addColumn("sentMessages", "Messages sent", new TextField()).setDefaultWidth(70).setValueExtractor(node -> node.getSentMessages() + "");
		table.addColumn("receivedMessages", "Messages received", new TextField()).setDefaultWidth(70).setValueExtractor(node -> node.getReceivedMessages() + "");
		table.addColumn("reconnects", "Reconnects", new TextField()).setDefaultWidth(70).setValueExtractor(node -> node.getReconnects() + "");
		table.addColumn("services", "Services", new TagComboBox<String>()).setDefaultWidth(150).setValueExtractor(Node::getServices);

		table.onSingleRowSelected.addListener(selectedNode::set);


		ResponsiveForm form = new ResponsiveForm(120, 200, 0);
		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(450);

		TextField nameField = new TextField();
		TextField hostField = new TextField();
		TextField portField = new TextField();
		TextField sentBytesField = new TextField();
		TextField receivedBytesField = new TextField();
		TextField sentMessagesField = new TextField();
		TextField receivedMessagesField = new TextField();
		TextField reconnectsField = new TextField();
		TagComboBox<String> servicesField = new TagComboBox<>();
		servicesField.setWrappingMode(TagBoxWrappingMode.SINGLE_TAG_PER_LINE);

		formLayout.addLabelAndField(null, "Node ID", nameField);
		formLayout.addLabelAndField(null, "Host", hostField);
		formLayout.addLabelAndField(null, "Port", portField);
		formLayout.addLabelAndField(null, "Bytes sent", sentBytesField);
		formLayout.addLabelAndField(null, "Bytes received", receivedBytesField);
		formLayout.addLabelAndField(null, "Messages sent", sentMessagesField);
		formLayout.addLabelAndField(null, "Messages received", receivedMessagesField);
		formLayout.addLabelAndField(null, "Reconnects", reconnectsField);
		formLayout.addLabelAndField(null, "Services", servicesField);


		selectedNode.onChanged().addListener(node -> {
			nameField.setValue(node.getNodeId());
			hostField.setValue(node.getHostAddress().getHost());
			portField.setValue(node.getHostAddress().getPort() + "");
			sentBytesField.setValue(readableFileSize(node.getSentBytes()));
			receivedBytesField.setValue(readableFileSize(node.getReceivedBytes()));
			sentMessagesField.setValue(node.getSentMessages() + "");
			receivedMessagesField.setValue(node.getReceivedMessages() + "");
			reconnectsField.setValue(node.getReconnects() + "");
			servicesField.setValue(node.getServices());
		});

		ToolbarButtonGroup buttonGroup = centerView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.NAV_REFRESH, "Refresh view", "Refresh all values")).onClick.addListener(() -> table.setRecords(cluster.getRemoteNodes()));

		centerView.setComponent(table);
		rightView.setComponent(form);
	}

	public static String readableFileSize(long size) {
		if(size <= 0) return "0";
		final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

}

