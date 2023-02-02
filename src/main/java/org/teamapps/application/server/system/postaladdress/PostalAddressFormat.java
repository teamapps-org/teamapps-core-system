/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
 * ---
 * Copyright (C) 2020 - 2023 TeamApps.org
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
package org.teamapps.application.server.system.postaladdress;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PostalAddressFormat {

	private final String country;
	private List<PostalAddressElement> elements = new ArrayList<>();

	public PostalAddressFormat(String country) {
		this.country = country;
	}

	public void addElement(PostalAddressElement element) {
		elements.add(element);
	}

	public String getCountry() {
		return country;
	}

	public List<PostalAddressElement> getElements() {
		return elements;
	}

	public void sortEntries() {
		elements = elements.stream().sorted((o1, o2) -> {
			if (o1.getRow() != o2.getRow()) {
				return Integer.compare(o1.getRow(), o2.getRow());
			} else {
				return Integer.compare(o1.getColumn(), o2.getColumn());
			}
		}).collect(Collectors.toList());
	}

	public int maxDuplicateTypes() {
		return elements.stream()
				.collect(Collectors.groupingBy(PostalAddressElement::getType))
				.values().stream()
				.mapToInt(List::size)
				.max().orElse(0);
	}

	public Set<PostalAddressElementType> getTypes() {
		return elements.stream()
				.map(PostalAddressElement::getType)
				.collect(Collectors.toSet());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(country).append("\n");
		sortEntries();
		int row = 0;
		for (PostalAddressElement element : elements) {
			if (element.getRow() > row) {
				sb.append("\n");
				row = element.getRow();
			}
			String name = element.getPrefixOrEmptyString() + (element.isUpper() ? element.getName().toUpperCase() : element.getName()) + (element.isRequired() ? "*" : "");
			sb.append(name);
		}
		return sb.toString();
	}
}
