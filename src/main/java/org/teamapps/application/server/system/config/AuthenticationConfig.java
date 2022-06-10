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

public class AuthenticationConfig {

	private boolean allowStoringSecurityTokensOnClient = true;
	private boolean allowPasswordReset;
	private boolean allowRegistration;

	private boolean checkTermsOfUse = false;
	private int currentTermsOfUseVersion = 1;
	private String termsOfUseLink = "https://www.example.com/terms-of.use.html";

	private boolean checkPrivacyPolicy = false;
	private int currentPrivacyPolicyVersion = 1;
	private String privacyPolicyLink = "https://www.example.com/privacy-policy.html";

	private boolean enableAutoLoginUrls = false;
	private String autoLoginSecret = "the-secret";
	private int autoLoginUrlValidityInSeconds = 30;

	public boolean isAllowStoringSecurityTokensOnClient() {
		return allowStoringSecurityTokensOnClient;
	}

	public void setAllowStoringSecurityTokensOnClient(boolean allowStoringSecurityTokensOnClient) {
		this.allowStoringSecurityTokensOnClient = allowStoringSecurityTokensOnClient;
	}

	public boolean isAllowPasswordReset() {
		return allowPasswordReset;
	}

	public void setAllowPasswordReset(boolean allowPasswordReset) {
		this.allowPasswordReset = allowPasswordReset;
	}

	public boolean isAllowRegistration() {
		return allowRegistration;
	}

	public void setAllowRegistration(boolean allowRegistration) {
		this.allowRegistration = allowRegistration;
	}

	public boolean isCheckTermsOfUse() {
		return checkTermsOfUse;
	}

	public void setCheckTermsOfUse(boolean checkTermsOfUse) {
		this.checkTermsOfUse = checkTermsOfUse;
	}

	public int getCurrentTermsOfUseVersion() {
		return currentTermsOfUseVersion;
	}

	public void setCurrentTermsOfUseVersion(int currentTermsOfUseVersion) {
		this.currentTermsOfUseVersion = currentTermsOfUseVersion;
	}

	public String getTermsOfUseLink() {
		return termsOfUseLink;
	}

	public void setTermsOfUseLink(String termsOfUseLink) {
		this.termsOfUseLink = termsOfUseLink;
	}

	public boolean isCheckPrivacyPolicy() {
		return checkPrivacyPolicy;
	}

	public void setCheckPrivacyPolicy(boolean checkPrivacyPolicy) {
		this.checkPrivacyPolicy = checkPrivacyPolicy;
	}

	public int getCurrentPrivacyPolicyVersion() {
		return currentPrivacyPolicyVersion;
	}

	public void setCurrentPrivacyPolicyVersion(int currentPrivacyPolicyVersion) {
		this.currentPrivacyPolicyVersion = currentPrivacyPolicyVersion;
	}

	public String getPrivacyPolicyLink() {
		return privacyPolicyLink;
	}

	public void setPrivacyPolicyLink(String privacyPolicyLink) {
		this.privacyPolicyLink = privacyPolicyLink;
	}

	public boolean isEnableAutoLoginUrls() {
		return enableAutoLoginUrls;
	}

	public void setEnableAutoLoginUrls(boolean enableAutoLoginUrls) {
		this.enableAutoLoginUrls = enableAutoLoginUrls;
	}

	public String getAutoLoginSecret() {
		return autoLoginSecret;
	}

	public void setAutoLoginSecret(String autoLoginSecret) {
		this.autoLoginSecret = autoLoginSecret;
	}

	public int getAutoLoginUrlValidityInSeconds() {
		return autoLoginUrlValidityInSeconds;
	}

	public void setAutoLoginUrlValidityInSeconds(int autoLoginUrlValidityInSeconds) {
		this.autoLoginUrlValidityInSeconds = autoLoginUrlValidityInSeconds;
	}
}
