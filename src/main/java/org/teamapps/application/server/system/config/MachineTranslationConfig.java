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
package org.teamapps.application.server.system.config;

public class MachineTranslationConfig {

	private boolean active;
	private String deepLKey = "deepKey";
	private boolean deepLFreeApi;
	private String googleKey = "googleKey";

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getDeepLKey() {
		return deepLKey;
	}

	public boolean isDeepLFreeApi() {
		return deepLFreeApi;
	}

	public void setDeepLFreeApi(boolean deepLFreeApi) {
		this.deepLFreeApi = deepLFreeApi;
	}

	public void setDeepLKey(String deepLKey) {
		this.deepLKey = deepLKey;
	}

	public String getGoogleKey() {
		return googleKey;
	}

	public void setGoogleKey(String googleKey) {
		this.googleKey = googleKey;
	}
}
