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

public class MailConfig {

	private String callBackDomain = "example.com";
	private String mailFooter = "footer";
	private String fromName = "the name";

	private List<MailProviderConfig> mailProviders = Arrays.asList(new MailProviderConfig());


	public String getCallBackDomain() {
		return callBackDomain;
	}

	public void setCallBackDomain(String callBackDomain) {
		this.callBackDomain = callBackDomain;
	}

	public String getMailFooter() {
		return mailFooter;
	}

	public void setMailFooter(String mailFooter) {
		this.mailFooter = mailFooter;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public List<MailProviderConfig> getMailProviders() {
		return mailProviders;
	}

	public void setMailProviders(List<MailProviderConfig> mailProviders) {
		this.mailProviders = mailProviders;
	}
}
