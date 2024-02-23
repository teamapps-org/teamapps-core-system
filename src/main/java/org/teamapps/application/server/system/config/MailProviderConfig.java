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
package org.teamapps.application.server.system.config;

import java.util.Arrays;
import java.util.List;

public class MailProviderConfig {

	private boolean active = true;
	private boolean defaultProvider = true;
	private String user = "user";
	private String password = "password";
	private String smtpHost = "smtpHost";
	private String messageHeaderDomain = "messageHeaderDomain";
	private String fromEmail = "fromEmail";
	private List<String> filters = Arrays.asList("filter1", "filter2");

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isDefaultProvider() {
		return defaultProvider;
	}

	public void setDefaultProvider(boolean defaultProvider) {
		this.defaultProvider = defaultProvider;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSmtpHost() {
		return smtpHost;
	}

	public void setSmtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
	}

	public String getMessageHeaderDomain() {
		return messageHeaderDomain;
	}

	public void setMessageHeaderDomain(String messageHeaderDomain) {
		this.messageHeaderDomain = messageHeaderDomain;
	}

	public String getFromEmail() {
		return fromEmail;
	}

	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}

	public List<String> getFilters() {
		return filters;
	}

	public void setFilters(List<String> filters) {
		this.filters = filters;
	}
}
