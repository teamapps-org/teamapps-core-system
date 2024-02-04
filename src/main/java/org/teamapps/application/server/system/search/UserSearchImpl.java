package org.teamapps.application.server.system.search;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.search.SearchEntry;
import org.teamapps.application.api.search.UserMatch;
import org.teamapps.application.api.search.UserSearch;
import org.teamapps.application.api.search.UserSearchBuilder;
import org.teamapps.model.controlcenter.Address;
import org.teamapps.model.controlcenter.AddressQuery;
import org.teamapps.model.controlcenter.User;
import org.teamapps.model.controlcenter.UserQuery;
import org.teamapps.universaldb.index.text.TextFilter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserSearchImpl implements UserSearch {

	private final String authCode;
	private final ApplicationInstanceData applicationInstanceData;

	public UserSearchImpl(String authCode, ApplicationInstanceData applicationInstanceData) {
		this.authCode = authCode;
		this.applicationInstanceData = applicationInstanceData;
	}

	@Override
	public List<UserMatch> search(UserSearchBuilder searchBuilder, int minScore) {
		if (searchBuilder.getMaxScore() == 0) {
			return Collections.emptyList();
		}
		UserQuery query = User.filter();
		Map<String, SearchEntry> searchMap = searchBuilder.getSearchMap();
		TextFilter firstNameFilter = createTextFilter(searchMap.get(UserSearchBuilder.FIRST_NAME));
		if (firstNameFilter != null) {
			query.firstName(firstNameFilter);
		}
		TextFilter lastNameFilter = createTextFilter(searchMap.get(UserSearchBuilder.LAST_NAME));
		if (lastNameFilter != null) {
			query.lastName(lastNameFilter);
		}
		TextFilter emailFilter = createTextFilter(searchMap.get(UserSearchBuilder.E_MAIL));
		if (emailFilter != null) {
			query.email(emailFilter);
		}
		TextFilter phoneFilter = createTextFilter(searchMap.get(UserSearchBuilder.PHONE));
		if (phoneFilter != null) {
			query.phone(phoneFilter);
		}
		if (searchBuilder.containsAddressFilter()) {
			AddressQuery addressQuery = Address.filter();
			TextFilter streetFilter = createTextFilter(searchMap.get(UserSearchBuilder.STREET));
			if (streetFilter != null) {
				addressQuery.street(streetFilter);
			}
			TextFilter postalCodeFilter = createTextFilter(searchMap.get(UserSearchBuilder.POSTAL_CODE));
			if (postalCodeFilter != null) {
				addressQuery.postalCode(postalCodeFilter);
			}
			TextFilter cityFilter = createTextFilter(searchMap.get(UserSearchBuilder.CITY));
			if (cityFilter != null) {
				addressQuery.city(cityFilter);
			}
			TextFilter countryFilter = createTextFilter(searchMap.get(UserSearchBuilder.COUNTRY_CODE));
			if (countryFilter != null) {
				addressQuery.country(countryFilter);
			}
			query.filterAddress(addressQuery);
		}
		List<User> users = query.execute();
		return convert(users, searchBuilder).stream().filter(u -> u.matchScore() >= minScore).toList();
	}

	private static TextFilter createTextFilter(SearchEntry searchEntry) {
		if (searchEntry == null) return null;
		return switch (searchEntry.getSearchType()) {
			case EXACT_PHRASE -> TextFilter.textEqualsFilter(searchEntry.getValue());
			case EXACT_TERMS -> TextFilter.termEqualsFilter(searchEntry.getValue());
			case CONTAINS_TERMS -> TextFilter.termContainsFilter(searchEntry.getValue());
			case SIMILAR_TERMS -> TextFilter.termSimilarFilter(searchEntry.getValue());
			case OPTIONAL -> null;
		};
	}

	private static List<UserMatch> convert(List<User> users, UserSearchBuilder searchBuilder) {
		if (users == null || users.isEmpty()) {
			return Collections.emptyList();
		} else {
			return users.stream()
					.map(u -> convert(u, searchBuilder))
					.sorted(Comparator.comparingInt(UserMatch::matchScore).reversed())
					.toList();
		}
	}

	private static UserMatch convert(User user) {
		return convert(user, null);
	}

	private static UserMatch convert(User user, UserSearchBuilder searchBuilder) {
		Address address = user.getAddress() == null ? Address.create() : user.getAddress();
		int score = 0;
		if (searchBuilder != null) {
			score += searchBuilder.getScore(UserSearchBuilder.FIRST_NAME, user.getFirstName());
			score += searchBuilder.getScore(UserSearchBuilder.LAST_NAME, user.getLastName());
			score += searchBuilder.getScore(UserSearchBuilder.E_MAIL, user.getEmail());
			score += searchBuilder.getScore(UserSearchBuilder.PHONE, user.getPhone());

			score += searchBuilder.getScore(UserSearchBuilder.STREET, address.getStreet());
			score += searchBuilder.getScore(UserSearchBuilder.POSTAL_CODE, address.getPostalCode());
			score += searchBuilder.getScore(UserSearchBuilder.CITY, address.getCity());
			score += searchBuilder.getScore(UserSearchBuilder.COUNTRY_CODE, address.getCountry());
		}
		return new UserMatch(user.getId(), null, user.getFirstName(), user.getLastName(), address.getStreet(), address.getPostalCode(), address.getCity(), address.getCountry(), user.getPhone(), user.getEmail(), score);
	}

	@Override
	public UserMatch getUser(int userId) {
		User user = User.getById(userId);
		return user.isStored() ? convert(user) : null;
	}

	@Override
	public String getUserTasks(int userId) {
		User user = User.getById(userId);
		if (!user.isStored()) {
			return null;
		}
		return user.getRoleAssignments().stream()
				.map(r -> applicationInstanceData.getLocalized(r.getRole().getTitle())).distinct()
				.collect(Collectors.joining(", "));
	}
}
