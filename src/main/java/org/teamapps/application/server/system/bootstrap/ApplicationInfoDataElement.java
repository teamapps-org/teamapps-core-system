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
package org.teamapps.application.server.system.bootstrap;

import org.teamapps.model.controlcenter.ApplicationVersionData;

import java.util.ArrayList;
import java.util.List;

public class ApplicationInfoDataElement {

	private String data;
	private List<String> dataAdded = new ArrayList<>();
	private List<String> dataRemoved = new ArrayList<>();

	public static String getChangeString(ApplicationVersionData versionData) {
		return versionData != null ? "+ " + versionData.getDataAddedRows() + " | - " + versionData.getDataRemovedRows() : null;
	}

	public static String getMultiLineChangeHtml(ApplicationVersionData versionData, String addedTitle, String removedTitle) {
		if (versionData == null) {
			return null;
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("<b>").append(addedTitle).append("</b>:<br>");
			sb.append(versionData.getDataAdded() != null ? versionData.getDataAdded().replace("\n", "<br>") : "");
			sb.append("<br><b>").append(removedTitle).append("</b>:<br>");
			sb.append(versionData.getDataRemoved() != null ? versionData.getDataRemoved().replace("\n", "<br>"): "");
			return sb.toString();
		}
	}

	public ApplicationInfoDataElement() {

	}

	public String getChangeString() {
		return "+ " + dataAdded.size() + " | - " + dataRemoved.size();
	}

	public String getMultiLineChangeHtml(String addedTitle, String removedTitle) {
		StringBuilder sb = new StringBuilder();
		sb.append("<b>").append(addedTitle).append("</b>:<br>");
		dataAdded.forEach(v -> sb.append(v).append("<br>"));
		sb.append("<br><b>").append(removedTitle).append("</b>:<br>");
		dataRemoved.forEach(v -> sb.append(v).append("<br>"));
		return sb.toString();
	}

	public void added(String element) {
		dataAdded.add(element);
	}

	public void removed(String element) {
		dataRemoved.add(element);
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public List<String> getDataAdded() {
		return dataAdded;
	}

	public void setDataAdded(List<String> dataAdded) {
		this.dataAdded = dataAdded;
	}

	public List<String> getDataRemoved() {
		return dataRemoved;
	}

	public void setDataRemoved(List<String> dataRemoved) {
		this.dataRemoved = dataRemoved;
	}

	@Override
	public String toString() {
		return "ApplicationInfoDataElement{" +
				"data='" + data + '\'' +
				", dataAdded=" + dataAdded +
				", dataRemoved=" + dataRemoved +
				'}';
	}
}
