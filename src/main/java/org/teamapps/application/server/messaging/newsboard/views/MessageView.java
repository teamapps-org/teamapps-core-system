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
package org.teamapps.application.server.messaging.newsboard.views;

import org.teamapps.application.api.application.AbstractApplicationView;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.model.controlcenter.NewsBoardMessage;
import org.teamapps.model.controlcenter.User;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.format.HorizontalElementAlignment;
import org.teamapps.ux.component.pageview.MessagePageViewBlock;
import org.teamapps.ux.component.pageview.PageView;
import org.teamapps.ux.component.pageview.PageViewBlockAlignment;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.toolbutton.ToolButton;
import org.teamapps.ux.session.SessionContext;

import java.util.*;
import java.util.stream.Collectors;

public class MessageView extends AbstractApplicationView {

	private final List<String> preferredLanguages;
	private final ViewMessageHandler viewMessageHandler;
	private PageView pageView;

	public MessageView(NewsBoardMessage message, ApplicationInstanceData applicationInstanceData, List<String> preferredLanguages, ViewMessageHandler viewMessageHandler) {
		this(Collections.singletonList(message), applicationInstanceData, preferredLanguages, viewMessageHandler);
	}

	public MessageView(List<NewsBoardMessage> messages, ApplicationInstanceData applicationInstanceData, List<String> preferredLanguages, ViewMessageHandler viewMessageHandler) {
		super(applicationInstanceData);
		this.preferredLanguages = preferredLanguages;
		this.viewMessageHandler = viewMessageHandler;
		pageView = new PageView();
		messages.forEach(message -> addMessageBlock(message, pageView));
	}

	public Component getComponent() {
		return pageView;
	}

	private void addMessageBlock(NewsBoardMessage message, PageView pageView) {
		MessagePageViewBlock<User> messageBlock = new MessagePageViewBlock<>(PageViewBlockAlignment.FULL, BaseTemplate.LIST_ITEM_LARGE_ICON_TWO_LINES, User.getById(message.getMetaCreatedBy()));
		messageBlock.setTopRecordAlignment(HorizontalElementAlignment.STRETCH);
		messageBlock.setTopRecordPropertyProvider(PropertyProviders.createUserPropertyProvider(getApplicationInstanceData()));
		if (viewMessageHandler != null) {
			List<ToolButton> toolButtons = new ArrayList<>();
			ToolButton toolButton = new ToolButton(ApplicationIcons.RADIO_BUTTON_GROUP);
			toolButton.onClick.addListener(() -> viewMessageHandler.handleViewMessageRequest(message));
			toolButtons.add(toolButton);
			messageBlock.setToolButtons(toolButtons);
		}

		Map<String, String> messageByLanguage = new HashMap<>();
		messageByLanguage.put(message.getLanguage(), message.getHtmlMessage());
		message.getTranslations().forEach(translation -> messageByLanguage.put(translation.getLanguage(), translation.getTranslation()));

		String htmlMessage = message.getHtmlMessage();
		for (String language : preferredLanguages) {
			String value = messageByLanguage.get(language);
			if (value != null) {
				htmlMessage = value;
				break;
			}
		}
		if (!message.isPublished()) {
			htmlMessage = "<p style=\"border-style:solid;border-color:#f07d00;border-width:1px;background-color:#ffe3c9;padding:2px\">" + getLocalized("newsBoard.messageIsNotYetPublished") + "</p>" + htmlMessage;
		}
		messageBlock.setHtml(htmlMessage);

		SessionContext context = SessionContext.current();
		List<String> images = message.getImages().stream().filter(image -> image.getFile() != null).map(image -> context.createFileLink(image.getFile().retrieveFile())).collect(Collectors.toList());
		messageBlock.setImageUrls(images);


		pageView.addBlock(messageBlock);
	}

}
