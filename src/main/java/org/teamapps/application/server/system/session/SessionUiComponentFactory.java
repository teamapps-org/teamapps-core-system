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
package org.teamapps.application.server.system.session;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.ui.FormMetaFields;
import org.teamapps.application.api.ui.TranslationKeyField;
import org.teamapps.application.api.ui.UiComponentFactory;
import org.teamapps.application.server.system.bootstrap.BaseResourceLinkProvider;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.ui.localize.LocalizationTranslationKeyField;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.form.FormMetaFieldsImpl;
import org.teamapps.application.ux.localize.TranslatableField;
import org.teamapps.application.ux.org.OrganizationViewUtils;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.OrganizationUnitTypeView;
import org.teamapps.model.controlcenter.OrganizationUnitView;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.field.combobox.TagComboBox;
import org.teamapps.ux.component.template.BaseTemplate;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public class SessionUiComponentFactory implements UiComponentFactory {

	private final ApplicationInstanceData applicationInstanceData;
	private final SystemRegistry systemRegistry;
	private final BaseResourceLinkProvider baseResourceLinkProvider;
	private final Application application;

	public SessionUiComponentFactory(ApplicationInstanceData applicationInstanceData, SystemRegistry systemRegistry, Application application) {
		this.applicationInstanceData = applicationInstanceData;
		this.systemRegistry = systemRegistry;
		this.baseResourceLinkProvider = systemRegistry.getBaseResourceLinkProvider();
		this.application = application;
	}

	@Override
	public ComboBox<OrganizationUnitView> createOrganizationUnitComboBox(Supplier<Collection<OrganizationUnitView>> allowedUnitsSupplier) {
		return OrganizationViewUtils.createOrganizationComboBox(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, allowedUnitsSupplier, applicationInstanceData);
	}

	@Override
	public ComboBox<OrganizationUnitView> createOrganizationUnitComboBox(Set<OrganizationUnitView> allowedUnits) {
		return OrganizationViewUtils.createOrganizationComboBox(BaseTemplate.LIST_ITEM_SMALL_ICON_SINGLE_LINE, allowedUnits, applicationInstanceData);
	}

	@Override
	public TagComboBox<OrganizationUnitTypeView> createOrganizationUnitTypeTagComboBox() {
		return OrganizationViewUtils.createOrganizationUnitTypeTagComboBox(150, applicationInstanceData);
	}

	@Override
	public TemplateField<OrganizationUnitView> createOrganizationUnitTemplateField() {
		return UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, OrganizationViewUtils.creatOrganizationUnitViewPropertyProvider(applicationInstanceData));
	}

	@Override
	public TemplateField<Integer> createUserTemplateField() {
		return UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.createUserIdPropertyProvider(applicationInstanceData));
	}

	@Override
	public TranslatableField createTranslatableField() {
		return new TranslatableField(applicationInstanceData);
	}

	@Override
	public TranslationKeyField createTranslationKeyField(String linkButtonCaption, boolean allowMultiLine, boolean selectionFieldWithKey) {
		return new LocalizationTranslationKeyField(linkButtonCaption, applicationInstanceData, systemRegistry, () -> application, allowMultiLine, selectionFieldWithKey);
	}

	@Override
	public FormMetaFields createFormMetaFields() {
		return new FormMetaFieldsImpl(applicationInstanceData);
	}

	@Override
	public String createUserAvatarLink(int userId, boolean large) {
		return baseResourceLinkProvider.getUserProfilePictureLink(userId, large);
	}
}
