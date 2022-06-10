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
import org.teamapps.application.tools.EntityListModelBuilder;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.event.Event;
import org.teamapps.formatter.FileSizeFormatter;
import org.teamapps.model.controlcenter.NewsBoardMessageImage;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.session.SessionContext;

import java.util.*;

public class ImageListView extends AbstractApplicationView {

	private Table<NewsBoardMessageImage> table;
	private final EntityListModelBuilder<NewsBoardMessageImage> modelBuilder;

	public ImageListView(ApplicationInstanceData applicationInstanceData) {
		super(applicationInstanceData);
		modelBuilder = new EntityListModelBuilder<>(applicationInstanceData);
		//modelBuilder.setEntityStringFunction(s -> (String) propertyProvider.getValues(s, null).get(BaseTemplate.PROPERTY_CAPTION));
		SessionContext context = SessionContext.current();
		PropertyProvider<NewsBoardMessageImage> propertyProvider = (newsBoardMessageImage, collection) -> {
			Map<String, Object> map =new HashMap<>();
			map.put(BaseTemplate.PROPERTY_IMAGE, context.createFileLink(newsBoardMessageImage.getThumbnail().retrieveFile())); //TODO!
			map.put(BaseTemplate.PROPERTY_CAPTION, newsBoardMessageImage.getFileName());
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, FileSizeFormatter.humanReadableByteCount(newsBoardMessageImage.getFile().getSize(), false, 2));
			return map;
		};
		table = modelBuilder.createTemplateFieldTableList(BaseTemplate.LIST_ITEM_VERY_LARGE_ICON_TWO_LINES, propertyProvider, 52);

	}

	public Component getComponent() {
		return table;
	}

	public NewsBoardMessageImage getSelectedImage() {
		return modelBuilder.getSelectedRecord();
	}

	public void removeSelectedImage() {
		modelBuilder.removeRecord(modelBuilder.getSelectedRecord());
	}

	public void setImages(List<NewsBoardMessageImage> images) {
		modelBuilder.setRecords(images);
	}

	public void addImages(List<NewsBoardMessageImage> images) {
		modelBuilder.addRecords(images);
	}


}
