/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
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
package org.teamapps.application.server.controlcenter.core;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.SessionHandler;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.form.FormWindow;
import org.teamapps.cluster.core.Cluster;
import org.teamapps.cluster.core.Node;
import org.teamapps.cluster.core.RemoteNode;
import org.teamapps.databinding.MutableValue;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.field.AbstractField;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.TextField;
import org.teamapps.ux.component.field.combobox.TagBoxWrappingMode;
import org.teamapps.ux.component.field.combobox.TagComboBox;
import org.teamapps.ux.component.field.upload.UploadedFile;
import org.teamapps.ux.component.field.upload.simple.FileItem;
import org.teamapps.ux.component.field.upload.simple.SimpleFileField;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.panel.Panel;
import org.teamapps.ux.component.table.ListTable;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.template.BaseTemplateRecord;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemCoreUpdatePerspective extends AbstractManagedApplicationPerspective {

	private final UserSessionData userSessionData;
	private final TwoWayBindableValue<RemoteNode> selectedNode = TwoWayBindableValue.create();

	public SystemCoreUpdatePerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		PerspectiveSessionData perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		SystemRegistry registry = userSessionData.getRegistry();
		View mainView = getPerspective().addView(View.createView(StandardLayout.CENTER, ApplicationIcons.COMPUTER_CHIP, "Update", null));
		updateView(mainView);

		ToolbarButtonGroup buttonGroup = getPerspective().addWorkspaceButtonGroup(new ToolbarButtonGroup());
		buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.NAV_REFRESH, "Refresh", "Update the view")).onClick.addListener(() -> updateView(mainView));
		buttonGroup = getPerspective().addWorkspaceButtonGroup(new ToolbarButtonGroup());

		buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.INSTALL, "Install new system", "Update current base system")).onClick.addListener(() -> {
			FormWindow formWindow = new FormWindow(ApplicationIcons.UPLOAD, "Install new core system", getApplicationInstanceData());
			SimpleFileField uploadField = new SimpleFileField();
			uploadField.setMaxBytesPerFile(Integer.MAX_VALUE);
			formWindow.addField(null, "Upload", uploadField);
			uploadField.onUploadSuccessful.addListener(uploadedFile -> {
				File file = uploadedFile.getFile();
				System.out.println("Uploaded system with file:" + file.getPath() + ", " + uploadedFile.getFileName());
			});
			formWindow.addButton(ApplicationIcons.INSTALL, "Install new system").onClick.addListener(() -> {
				if (uploadField.getValue() != null && uploadField.getValue().size() == 1) {
					FileItem uploadedFile = uploadField.getValue().get(0);
					File file = uploadedFile.getFile();
					try {
						System.out.println("Update system with file:" + file.getPath());
						formWindow.close();
						registry.getSessionManager().updateSessionHandler(file);
						UiUtils.showNotification(ApplicationIcons.OK, "New system core successfully installed");
					} catch (Exception e) {
						UiUtils.showNotification(ApplicationIcons.ERROR, "Error installing new system core:" + e.getMessage());
						throw new RuntimeException(e);
					}
				}
			});
			formWindow.addCancelButton();
			formWindow.show();
		});
	}

	private void updateView(View view) {
		System.gc();
		SystemRegistry registry = userSessionData.getRegistry();
		List<SessionHandler> sessionHandlers = registry.getServerRegistry().getSessionHandlers();

		ResponsiveForm<?> form = new ResponsiveForm<>(120, 0, 0);
		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(450);

		for (SessionHandler sessionHandler : sessionHandlers) {
			List<BaseTemplateRecord<Long>> activeUsers = sessionHandler.getActiveUsers();
			formLayout.addSection(ApplicationIcons.COMPUTER_CHIP, "Core System");
			ListTable<BaseTemplateRecord<Long>> table = new ListTable<>(activeUsers);
			table.addColumn("theData", "User", new TemplateField<BaseTemplateRecord<Long>>(BaseTemplate.LIST_ITEM_MEDIUM_ICON_TWO_LINES).setPropertyProvider((record, collection) -> {
				Map<String, Object> map = new HashMap<>();
				map.put(BaseTemplate.PROPERTY_ICON, record.getIcon());
				map.put(BaseTemplate.PROPERTY_IMAGE, record.getImage());
				map.put(BaseTemplate.PROPERTY_CAPTION, record.getCaption());
				map.put(BaseTemplate.PROPERTY_DESCRIPTION, record.getDescription());
				map.put(BaseTemplate.PROPERTY_BADGE, new Date(record.getPayload()).toString());
				return map;
			}));
			table.setPropertyExtractor((record, propertyName) -> record);
			table.setDisplayAsList(true);
			table.setHideHeaders(true);
			table.setRowHeight(40);
			table.setStripedRows(false);
			table.setForceFitWidth(true);
			Panel panel = new Panel(ApplicationIcons.USERS_CROWD, "Active users in core system");
			panel.setCssStyle("height", "300px");
			panel.setContent(table);
			formLayout.addLabelAndComponent(panel);
		}

		view.setComponent(form);

	}


}

