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
package org.teamapps.application.server.ui.address;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Country;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.model.controlcenter.Address;
import org.teamapps.ux.component.field.NumberField;
import org.teamapps.ux.component.field.TextField;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.form.ResponsiveFormLayout;

public class AddressForm {

	/*
			https://github.com/google/libaddressinput/blob/master/common/src/main/java/com/google/i18n/addressinput/common/RegionDataConstants.java
			https://github.com/google/libaddressinput/wiki/AddressValidationMetadata

			N – Name
			O – Organisation
			A – Street Address Line(s)
			D – Dependent locality (may be an inner-city district or a suburb)
			C – City or Locality
			S – Administrative area such as a state, province, island etc
			Z – Zip or postal code

		address
				.addText("name") //N
				.addText("organisation") //O
				.addText("street") //A
				.addText("city") //C //City/Town/Village
				.addText("dependentLocality") //D
				.addText("state") //S  //State/Province/County
				.addText("postalCode") //Z //ZIP code/Postal code
				.addText("country")
				.addFloat("latitude")
				.addFloat("longitude")
			;
	 */


	private final ApplicationInstanceData applicationInstanceData;
	private final TwoWayBindableValue<Address> selectedAddress = TwoWayBindableValue.create();
	private boolean withName;
	private boolean withOrganization;
	private boolean withDependentLocality;
	private boolean withGeoCoordinates;
	private TextField nameField;
	private TextField organizationField;
	private TextField streetField;
	private TextField dependentLocalityField;
	private TextField postalCodeField;
	private TextField cityField;
	private TextField stateField;
	private ComboBox<Country> countryComboBox;
	private NumberField latitudeField;
	private NumberField longitudeField;

	public AddressForm(ApplicationInstanceData applicationInstanceData) {
		this.applicationInstanceData = applicationInstanceData;
		init();
	}

	private void init() {
		nameField = new TextField();
		organizationField = new TextField();
		streetField = new TextField();
		dependentLocalityField = new TextField();
		postalCodeField = new TextField();
		cityField = new TextField();
		stateField = new TextField();
		countryComboBox = Country.createComboBox(applicationInstanceData);
		latitudeField = new NumberField(7);
		longitudeField = new NumberField(7);
	}

	public void createAddressSection(ResponsiveFormLayout formLayout) {
		formLayout.addSection(ApplicationIcons.MAP_LOCATION, getLocalized(Dictionary.POSTAL_ADDRESS));
	}

	public void addFields(ResponsiveFormLayout formLayout) {
		if (withName) {
			formLayout.addLabelAndField(null, getLocalized(Dictionary.NAME), "name", nameField);
		}
		if (withOrganization) {
			formLayout.addLabelAndField(null, getLocalized(Dictionary.ORGANIZATION), "organization", organizationField);
		}
		formLayout.addLabelAndField(null, getLocalized(Dictionary.STREET), "street", streetField);
		if (withDependentLocality) {
			formLayout.addLabelAndField(null, getLocalized(Dictionary.DEPENDENT_LOCALITY), "dependentLocality", dependentLocalityField);
		}
		formLayout.addLabelAndField(null, getLocalized(Dictionary.POSTAL_CODE), "postalCode", postalCodeField);
		formLayout.addLabelAndField(null, getLocalized(Dictionary.CITY), "city", cityField);
		formLayout.addLabelAndField(null, getLocalized(Dictionary.STATE), "state", stateField);
		formLayout.addLabelAndField(null, getLocalized(Dictionary.COUNTRY), "country", countryComboBox);
		if (withGeoCoordinates) {
			formLayout.addLabelAndField(null, getLocalized(Dictionary.LATITUDE), "latitude", latitudeField);
			formLayout.addLabelAndField(null, getLocalized(Dictionary.LONGITUDE), "longitude", longitudeField);
		}
	}

	public void setAddress(Address address) {
		if (address == null) {
			address = Address.create();
		}
		selectedAddress.set(address);
		nameField.setValue(address.getName());
		organizationField.setValue(address.getOrganisation());
		streetField.setValue(address.getStreet());
		cityField.setValue(address.getCity());
		dependentLocalityField.setValue(address.getDependentLocality());
		stateField.setValue(address.getState());
		postalCodeField.setValue(address.getPostalCode());
		countryComboBox.setValue(Country.getCountryByIsoCode(address.getCountry()));
		latitudeField.setValue(address.getLatitude());
		longitudeField.setValue(address.getLongitude());
	}


	public boolean validateAddress() {
		if (countryComboBox.getValue() != null) {
			return true;
		} else {
			return false;
		}
	}

	public Address getAddress() {
		Address address = selectedAddress.get();
		address.setName(nameField.getValue());
		address.setOrganisation(organizationField.getValue());
		address.setStreet(streetField.getValue());
		address.setDependentLocality(dependentLocalityField.getValue());
		address.setPostalCode(postalCodeField.getValue());
		address.setCity(cityField.getValue());
		address.setState(stateField.getValue());
		address.setCountry(countryComboBox.getValue() != null ? countryComboBox.getValue().getIsoCode() : null);
		address.setLatitude(latitudeField.getValue() != null ? latitudeField.getValue().floatValue() : 0);
		address.setLongitude(longitudeField.getValue() != null ? longitudeField.getValue().floatValue() : 0);
		return address;
	}

	private String getLocalized(String key) {
		return applicationInstanceData.getLocalized(key);
	}

	public boolean isWithName() {
		return withName;
	}

	public void setWithName(boolean withName) {
		this.withName = withName;
	}

	public boolean isWithOrganization() {
		return withOrganization;
	}

	public void setWithOrganization(boolean withOrganization) {
		this.withOrganization = withOrganization;
	}

	public boolean isWithDependentLocality() {
		return withDependentLocality;
	}

	public void setWithDependentLocality(boolean withDependentLocality) {
		this.withDependentLocality = withDependentLocality;
	}

	public boolean isWithGeoCoordinates() {
		return withGeoCoordinates;
	}

	public void setWithGeoCoordinates(boolean withGeoCoordinates) {
		this.withGeoCoordinates = withGeoCoordinates;
	}

	public TextField getNameField() {
		return nameField;
	}

	public TextField getOrganizationField() {
		return organizationField;
	}

	public TextField getStreetField() {
		return streetField;
	}

	public TextField getDependentLocalityField() {
		return dependentLocalityField;
	}

	public TextField getPostalCodeField() {
		return postalCodeField;
	}

	public TextField getCityField() {
		return cityField;
	}

	public TextField getStateField() {
		return stateField;
	}

	public ComboBox<Country> getCountryComboBox() {
		return countryComboBox;
	}

	public NumberField getLatitudeField() {
		return latitudeField;
	}

	public NumberField getLongitudeField() {
		return longitudeField;
	}
}
