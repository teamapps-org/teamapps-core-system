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
package org.teamapps.application.server.system.session;

import com.ibm.icu.util.ULocale;
import org.teamapps.application.api.user.SessionUser;
import org.teamapps.application.server.system.utils.ValueConverterUtils;
import org.teamapps.model.controlcenter.User;
import org.teamapps.universaldb.context.UserContext;
import org.teamapps.ux.session.SessionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SessionUserImpl implements SessionUser {

	private final User user;
	private final SessionContext context;
	private final List<String> rankedLanguages;

	public SessionUserImpl(User user, SessionContext context) {
		this.user = user;
		this.context = context;
		rankedLanguages = new ArrayList<>();
		init();
	}

	private void init() {
		List<String> languages = ValueConverterUtils.decompressToStringList(user.getLanguages());
		if (languages == null || languages.isEmpty()) {
			languages = new ArrayList<>();
			languages.add(context.getLocale().getLanguage());
		}
		rankedLanguages.addAll(languages);
	}

	@Override
	public int getId() {
		return user.getId();
	}

	@Override
	public String getFirstName() {
		return user.getFirstName();
	}

	@Override
	public String getLastName() {
		return user.getLastName();
	}

	@Override
	public String getName(boolean lastNameFirst) {
		if (lastNameFirst) {
			return user.getLastName() + ", " + user.getFirstName();
		} else {
			return user.getFirstName() + " " + user.getLastName();
		}
	}

	@Override
	public String getProfilePictureLink() {
		return null; //todo - link must be the same vor all users?
	}

	@Override
	public String getLargeProfilePictureLink() {
		return null; //todo
	}

	@Override
	public SessionContext getSessionContext() {
		return context;
	}

	@Override
	public ULocale getULocale() {
		return null;
	}

	@Override
	public Locale getLocale() {
		return context.getLocale();
	}

	@Override
	public List<String> getRankedLanguages() {
		return rankedLanguages;
	}

}
