/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
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
import org.teamapps.application.api.user.LocalizedFormatter;
import org.teamapps.application.api.user.SessionUser;
import org.teamapps.application.server.system.utils.ValueConverterUtils;
import org.teamapps.event.Event;
import org.teamapps.model.controlcenter.User;
import org.teamapps.universaldb.context.UserContext;
import org.teamapps.ux.session.SessionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SessionUserImpl implements SessionUser {

	private final User user;
	private final SessionContext context;
	private final List<String> rankedLanguages;
	private final UserSessionData userSessionData;
	private final Locale locale;
	private final LocalizedFormatter localizedFormatter;
	private final Event<Void> onLogout = new Event<>();

	public SessionUserImpl(UserSessionData userSessionData) {
		this.userSessionData = userSessionData;
		this.user = userSessionData.getUser();
		this.context = userSessionData.getContext();
		rankedLanguages = new ArrayList<>();
		List<String> languages = new ArrayList<>();
		if (user.getLanguage() != null) {
			languages.add(user.getLanguage());
		}
		user.getLanguageSettings().stream()
				.filter(settings -> settings.getLanguage() != null && settings.getLanguageSkillLevel() != null)
				.sorted(Comparator.comparingInt(o -> o.getLanguageSkillLevel().ordinal()))
				.forEach(settings -> rankedLanguages.add(settings.getLanguage()));
		if (languages.isEmpty()) {
			languages.add(context.getLocale().getLanguage());
		}
		locale = Locale.forLanguageTag(languages.get(0));
		rankedLanguages.addAll(languages.stream().distinct().collect(Collectors.toList()));
		localizedFormatter = new LocalizedFormatter(locale, context.getTimeZone());
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
	public String getFirstNameTranslated() {
		return user.getFirstNameTranslated();
	}

	@Override
	public String getLastName() {
		return user.getLastName();
	}

	@Override
	public String getLastNameTranslated() {
		return user.getLastNameTranslated();
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
	public String getDescription() {
		String description = "";
		if (user.getAddress() != null) {
			description = user.getAddress().getCountry() + " ";
		}
		if (user.getOrganizationUnit() != null) {
			description += user.getOrganizationUnit().getName().getText();
		}
		return description;
	}

	@Override
	public String getProfilePictureLink() {
		return userSessionData.getRegistry().getBaseResourceLinkProvider().getUserProfilePictureLink(user);
	}

	@Override
	public String getLargeProfilePictureLink() {
		return userSessionData.getRegistry().getBaseResourceLinkProvider().getUserProfilePictureLink(user.getId(), true);
	}

	@Override
	public SessionContext getSessionContext() {
		return context;
	}

	@Override
	public ULocale getULocale() {
		return ULocale.forLocale(getLocale());
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public List<String> getRankedLanguages() {
		return rankedLanguages;
	}

	@Override
	public boolean isDarkTheme() {
		return userSessionData.isDarkTheme();
	}

	@Override
	public LocalizedFormatter getLocalizedFormatter() {
		return localizedFormatter;
	}

	@Override
	public Event<Void> onUserLogout() {
		return onLogout;
	}

}
