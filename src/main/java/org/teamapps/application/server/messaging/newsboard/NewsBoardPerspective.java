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

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.messaging.MessagingPrivileges;
import org.teamapps.application.server.messaging.newsboard.views.MessageView;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.common.format.Color;
import org.teamapps.databinding.MutableValue;
import org.teamapps.model.controlcenter.NewsBoardMessage;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.field.Button;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.template.BaseTemplateRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewsBoardPerspective extends AbstractManagedApplicationPerspective {

	private View masterView;
	private final UserSessionData userSessionData;


	public NewsBoardPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		PerspectiveSessionData perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		masterView = getPerspective().addView(View.createView(StandardLayout.CENTER, ApplicationIcons.MESSAGE, getLocalized("newsBoard.title"), null));
		masterView.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.1f));

		updateMessages();

		if (isAllowed(MessagingPrivileges.NEWS_BOARD_ADMIN_ACCESS)) {
			Button<BaseTemplateRecord> addButton = Button.create(ApplicationIcons.ADD, getLocalized(Dictionary.ADD));
			masterView.getPanel().setRightHeaderField(addButton);
			addButton.onClicked.addListener(() -> showMessageWindow(null));
		}
	}

	public void updateMessages() {
		updateMessages(getUser().getRankedLanguages());
	}

	public void updateMessages(List<String> rankedLanguages) {
		List<NewsBoardMessage> messages = isAppFilter() ? NewsBoardMessage.filter().organizationField(NumericFilter.equalsFilter(getOrganizationField().getId())).execute() : NewsBoardMessage.getAll();
		messages = new ArrayList<>(messages);
		ComboBox<String> languageComboBox = NewsBoardUtils.createLanguageSelectionComboBox(NewsBoardUtils.getUsedLanguageValues(messages), getApplicationInstanceData());
		masterView.getPanel().setLeftHeaderField(languageComboBox);
		Collections.reverse(messages);
		MessageView messageView = new MessageView(messages, getApplicationInstanceData(), rankedLanguages, this::showMessageWindow);
		masterView.setComponent(messageView.getComponent());
		languageComboBox.setValue(rankedLanguages.get(0));
		languageComboBox.onValueChanged.addListener(language -> {
			if (NewsBoardUtils.USER_LANGUAGES.equals(language)) {
				updateMessages();
			} else {
				updateMessages(Collections.singletonList(language));
			}
		});
	}


	private void showMessageWindow(NewsBoardMessage message) {
		if (!isAllowed(MessagingPrivileges.NEWS_BOARD_ADMIN_ACCESS)) {
			return;
		}
		if (message == null) {
			message = NewsBoardMessage.create()
					.setLanguage(getUser().getLocale().getLanguage())
					.setMetaCreatedBy(getUser().getId())
					.setOrganizationField(getOrganizationField());
		}
		new MessageWindow(message, this, getApplicationInstanceData(), userSessionData.getRegistry());
	}
}
