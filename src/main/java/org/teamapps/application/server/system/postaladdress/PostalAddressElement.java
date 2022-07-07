package org.teamapps.application.server.system.postaladdress;

public class PostalAddressElement {
	private final PostalAddressElementType type;
	private String name;
	private int row;
	private int column;
	private String prefix;
	private boolean upper;
	private boolean required;
	private String matchRegex;


	public PostalAddressElement(PostalAddressElementType type) {
		this.type = type;
	}

	public String getPrefixOrEmptyString() {
		return prefix != null ? prefix : "";
	}

	public PostalAddressElementType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public int getColumn() {
		return column;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public boolean isUpper() {
		return upper;
	}

	public void setUpper(boolean upper) {
		this.upper = upper;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public String getMatchRegex() {
		return matchRegex;
	}

	public void setMatchRegex(String matchRegex) {
		this.matchRegex = matchRegex;
	}
}
