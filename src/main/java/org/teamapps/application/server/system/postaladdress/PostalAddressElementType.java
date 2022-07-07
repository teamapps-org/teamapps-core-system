package org.teamapps.application.server.system.postaladdress;

public enum PostalAddressElementType {

	FIRST_NAME("FN", "Firstname"),
	LAST_NAME("N", "Lastname"),
	ORGANIZATION("O", "Organisation"),
	ADDRESS("A", "Street"),
	DEPENDENT_LOCALITY("D", "Dependent locality"),
	CITY("C", "City"),
	STATE("S", "State"),
	ZIP("Z", "Zip"),
	SORTING_CODE("X", "Sorting code"),
	;

	private final String key;
	private final String name;

	PostalAddressElementType(String key, String name) {
		this.key = key;
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public static PostalAddressElementType findType(String s) {
		for (PostalAddressElementType value : values()) {
			if (value.getKey().equals(s)) {
				return value;
			}
		}
		return null;
	}

}
