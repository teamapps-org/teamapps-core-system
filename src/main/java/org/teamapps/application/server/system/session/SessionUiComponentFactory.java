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
package org.teamapps.application.server.system.session;

import net.coobird.thumbnailator.Thumbnailator;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.ui.FormMetaFields;
import org.teamapps.application.api.ui.TranslationKeyField;
import org.teamapps.application.api.ui.UiComponentFactory;
import org.teamapps.application.api.user.SessionUser;
import org.teamapps.application.server.EmbeddedResourceStore;
import org.teamapps.application.server.system.bootstrap.BaseResourceLinkProvider;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.organization.OrganizationUtils;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.ui.localize.LocalizationTranslationKeyField;
import org.teamapps.application.ux.UiUtils;
import org.teamapps.application.ux.combo.ComboBoxUtils;
import org.teamapps.application.ux.form.FormMetaFieldsImpl;
import org.teamapps.application.ux.localize.TranslatableField;
import org.teamapps.application.ux.org.OrganizationViewUtils;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.ux.component.field.TemplateField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.field.combobox.TagComboBox;
import org.teamapps.ux.component.field.richtext.RichTextEditor;
import org.teamapps.ux.component.template.BaseTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

	public ApplicationInstanceData getApplicationInstanceData() {
		return applicationInstanceData;
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
		return UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, OrganizationViewUtils.creatOrganizationUnitPropertyProvider(applicationInstanceData));
	}

	@Override
	public TemplateField<Integer> createUserTemplateField() {
		return UiUtils.createTemplateField(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, PropertyProviders.createUserIdPropertyProvider(applicationInstanceData));
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

	@Override
	public RichTextEditor createEmbeddedImagesEnabledRichTextEditor(String bucket) {
		RichTextEditor editor = new RichTextEditor();
		editor.setLocale(applicationInstanceData.getUser().getLocale());
		editor.setMaxImageFileSizeInBytes(Integer.MAX_VALUE);
		editor.setImageUploadEnabled(true);
		editor.setUploadedFileToUrlConverter(uploadedFile -> {
			try {
				String link = null;
				if (uploadedFile.getSizeInBytes() > 300_000) {
					File newFile = Files.createTempFile("temp", ".jpg").toFile();
					Thumbnailator.createThumbnail(uploadedFile.getAsFile(), newFile, 1_200, 1_000);
					link = createLink(newFile, bucket, uploadedFile.getName());
				} else {
					link = createLink(uploadedFile.getAsFile(), bucket, uploadedFile.getName());
				}
				return link;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		return editor;
	}

	@Override
	public void showDeleteQuestion(Runnable onConfirmation) {
		UiUtils.showDeleteQuestion(onConfirmation, applicationInstanceData);
	}

	@Override
	public void showQuestion(Icon icon, String title, String text, Runnable onConfirmation) {
		UiUtils.showQuestion(icon, title, text, onConfirmation, applicationInstanceData);
	}

	private String createLink(File file, String bucket, String name) throws IOException {
		return EmbeddedResourceStore.getInstance().saveResource(application.getName(), bucket, file) + "/" + name;
	}
}
