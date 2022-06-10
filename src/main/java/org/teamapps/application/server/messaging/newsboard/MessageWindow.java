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
package org.teamapps.application.server.messaging.newsboard;

import net.coobird.thumbnailator.Thumbnailator;
import org.teamapps.application.api.application.AbstractApplicationView;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.messaging.MessagingPrivileges;
import org.teamapps.application.server.messaging.newsboard.views.EditorView;
import org.teamapps.application.server.messaging.newsboard.views.ImageListView;
import org.teamapps.application.server.messaging.newsboard.views.LanguageSelectionView;
import org.teamapps.application.server.messaging.newsboard.views.MessageView;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.ui.dialogue.UploadDialogue;
import org.teamapps.common.format.Color;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.icon.flags.FlagIcon;
import org.teamapps.icons.composite.CompositeIcon;
import org.teamapps.model.controlcenter.NewsBoardMessage;
import org.teamapps.model.controlcenter.NewsBoardMessageImage;
import org.teamapps.model.controlcenter.NewsBoardMessageTranslation;
import org.teamapps.ux.application.ResponsiveApplication;
import org.teamapps.ux.application.layout.ExtendedLayout;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.perspective.Perspective;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.application.view.ViewSize;
import org.teamapps.ux.component.field.upload.simple.FileItem;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;
import org.teamapps.ux.component.window.Window;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MessageWindow extends AbstractApplicationView {

	private final NewsBoardMessage message;
	private final SystemRegistry registry;
	private Window window;
	private ResponsiveApplication application;
	private boolean activeEditor;

	private NewsBoardMessageTranslation selectedTranslation;
	private final EditorView editorView;
	private final View centerView;
	private TwoWayBindableValue<String> selectedLanguage = TwoWayBindableValue.create();

	public MessageWindow(NewsBoardMessage message, NewsBoardPerspective newsBoardPerspective, ApplicationInstanceData applicationInstanceData, SystemRegistry registry) {
		super(applicationInstanceData);
		this.message = message;
		this.registry = registry;
		application = ResponsiveApplication.createApplication();
		Perspective perspective = application.addPerspective(Perspective.createPerspective());


		View languageView = perspective.addView(View.createView(StandardLayout.LEFT, ApplicationIcons.EARTH, getLocalized(Dictionary.LANGUAGE), null));
		languageView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.94f));
		languageView.setVisible(false);

		centerView = perspective.addView(View.createView(StandardLayout.CENTER, ApplicationIcons.MESSAGE, getLocalized("newsBoard.message"), null));
		centerView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.7f));


		View imageView = perspective.addView(View.createView(ExtendedLayout.OUTER_RIGHT, ApplicationIcons.PHOTO_LANDSCAPE, getLocalized("newsBoard.images"), null));
		imageView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.94f));
		imageView.setSize(ViewSize.ofAbsoluteWidth(120));
		imageView.setSize(ViewSize.ofRelativeWidth(0.15f));
		imageView.setVisible(false);
		centerView.setSize(ViewSize.ofRelativeWidth(0.8f));

		ToolbarButtonGroup buttonGroup = perspective.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton saveButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.FLOPPY_DISK, getLocalized(Dictionary.SAVE), getLocalized(Dictionary.SAVE_CHANGES)));
		buttonGroup = perspective.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton previewButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.DOCUMENT_NOTEBOOK, getLocalized(Dictionary.PREVIEW), getLocalized(Dictionary.PREVIEW)));
		ToolbarButton editButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.EDIT, getLocalized(Dictionary.EDIT), getLocalized(Dictionary.EDIT)));
		buttonGroup = perspective.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton translationsButton = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.EARTH, FlagIcon.GB_GREAT_BRITAIN), getLocalized("newsBoard.translations"), getLocalized("newsBoard.showHideTranslations")));
		ToolbarButton showOriginalButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.FORM, getLocalized("newsBoard.showOriginal"), getLocalized("newsBoard.showOriginalMessage")));
		buttonGroup = perspective.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton publishButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.NEWSPAPER, getLocalized("newsBoard.publish"), getLocalized("newsBoard.publish")));
		ToolbarButton publishWithTranslationsButton = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.NEWSPAPER, ApplicationIcons.EARTH), getLocalized("newsBoard.publishWithTranslation"), getLocalized("newsBoard.publishWithTranslation")));
		ToolbarButton addTranslationsButton = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.EARTH, ApplicationIcons.ADD), getLocalized("newsBoard.addTranslations"), getLocalized("newsBoard.addTranslations")));
		buttonGroup = perspective.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton hideButton = buttonGroup.addButton(ToolbarButton.create(CompositeIcon.of(ApplicationIcons.NEWSPAPER, ApplicationIcons.SIGN_FORBIDDEN), getLocalized(Dictionary.HIDE), getLocalized(Dictionary.HIDE)));
		buttonGroup = perspective.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton imagesButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.PHOTO_LANDSCAPE, getLocalized("newsBoard.images"), getLocalized("newsBoard.showHideImages")));
		buttonGroup = perspective.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton deleteButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.ERROR, getLocalized(Dictionary.DELETE), getLocalized(Dictionary.DELETE_RECORD)));
		buttonGroup = perspective.addWorkspaceButtonGroup(new ToolbarButtonGroup());
		ToolbarButton cancelAndCloseButton = buttonGroup.addButton(ToolbarButton.create(ApplicationIcons.WINDOW_CLOSE, getLocalized(Dictionary.CANCEL), getLocalized(Dictionary.CANCEL_CLOSE)));
		cancelAndCloseButton.onClick.addListener(() -> window.close());


		LanguageSelectionView languageSelectionView = new LanguageSelectionView(getApplicationInstanceData());
		languageSelectionView.onLanguageSelection.addListener(this::handleLanguageSelection);
		languageView.setComponent(languageSelectionView.getComponent());

		ImageListView imageListView = new ImageListView(getApplicationInstanceData());
		imageListView.setImages(new ArrayList<>(message.getImages()));
		imageView.setComponent(imageListView.getComponent());
		buttonGroup = imageView.addLocalButtonGroup(new ToolbarButtonGroup());
		ToolbarButton addImageButton = buttonGroup.addButton(ToolbarButton.createTiny(ApplicationIcons.ADD, getLocalized(Dictionary.ADD)));
		ToolbarButton deleteImageButton = buttonGroup.addButton(ToolbarButton.createTiny(ApplicationIcons.ERROR, getLocalized(Dictionary.DELETE)));

		addImageButton.onClick.addListener(() -> {
			UploadDialogue.createMultiFileUploadDialogue(files -> {
				List<NewsBoardMessageImage> imageList = files.stream().map(file -> createImage(message, file)).filter(Objects::nonNull).collect(Collectors.toList());
				imageListView.addImages(imageList);
			}, Arrays.asList(".jpg", ".jpeg", "png", ".JPG", ".PNG"), getApplicationInstanceData());
		});

		deleteImageButton.onClick.addListener(() -> {
			NewsBoardMessageImage selectedImage = imageListView.getSelectedImage();
			if (selectedImage != null) {
				message.removeImages(selectedImage);
				imageListView.removeSelectedImage();
			}
		});

		editorView = new EditorView(getApplicationInstanceData());
		editorView.onTextUpdate.addListener(this::handleEditorUpdate);
		//MessageView messageView = new MessageView(NewsBoardMessage.create(), applicationInstanceData, getUser().getRankedLanguages(), null);

		showOriginalButton.setVisible(false); //todo check language

		if (message.isPublished()) {
			publishButton.setVisible(false);
			publishWithTranslationsButton.setVisible(false);
			addTranslationsButton.setVisible(true);
			hideButton.setVisible(true);
			languageSelectionView.setLanguages(NewsBoardUtils.getUsedLanguageValues(Collections.singletonList(message)));
		} else {
			publishButton.setVisible(true);
			publishWithTranslationsButton.setVisible(true);
			addTranslationsButton.setVisible(false);
			hideButton.setVisible(false);
			languageSelectionView.setLanguages(NewsBoardUtils.getUsedLanguageValues(Collections.singletonList(message)));
		}

		if (message.isStored()) {
			editButton.setVisible(true);
			previewButton.setVisible(false);
			saveButton.setVisible(false);
			deleteButton.setVisible(true);
			MessageView messageView = new MessageView(message, applicationInstanceData, getUser().getRankedLanguages(), null);
			centerView.setComponent(messageView.getComponent());
		} else {
			editButton.setVisible(false);
			previewButton.setVisible(true);
			saveButton.setVisible(true);
			deleteButton.setVisible(false);
			editorView.showMessage(message.getHtmlMessage());
			centerView.setComponent(editorView.getComponent());
			activeEditor = true;
		}

		editButton.onClick.addListener(() -> {
			editButton.setVisible(false);
			previewButton.setVisible(true);
			saveButton.setVisible(true);
			activeEditor = true;
			updateView();
		});

		previewButton.onClick.addListener(() -> {
			editButton.setVisible(true);
			previewButton.setVisible(false);
			saveButton.setVisible(false);
			activeEditor = false;
			updateView();
		});

		saveButton.onClick.addListener(() -> {
			if (selectedTranslation != null) {
				selectedTranslation.save();
			}
			if (message.getHtmlMessage() != null) {
				message.save();
				//newsBoardPerspective.updateMessages();
				addTranslationsButton.setVisible(true);
			}
		});

		deleteButton.onClick.addListener(() -> {
			//todo check...
			message.delete();
			newsBoardPerspective.updateMessages();
			window.close();
		});

		hideButton.onClick.addListener(() -> {
			message.setPublished(false);
		});

		imagesButton.onClick.addListener(() -> imageView.setVisible(!imageView.isVisible()));
		translationsButton.onClick.addListener(() -> languageView.setVisible(!languageView.isVisible()));

		addTranslationsButton.onClick.addListener(() -> {
			if (message.getHtmlMessage() == null || message.getHtmlMessage().isBlank()) {
				return;
			}
			new TranslationSelectionDialogue(message, false, registry.getSystemConfig().getLocalizationConfig(), registry.getTranslationService(), () -> {
				languageSelectionView.setLanguages(NewsBoardUtils.getUsedLanguageValues(Collections.singletonList(message)));
			}, getApplicationInstanceData());
		});

		publishWithTranslationsButton.onClick.addListener(() -> {
			if (message.getHtmlMessage() == null || message.getHtmlMessage().isBlank()) {
				return;
			}
			new TranslationSelectionDialogue(message, true, registry.getSystemConfig().getLocalizationConfig(), registry.getTranslationService(), () -> {
				message.setPublished(true).save();
				window.close();
				newsBoardPerspective.updateMessages();
			}, getApplicationInstanceData());
		});

		publishButton.onClick.addListener(() -> {
			if (message.getHtmlMessage() != null) {
				message.setPublished(true).save();
				window.close();
				newsBoardPerspective.updateMessages();
			}
		});

		if (!isAllowed(MessagingPrivileges.NEWS_BOARD_ADMIN_ACCESS)) {
			return;
		}
		application.showPerspective(perspective);
		window = new Window(1100, 800, application.getUi());
		window.setIcon(ApplicationIcons.MESSAGE);
		window.setTitle(getLocalized("newsBoard.message"));
		window.setBodyBackgroundColor(Color.WHITE.withAlpha(0.3f));
		window.setMaximizable(true);
		window.setCloseable(true);
		window.setCloseOnEscape(true);
		window.show();
	}

	private void handleLanguageSelection(String language) {
		selectedLanguage.set(language);
		updateView();
	}

	private void updateView() {
		String language = selectedLanguage.get();
		NewsBoardMessageTranslation messageTranslation = message.getTranslations().stream().filter(translation -> translation.getLanguage().equals(language)).findFirst().orElse(null);
		if (messageTranslation != null) {
			selectedTranslation = messageTranslation;
		} else {
			selectedTranslation = null;
		}
		if (activeEditor) {
			if (selectedTranslation != null) {
				editorView.showMessage(selectedTranslation.getTranslation());
			} else {
				editorView.showMessage(message.getHtmlMessage());
			}
			centerView.setComponent(editorView.getComponent());
		} else {
			MessageView messageView;
			if (selectedTranslation != null) {
				messageView = new MessageView(message, getApplicationInstanceData(), Collections.singletonList(language), null);
			} else {
				messageView = new MessageView(message, getApplicationInstanceData(), getUser().getRankedLanguages(), null);
			}
			centerView.setComponent(messageView.getComponent());
		}
	}

	private void handleEditorUpdate(String text) {
		if (selectedTranslation != null) {
			selectedTranslation.setTranslation(text);
		} else {
			message.setHtmlMessage(text);
		}
	}

	private NewsBoardMessageImage createImage(NewsBoardMessage message, FileItem fileItem) {
		try {
			File file = fileItem.getFile();
			File tempFile = File.createTempFile("temp", ".jpg");
			Thumbnailator.createThumbnail(file, tempFile, 100, 100);
			NewsBoardMessageImage messageImage = NewsBoardMessageImage.create()
					.setFile(file)
					.setFileName(fileItem.getFileName())
					.setThumbnail(tempFile)
					.setPosition(0);
			message.addImages(messageImage);
			return messageImage;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
