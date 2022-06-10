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

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.api.ui.FormMetaFields;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.form.FormController;
import org.teamapps.application.ux.localize.TranslatableField;
import org.teamapps.application.ux.localize.TranslatableTextUtils;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.application.ux.view.MasterDetailController;
import org.teamapps.common.format.Color;
import org.teamapps.databinding.MutableValue;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.OrganizationField;
import org.teamapps.model.controlcenter.OrganizationUnit;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;

public class OrganizationFieldPerspective extends AbstractManagedApplicationPerspective {

	public OrganizationFieldPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		createUi();
	}

	private void createUi() {
		MasterDetailController<OrganizationField> masterDetailController = new MasterDetailController<>(ApplicationIcons.ELEMENTS_TREE, getLocalized("organizationField.organizationFields"), getApplicationInstanceData(), OrganizationField::filter, Privileges.ORGANIZATION_FIELD_PERSPECTIVE);
		EntityModelBuilder<OrganizationField> entityModelBuilder = masterDetailController.getEntityModelBuilder();
		FormController<OrganizationField> formController = masterDetailController.getFormController();
		ResponsiveForm<OrganizationField> form = masterDetailController.getResponsiveForm();

		Table<OrganizationField> table = entityModelBuilder.createTemplateFieldTableList(BaseTemplate.LIST_ITEM_LARGE_ICON_SINGLE_LINE, PropertyProviders.createOrganizationFieldPropertyProvider(getApplicationInstanceData()), 38);
		entityModelBuilder.updateModels();

		TranslatableField translatableNameField = TranslatableTextUtils.createTranslatableField(getApplicationInstanceData());
		ComboBox<Icon> iconComboBox = ApplicationIcons.createIconComboBox(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, true);
		iconComboBox.setDropDownTemplate(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE);

		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(450);
		formLayout.addSection().setCollapsible(false).setDrawHeaderLine(false);
		formLayout.addLabelAndField(null, getLocalized("organizationField.organizationFieldName"), translatableNameField);
		formLayout.addLabelAndField(null, getLocalized("organizationField.icon"), iconComboBox);

		formController.addNotNull(translatableNameField);
		formController.addNotNull(iconComboBox);

		masterDetailController.createViews(getPerspective(), table, formLayout);

		formController.setSaveEntityHandler(field -> {
			field
					.setTitle(translatableNameField.getValue())
					.setIcon(IconUtils.encodeNoStyle(iconComboBox.getValue()));
			return true;
		});

		entityModelBuilder.getOnSelectionEvent().addListener(type -> {
			translatableNameField.setValue(type.getTitle());
			iconComboBox.setValue(IconUtils.decodeIcon(type.getIcon()));
		});
		entityModelBuilder.setSelectedRecord(OrganizationField.create());
	}


}

