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
package org.teamapps.application.server.system.auth;

import org.apache.commons.codec.digest.DigestUtils;
import org.teamapps.application.server.system.config.AuthenticationConfig;
import org.teamapps.model.controlcenter.User;
import org.teamapps.model.controlcenter.UserAccountStatus;
import org.teamapps.universaldb.index.text.TextFilter;
import org.teamapps.ux.session.SessionContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.function.Supplier;

public class UrlAuthenticationHandler implements AuthenticationHandler{

	private final Supplier<AuthenticationConfig> authenticationConfigSupplier;

	/**
	 * 	Encoding scheme:
	 * 		base64(login:timestamp:sha256Hex(login:pwd-hash:timestamp:secret))
	 *
	 * @param authenticationConfigSupplier
	 */
	public UrlAuthenticationHandler(Supplier<AuthenticationConfig> authenticationConfigSupplier) {
		this.authenticationConfigSupplier = authenticationConfigSupplier;
	}

	@Override
	public User authenticate(SessionContext context, Map<String, Object> clientParameters) {
		String token = (String) clientParameters.get("ATOK");
		AuthenticationConfig authenticationConfig = authenticationConfigSupplier.get();
		if (authenticationConfig.isEnableAutoLoginUrls()) {
			return authenticate(token, authenticationConfig);
		} else {
			return null;
		}
	}

	private User authenticate(String token, AuthenticationConfig authenticationConfig) {
		try {
			String data = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
			String[] parts = data.split(":");
			if (parts.length == 3) {
				String login = parts[0];
				long time = Long.parseLong(parts[1]);
				String hash = parts[2];
				User user = User.filter().login(TextFilter.textEqualsIgnoreCaseFilter(login)).executeExpectSingleton();
				if (user == null || user.getUserAccountStatus() == UserAccountStatus.INACTIVE || user.getLogin() == null || user.getPassword() == null) {
					return null;
				}
				if (validate(login, user.getPassword(), time, authenticationConfig.getAutoLoginSecret(), hash)) {
					long diff = Math.abs(System.currentTimeMillis() - time);
					if (diff < authenticationConfig.getAutoLoginUrlValidityInSeconds() * 1_000L){
						return user;
					}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	private static boolean validate(String login, String password, long timestamp, String secret, String hash) {
		String sha256Hex = DigestUtils.sha256Hex(login + ":" + password + ":" + timestamp + ":" + secret);
		return hash.equals(sha256Hex);
	}

	public static String createToken(String login, String password, long timestamp, String secret) {
		String sha256Hex = DigestUtils.sha256Hex(login + ":" + password + ":" + timestamp + ":" + secret);
		String token = login + ":" + timestamp + ":" + sha256Hex;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
	}

}
