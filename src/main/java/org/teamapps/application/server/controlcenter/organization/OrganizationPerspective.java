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
import org.teamapps.application.api.application.perspective.PerspectiveMenuPanel;
import org.teamapps.application.api.localization.Country;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.api.ui.FormMetaFields;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.ui.address.AddressForm;
import org.teamapps.application.tools.EntityModelBuilder;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.form.FormController;
import org.teamapps.application.ux.localize.TranslatableField;
import org.teamapps.application.ux.localize.TranslatableTextUtils;
import org.teamapps.application.ux.view.MasterDetailController;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.databinding.MutableValue;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.GeoLocationType;
import org.teamapps.model.controlcenter.OrganizationUnit;
import org.teamapps.model.controlcenter.OrganizationUnitType;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.tree.Tree;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrganizationPerspective extends AbstractManagedApplicationPerspective {

	public OrganizationPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		createUi();
	}

	private void createUi() {
		OrganizationPerspectiveBuilder organizationPerspectiveBuilder = new OrganizationPerspectiveBuilder();
		PerspectiveMenuPanel menuPanel = PerspectiveMenuPanel.createMenuPanel(getApplicationInstanceData(),
				new OrganizationChartPerspectiveBuilder(),
				organizationPerspectiveBuilder,
				new OrganizationUnitTypePerspectiveBuilder(),
				new OrganizationFieldPerspectiveBuilder()
		);
		menuPanel.addInstantiatedPerspective(organizationPerspectiveBuilder, this);
		setPerspectiveMenuPanel(menuPanel.getComponent(), menuPanel.getButtonMenu());

		MasterDetailController<OrganizationUnit> masterDetailController = new MasterDetailController<>(ApplicationIcons.ELEMENTS_HIERARCHY, getLocalized("organization.organizationUnits"), getApplicationInstanceData(), OrganizationUnit::filter, Privileges.ORGANIZATION_UNIT_PERSPECTIVE);
		EntityModelBuilder<OrganizationUnit> entityModelBuilder = masterDetailController.getEntityModelBuilder();
		FormController<OrganizationUnit> formController = masterDetailController.getFormController();
		ResponsiveForm<OrganizationUnit> form = masterDetailController.getResponsiveForm();

		Tree<OrganizationUnit> tree = entityModelBuilder.createTree(BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, PropertyProviders.creatOrganizationUnitPropertyProvider(getApplicationInstanceData()), OrganizationUnit::getParent, unit -> OrganizationUtils.getLevel(unit) < 2);
		entityModelBuilder.updateModels();

		TemplateField<OrganizationUnit> parentUnitField = UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, PropertyProviders.creatOrganizationUnitPropertyProvider(getApplicationInstanceData()));
		TranslatableField translatableNameField = TranslatableTextUtils.createTranslatableField(getApplicationInstanceData());
		ComboBox<OrganizationUnitType> unitTypeComboBox = createOrgUnitTypeComboBox(entityModelBuilder);
		ComboBox<Icon> iconComboBox = ApplicationIcons.createIconComboBox();
		AddressForm addressForm = new AddressForm(getApplicationInstanceData());
		addressForm.setWithName(true);
		addressForm.setWithGeoCoordinates(true);

		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(450);
		formLayout.addSection(ApplicationIcons.FOLDER, getLocalized(""));
		formLayout.addLabelAndField(null, getLocalized("organizationUnit.parentUnit"), parentUnitField);
		formLayout.addLabelAndField(null, getLocalized("organizationUnit.unitName"), translatableNameField);
		formLayout.addLabelAndField(null, getLocalized("organizationUnit.unitType"), unitTypeComboBox);
		formLayout.addLabelAndField(null, getLocalized("organizationUnit.icon"), iconComboBox);

		addressForm.createAddressSection(formLayout);
		addressForm.addFields(formLayout);

		formController.addNotNull(translatableNameField);
		formController.addNotNull(unitTypeComboBox);

		masterDetailController.createViews(getPerspective(), tree, formLayout);

		formController.setCreateNewEntitySupplier(() -> OrganizationUnit.create().setParent(entityModelBuilder.getSelectedRecord()));

		formController.setSaveEntityHandler(unit -> {
			OrganizationUnit parentUnit = unit.getParent();
			if (addressForm.validateAddress() && (parentUnit != null || OrganizationUnit.getCount() == 0)) {
				unit
						.setParent(parentUnit)
						.setName(translatableNameField.getValue())
						.setType(unitTypeComboBox.getValue())
						.setIcon(IconUtils.encodeNoStyle(iconComboBox.getValue()))
						.setAddress(addressForm.getAddress());
				return true;
			} else {
				return false;
			}
		});

		entityModelBuilder.getOnSelectionEvent().addListener(unit -> {
			parentUnitField.setValue(unit.getParent());
			translatableNameField.setValue(unit.getName());
			unitTypeComboBox.setValue(unit.getType());
			iconComboBox.setValue(IconUtils.decodeIcon(unit.getIcon()));
			addressForm.setAddress(unit.getAddress());
			if (!unit.isStored()) {
				if (unit.getParent() != null) {
					unitTypeComboBox.setValue(unit.getParent().getType().getDefaultChildType());
				}

				if (OrganizationUtils.getParentWithGeoType(unit, GeoLocationType.COUNTRY) != null) {
					OrganizationUnit countryParent = OrganizationUtils.getParentWithGeoType(unit, GeoLocationType.COUNTRY);
					if (countryParent.getAddress() != null) {
						addressForm.getCountryComboBox().setValue(Country.getCountryByIsoCode(countryParent.getAddress().getCountry()));
					}
				}
			}
		});

		if (OrganizationUnit.getCount() == 0) {
			entityModelBuilder.setSelectedRecord(OrganizationUnit.create().setParent(null));
		}
	}

	private ComboBox<OrganizationUnitType> createOrgUnitTypeComboBox(EntityModelBuilder<OrganizationUnit> entityModelBuilder) {
		ComboBox<OrganizationUnitType> comboBox = new ComboBox<>(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE);
		PropertyProvider<OrganizationUnitType> propertyProvider = PropertyProviders.creatOrganizationUnitTypePropertyProvider(getApplicationInstanceData());
		comboBox.setModel(query -> {
			OrganizationUnit selectedRecord = entityModelBuilder.getSelectedRecord();
			List<OrganizationUnitType> allowedTypes = selectedRecord != null && selectedRecord.getParent() != null ? selectedRecord.getParent().getType().getPossibleChildrenTypes() : OrganizationUnitType.getAll();
			if (query == null || query.isBlank()) {
				return allowedTypes;
			} else {
				String q = query.toLowerCase();
				return allowedTypes.stream()
						.filter(type -> ((String) propertyProvider.getValues(type, Collections.emptyList()).get(BaseTemplate.PROPERTY_CAPTION)).toLowerCase().contains(q))
						.collect(Collectors.toList());
			}
		});
		comboBox.setPropertyProvider(propertyProvider);
		comboBox.setRecordToStringFunction(unitType -> (String) propertyProvider.getValues(unitType, Collections.emptyList()).get(BaseTemplate.PROPERTY_CAPTION));
		return comboBox;
	}

}

