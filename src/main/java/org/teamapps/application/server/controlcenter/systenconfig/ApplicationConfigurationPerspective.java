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
package org.teamapps.application.server.controlcenter.systenconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.databinding.MutableValue;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.field.MultiLineTextField;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;

import java.lang.invoke.MethodHandles;

public class ApplicationConfigurationPerspective extends AbstractManagedApplicationPerspective {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final UserSessionData userSessionData;
	private final TwoWayBindableValue<Application> selectedApplication = TwoWayBindableValue.create();

	public ApplicationConfigurationPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		PerspectiveSessionData perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		View masterView;
		View detailsView = getPerspective().addView(View.createView(StandardLayout.RIGHT, ApplicationIcons.CODE_LINE, getLocalized("applicationConfiguration.title"), null));

		if (!isAppFilter()) {
			masterView = getPerspective().addView(View.createView(StandardLayout.CENTER, ApplicationIcons.CODE_LINE, getLocalized("applications.title"), null));
			EntityModelBuilder<Application> entityModelBuilder = new EntityModelBuilder<>(Application::filter, getApplicationInstanceData());
			Table<Application> applicationsTable = entityModelBuilder.createTemplateFieldTableList(BaseTemplate.LIST_ITEM_VERY_LARGE_ICON_TWO_LINES, PropertyProviders.createApplicationPropertyProvider(userSessionData), 60);
			entityModelBuilder.attachSearchField(masterView);
			entityModelBuilder.attachViewCountHandler(masterView, () -> getLocalized(Dictionary.APPLICATIONS));
			entityModelBuilder.getOnSelectionEvent().addListener(selectedApplication::set);
			entityModelBuilder.updateModels();
			masterView.setComponent(applicationsTable);
		}

		MultiLineTextField configField = new MultiLineTextField();
		detailsView.setComponent(configField);

		ToolbarButtonGroup buttonGroup = detailsView.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton saveButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.FLOPPY_DISKS, getLocalized(Dictionary.SAVE_CHANGES), getLocalized("applicationConfiguration.updateApplicationConfiguration")));

		saveButton.onClick.addListener(() -> {
			Application application = selectedApplication.get();
			String configXml = configField.getValue();
			if (application == null || configXml == null || configXml.isBlank()) {
				return;
			}
			LoadedApplication loadedApplication = userSessionData.getRegistry().getLoadedApplication(application);
			try {
				loadedApplication.getBaseApplicationBuilder().updateConfig(configXml, loadedApplication.getApplicationClassLoaderOrDefault());
				application.setConfig(configXml).save();
			} catch (Exception e) {
				UiUtils.showNotification(ApplicationIcons.ERROR, e.getMessage());
				LOGGER.error("ERROR UPDATING APPLICATION CONFIG: " + application.getName() + ": " +e.getMessage());
				e.printStackTrace();
			}
		});

		selectedApplication.onChanged().addListener(application -> {
			LoadedApplication loadedApplication = userSessionData.getRegistry().getLoadedApplication(application);
			String xml = loadedApplication.getBaseApplicationBuilder().getApplicationConfigXml(loadedApplication.getApplicationClassLoaderOrDefault());
			configField.setValue(xml);
		});

		if (isAppFilter()) {
			selectedApplication.set(getMainApplication());
		}
	}


}
