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
import org.teamapps.event.Event;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.field.richtext.RichTextEditor;
import org.teamapps.ux.component.field.richtext.ToolbarVisibilityMode;

public class EditorView extends AbstractApplicationView {

	public Event<String> onTextUpdate = new Event<>();
	private RichTextEditor editor;

	public EditorView(ApplicationInstanceData applicationInstanceData) {
		super(applicationInstanceData);
		editor = new RichTextEditor();
		editor.setLocale(getUser().getLocale());
		editor.setToolbarVisibilityMode(ToolbarVisibilityMode.VISIBLE);
		editor.setCssStyle(".UiRichTextEditor", "height", "100%");

		editor.onTextInput.addListener(text -> onTextUpdate.fire(text));

//		editor.setUploadedFileToUrlConverter(uploadedFile -> {
//			File file = uploadedFile.getAsFile();
//
//		});
	}

	public Component getComponent() {
		return editor;
	}

	public void showMessage(String messageHtml) {
		editor.setValue(messageHtml);
	}

	public String getValue() {
		return editor.getValue();
	}
}
