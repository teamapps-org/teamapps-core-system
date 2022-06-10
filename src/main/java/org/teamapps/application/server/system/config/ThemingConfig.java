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

public class ThemingConfig {

	private String loginBackgroundUrl = "/resources/backgrounds/login.jpg";
	private String applicationBackgroundUrl = "/resources/backgrounds/default-bl.jpg";
	private String applicationSecondaryBackgroundUrl = "/resources/backgrounds/default-bl.jpg";
	private String applicationDarkBackgroundUrl = "/resources/backgrounds/dark.jpg";
	private String applicationDarkSecondaryBackgroundUrl = "/resources/backgrounds/dark.jpg";

	public String getLoginBackgroundUrl() {
		return loginBackgroundUrl;
	}

	public void setLoginBackgroundUrl(String loginBackgroundUrl) {
		this.loginBackgroundUrl = loginBackgroundUrl;
	}

	public String getApplicationBackgroundUrl() {
		return applicationBackgroundUrl;
	}

	public void setApplicationBackgroundUrl(String applicationBackgroundUrl) {
		this.applicationBackgroundUrl = applicationBackgroundUrl;
	}

	public String getApplicationSecondaryBackgroundUrl() {
		return applicationSecondaryBackgroundUrl != null ? applicationSecondaryBackgroundUrl : applicationBackgroundUrl;
	}

	public void setApplicationSecondaryBackgroundUrl(String applicationSecondaryBackgroundUrl) {
		this.applicationSecondaryBackgroundUrl = applicationSecondaryBackgroundUrl;
	}

	public String getApplicationDarkBackgroundUrl() {
		return applicationDarkBackgroundUrl;
	}

	public void setApplicationDarkBackgroundUrl(String applicationDarkBackgroundUrl) {
		this.applicationDarkBackgroundUrl = applicationDarkBackgroundUrl;
	}

	public String getApplicationDarkSecondaryBackgroundUrl() {
		return applicationDarkSecondaryBackgroundUrl != null ? applicationDarkSecondaryBackgroundUrl : applicationDarkBackgroundUrl;
	}

	public void setApplicationDarkSecondaryBackgroundUrl(String applicationDarkSecondaryBackgroundUrl) {
		this.applicationDarkSecondaryBackgroundUrl = applicationDarkSecondaryBackgroundUrl;
	}
}
