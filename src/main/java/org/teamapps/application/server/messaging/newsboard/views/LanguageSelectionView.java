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
import org.teamapps.application.server.messaging.newsboard.NewsBoardUtils;
import org.teamapps.application.tools.RecordListModelBuilder;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.event.Event;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.template.BaseTemplate;

import java.util.Collections;
import java.util.List;

public class LanguageSelectionView extends AbstractApplicationView {

	public Event<String> onLanguageSelection = new Event<>();
	private Table<String> table;
	private final RecordListModelBuilder<String> modelBuilder;

	public LanguageSelectionView(ApplicationInstanceData applicationInstanceData) {
		super(applicationInstanceData);
		PropertyProvider<String> propertyProvider = NewsBoardUtils.createLanguageSelectionPropertyProvider(applicationInstanceData);
		modelBuilder = new RecordListModelBuilder<>(applicationInstanceData);
		modelBuilder.setRecordStringFunction(s -> (String) propertyProvider.getValues(s, null).get(BaseTemplate.PROPERTY_CAPTION));
		table = modelBuilder.createTemplateFieldTableList(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, propertyProvider, 30);
		modelBuilder.getOnSelectionEvent().addListener(language -> onLanguageSelection.fire(language));
	}

	public Component getComponent() {
		return table;
	}

	public void setOwnLanguage() {
		modelBuilder.setRecords(Collections.singletonList(NewsBoardUtils.USER_LANGUAGES));
	}

	public void setLanguages(List<String> languages) {
		modelBuilder.setRecords(languages);
	}
}
