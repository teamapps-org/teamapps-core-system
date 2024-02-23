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
package org.teamapps.application.server.system.bootstrap;

import org.teamapps.application.server.SecureLinkBuilder;
import org.teamapps.application.server.SecureResourceHandler;
import org.teamapps.model.controlcenter.User;

public class BaseResourceLinkProvider {


	private final SecureResourceHandler secureResourceHandler;
	private final SecureLinkBuilder userStandardProfilePictureLinkBuilder;
	private final SecureLinkBuilder userLargeProfilePictureLinkBuilder;

	public BaseResourceLinkProvider() {
		secureResourceHandler = SecureResourceHandler.getInstance();
		userStandardProfilePictureLinkBuilder = secureResourceHandler.registerByteArrayResourceHandler(id -> User.getById(id).getProfilePicture(), id -> User.getById(id).getMetaModificationDateAsEpochMilli(), "jpg");
		userLargeProfilePictureLinkBuilder = secureResourceHandler.registerByteArrayResourceHandler(id -> User.getById(id).getProfilePictureLarge(), id -> User.getById(id).getMetaModificationDateAsEpochMilli(),"jpg");
	}

	public String getUserProfilePictureLink(int userId, boolean large) {
		if (large) {
			String link = userLargeProfilePictureLinkBuilder.createLink(userId);
			if (link != null) {
				return link;
			}
		}
		return userStandardProfilePictureLinkBuilder.createLink(userId);
	}

	public String getUserProfilePictureLink(User user) {
		return userStandardProfilePictureLinkBuilder.createLink(user.getId());
	}

	public String getUserLargeProfilePictureLink(User user) {
		return userLargeProfilePictureLinkBuilder.createLink(user.getId());
	}

}
