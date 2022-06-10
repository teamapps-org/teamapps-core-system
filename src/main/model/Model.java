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

import org.teamapps.universaldb.schema.*;

import static org.teamapps.universaldb.schema.TableOption.*;

public class Model implements SchemaInfoProvider {


	@Override
	public Schema getSchema() {
		Schema schema = Schema.create("org.teamapps.model");
		schema.setSchemaName("ControlCenterSchema");
		Database db = schema.addDatabase("controlCenter");

		//system model:
		Table login = db.addTable("login", KEEP_DELETED);
		Table user = db.addTable("user", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);
		Table userAcceptedPolicy = db.addTable("userAcceptedPolicy", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);
		Table userAcceptedPolicyEntries = db.addTable("userAcceptedPolicyEntries", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);
		Table userAccessToken = db.addTable("userAccessToken", KEEP_DELETED, TRACK_CREATION);
		Table organizationUnit = db.addTable("organizationUnit", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table organizationUnitType = db.addTable("organizationUnitType", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table organizationField = db.addTable("organizationField", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table address = db.addTable("address", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);

		//views:
		Table organizationUnitView = db.addView("organizationUnitView", organizationUnit);
		Table organizationUnitTypeView = db.addView("organizationUnitTypeView", organizationUnitType);
		Table organizationFieldView = db.addView("organizationFieldView", organizationField);
		Table addressView = db.addView("addressView", address);

		Table role = db.addTable("role", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table userRoleAssignment = db.addTable("userRoleAssignment", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table rolePrivilegeAssignment = db.addTable("rolePrivilegeAssignment", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table roleApplicationRoleAssignment = db.addTable("roleApplicationRoleAssignment", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);


		Table systemSettings = db.addTable("systemSettings", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);
		Table systemLog = db.addTable("systemLog", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);
		Table application = db.addTable("application", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);
		Table applicationPerspective = db.addTable("applicationPerspective", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);
		Table applicationPrivilegeGroup = db.addTable("applicationPrivilegeGroup", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);
		Table applicationPrivilege = db.addTable("applicationPrivilege", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);
		Table applicationVersion = db.addTable("applicationVersion", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);
		Table applicationVersionData = db.addTable("applicationVersionData", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);

		Table managedApplication = db.addTable("managedApplication", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);
		Table managedApplicationPerspective = db.addTable("managedApplicationPerspective", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);

		Table managedApplicationGroup = db.addTable("managedApplicationGroup", TableOption.KEEP_DELETED, TableOption.TRACK_CREATION, TableOption.TRACK_MODIFICATION);

		Table localizationKey = db.addTable("localizationKey", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table localizationValue = db.addTable("localizationValue", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table localizationTopic = db.addTable("localizationTopic", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);

		//workspace model:
		Table group = db.addTable("group", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table groupMembershipDefinition = db.addTable("groupMembershipDefinition", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table groupFolder = db.addTable("groupFolder", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table userGroupMembership = db.addTable("userGroupMembership");
		Table applicationFolder = db.addTable("applicationFolder", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table message = db.addTable("message", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table appointment = db.addTable("appointment", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table appointmentSeries = db.addTable("appointmentSeries", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);

		Table chatChannel = db.addTable("chatChannel", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table chatMessage = db.addTable("chatMessage", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);

		//news board:
		Table newsBoardMessage = db.addTable("newsBoardMessage", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table newsBoardMessageImage = db.addTable("newsBoardMessageImage", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);
		Table newsBoardMessageTranslation = db.addTable("newsBoardMessageTranslation", KEEP_DELETED, TRACK_CREATION, TRACK_MODIFICATION);

		systemSettings
				.addText("allowedBaseLanguages")
		;

		application
				.addText("name")
				.addText("icon")
				.addText("titleKey")
				.addText("descriptionKey")
				.addBoolean("unmanagedApplication")
				.addText("config")
				.addBoolean("darkTheme")
				.addReference("perspectives", applicationPerspective, true, "application", true)
				.addReference("privilegeGroups", applicationPrivilegeGroup, true, "application")
				.addReference("versions", applicationVersion, true, "application")
				.addReference("installedVersion", applicationVersion, false)
				.addReference("installedAsMainApplication", managedApplication, true, "mainApplication")
		;

		applicationPerspective
				.addReference("application", application, false, "perspectives")
				.addText("name")
				.addText("icon")
				.addText("titleKey")
				.addText("descriptionKey")
				.addBoolean("autoProvision")
				.addBoolean("toolbarPerspectiveMenu")
				.addReference("managedPerspectives", managedApplicationPerspective, true, "applicationPerspective", true)
		;

		applicationPrivilegeGroup
				.addReference("application", application, false, "privilegeGroups")
				.addEnum("applicationPrivilegeGroupType", "simplePrivilege", "simpleOrganizationalPrivilege", "simpleCustomObjectPrivilege", "standardPrivilegeGroup", "organizationalPrivilegeGroup", "customObjectPrivilegeGroup", "roleAssignmentDelegatedCustomPrivilegeGroup")
				.addText("name")
				.addText("icon")
				.addText("titleKey")
				.addText("descriptionKey")
				.addReference("privileges", applicationPrivilege, true, "privilegeGroup")
		;

		applicationPrivilege
				.addReference("privilegeGroup", applicationPrivilegeGroup, false, "privileges")
				.addText("name")
				.addText("icon")
				.addText("titleKey")
		;

		applicationVersion
				.addReference("application", application, false, "versions")
				.addFile("binary")
				.addText("binaryHash")
				.addText("version")
				.addText("releaseNotes")
				.addReference("dataModelData", applicationVersionData, false)
				.addReference("localizationData", applicationVersionData, false)
				.addReference("privilegeData", applicationVersionData, false)
				.addReference("perspectiveData", applicationVersionData, false)
		;

		applicationVersionData
				.addText("data")
				.addInteger("dataRows")
				.addText("dataAdded")
				.addInteger("dataAddedRows")
				.addText("dataRemoved")
				.addInteger("dataRemovedRows")
		;

		systemLog
				.addReference("managedApplication", managedApplication, false)
				.addReference("managedPerspective", managedApplicationPerspective, false)
				.addReference("application", application, false)
				.addReference("applicationPerspective", applicationPerspective, false)
				.addReference("applicationVersion", applicationVersion, false)
				.addEnum("logLevel", "info", "warning", "error")
				.addText("thread")
				.addText("message")
				.addText("details")
				.addText("exceptionClass")
		;

		managedApplication
				.addReference("mainApplication", application, false, "installedAsMainApplication")
				.addReference("organizationField", organizationField, false)
				.addText("icon")
				.addText("titleKey")
				.addText("descriptionKey")
				.addBoolean("singleApplication")
				.addBoolean("darkTheme")
				.addBoolean("startOnLogin")
				.addReference("perspectives", managedApplicationPerspective, true, "managedApplication", true)
				.addInteger("listingPosition")
				.addBoolean("toolbarApplicationMenu")
				.addReference("applicationGroup", managedApplicationGroup, false, "applications")
				.addBoolean("hidden")
		;

		managedApplicationPerspective
				.addReference("managedApplication", managedApplication, false, "perspectives")
				.addReference("applicationPerspective", applicationPerspective, false, "managedPerspectives")
				.addText("iconOverride")
				.addText("titleKeyOverride")
				.addText("descriptionKeyOverride")
				.addBoolean("toolbarPerspectiveMenu")
				.addInteger("listingPosition")
		;

		managedApplicationGroup
				.addText("icon")
				.addText("titleKey") //.addTranslatableText("title")
				.addReference("applications", managedApplication, true, "applicationGroup")
				.addInteger("listingPosition")
		;

		localizationKey
				.addReference("application", application, false)
				.addText("key")
				.addBoolean("used")
				.addEnum("localizationKeyFormat", "singleLine", "multiLine", "formatted")
				.addEnum("localizationKeyType", "applicationResourceKey", "dictionaryKey", "reportingKey", "systemKey")
				.addText("comments")
				.addReference("localizationValues", localizationValue, true, "localizationKey")
				.addReference("topics", localizationTopic, true, "keys")
		;

		localizationValue
				.addReference("localizationKey", localizationKey, false, "localizationValues")
				.addText("language")
				.addText("original")
				.addText("machineTranslation")
				.addText("translation")
				.addText("adminLocalOverride") //override only this translation
				.addText("adminKeyOverride") //override all translations
				.addText("currentDisplayValue")
				.addText("notes")
				.addEnum("machineTranslationState", "translationRequested", "ok", "failed", "notNecessary")
				.addEnum("translationState", "translationRequested", "ok", "unclear", "notNecessary")
				.addEnum("translationVerificationState", "notYetTranslated", "verificationRequested", "ok", "correctionsRequired", "notNecessary")
		;

		localizationTopic
				.addReference("keys", localizationKey, true, "topics")
				.addReference("application", application, false)
				.addText("icon")
				.addText("title")
				.addFile("screenshot")
		;

		login
				.addReference("user", user, false)
				.addText("ip")
				.addText("userAgent")
				.addBoolean("mobileDevice")
				.addText("screenSize")
				.addTimestamp("dateLogin")
				.addTimestamp("dateLogout")
				.addInteger("activityCount")
				.addInteger("applicationOpenCount")
		;

		user
				.addText("firstName")
				.addText("lastName")
				.addBinary("profilePicture")
				.addBinary("profilePictureLarge")
				.addText("languages")
				.addText("email")
				.addText("mobile")
				.addText("phone")
				.addText("login")
				.addTimestamp("lastLogin")
				.addText("password")
				.addText("theme")
				.addEnum("userAccountStatus", "active", "inactive", "superAdmin")
				.addReference("acceptedPolicies", userAcceptedPolicy, false)
				.addReference("address", address, false)
				.addReference("organizationUnit", organizationUnit, false, "users")
				.addReference("accessTokens", userAccessToken, true, "user", true)
				.addReference("roleAssignments", userRoleAssignment, true, "user", true)
				.addReference("allGroupMemberships", userGroupMembership, true, "user", true)
				.addReference("privateMessages", message, true, "privateRecipients") //private message entry instead!?
		;

		userAcceptedPolicy
				.addInteger("lastAcceptedPrivacyPolicy")
				.addInteger("lastAcceptedTermsOfUse")
				.addReference("acceptEntries", userAcceptedPolicyEntries, true)
		;

		userAcceptedPolicyEntries
				.addInteger("acceptedPrivacyPolicy")
				.addInteger("acceptedTermsOfUse")
		;

		userAccessToken
				.addReference("user", user, false, "accessTokens")
				.addText("userAgentOnCreation")
				.addText("userAgentLastUsed")
				.addTimestamp("lastUsed")
				.addBoolean("valid")
				.addBoolean("restApi")
				.addText("secureToken")
		;

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

		 */

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

		addressView
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

		organizationUnit
				.addTranslatableText("name")
				.addReference("parent", organizationUnit, false, "children")
				.addReference("children", organizationUnit, true, "parent")
				.addReference("type", organizationUnitType, false)
				.addText("icon")
				.addReference("address", address, false)
				.addReference("users", user, true, "organizationUnit")
		;

		organizationUnitView
				.addTranslatableText("name")
				.addReference("parent", organizationUnitView, false, "children")
				.addReference("children", organizationUnitView, true, "parent")
				.addReference("type", organizationUnitTypeView, false)
				.addText("icon")
				.addReference("address", addressView, false)
		;


		organizationUnitType
				.addTranslatableText("name")
				.addTranslatableText("abbreviation")
				.addText("icon")
				.addBoolean("translateOrganizationUnits")
				.addBoolean("allowUsers")
				.addReference("defaultChildType", organizationUnitType, false)
				.addReference("possibleChildrenTypes", organizationUnitType, true)
				.addEnum("geoLocationType", "country", "state", "city", "place", "none")
		;

		organizationUnitTypeView
				.addTranslatableText("name")
				.addTranslatableText("abbreviation")
				.addText("icon")
				.addBoolean("translateOrganizationUnits")
				.addBoolean("allowUserContainer")
				.addReference("defaultChildType", organizationUnitTypeView, false)
				.addReference("possibleChildrenTypes", organizationUnitTypeView, true)
				.addEnum("geoLocationType", "country", "state", "city", "place", "none")
		;

		organizationField
				.addTranslatableText("title")
				.addText("icon")
		;

		organizationFieldView
				.addTranslatableText("title")
				.addText("icon")
		;

		role
				.addTranslatableText("title")
				.addText("icon")
				.addEnum("roleType", "leader", "assistant", "mentor", "administrator", "other")
				.addReference("parent", role, false, "children")
				.addReference("children", role, true, "parent")
				.addReference("organizationUnit", organizationUnit, false)
				.addReference("allowedOrganizationUnitTypes", organizationUnitType, true)
				.addReference("organizationField", organizationField, false)
				.addBoolean("delegatedCustomPrivilegeObjectRole")
				.addReference("generalizationRoles", role, true, "specializationRoles")
				.addReference("specializationRoles", role, true, "generalizationRoles")
				.addReference("privilegesReceivingRoles", role, true, "privilegesSendingRoles")
				.addReference("privilegesSendingRoles", role, true, "privilegesReceivingRoles")
				.addBoolean("noDirectMemberships")
				.addReference("userRoleAssignments", userRoleAssignment, true, "role", true)
				.addReference("privilegeAssignments", rolePrivilegeAssignment, true, "role", true)
				.addReference("applicationRoleAssignments", roleApplicationRoleAssignment, true, "role", true)
		;

		userRoleAssignment
				.addReference("user", user, false, "roleAssignments")
				.addReference("role", role, false, "userRoleAssignments")
				.addReference("organizationUnit", organizationUnit, false)
				.addInteger("delegatedCustomPrivilegeObjectId")
				.addBoolean("mainResponsible")
				.addTimestamp("lastVerified")
				.addReference("lastVerifiedBy", user, false)
		;

		rolePrivilegeAssignment
				.addReference("role", role, false, "privilegeAssignments")
				.addReference("application", application, false)
				.addReference("privilegeGroup", applicationPrivilegeGroup, false)
				.addReference("privileges", applicationPrivilege, true)
				.addText("privilegeObjects")
				.addBoolean("privilegeObjectInheritance")
				.addReference("organizationFieldFilter", organizationField, false)
				.addReference("fixedOrganizationRoot", organizationUnit, false)
				.addReference("organizationUnitTypeFilter", organizationUnitType, true)
				.addBoolean("noInheritanceOfOrganizationalUnits")
		;

		roleApplicationRoleAssignment
				.addReference("role", role, false, "applicationRoleAssignments")
				.addReference("application", application, false)
				.addText("applicationRoleName")
				.addReference("organizationFieldFilter", organizationField, false)
				.addReference("fixedOrganizationRoot", organizationUnit, false)
				.addReference("organizationUnitTypeFilter", organizationUnitType, true)
				.addBoolean("noInheritanceOfOrganizationalUnits")
		;


		group
				.addEnum("groupType", "messageGroup", "workGroup")
				.addText("name") //addTranslatableText
				.addText("description") //addTranslatableText
				.addText("language")
				.addReference("organizationField", organizationField, false)
				.addReference("organizationUnit", organizationUnit, false)
				.addReference("owner", user, false)
				.addReference("moderators", user, true)
				.addReference("mentors", user, true)
				.addBoolean("valid")
				.addReference("membershipDefinitions", groupMembershipDefinition, true, "group")
				.addReference("registrationAllowedForDefinitions", groupMembershipDefinition, true)
				.addReference("userMemberships", userGroupMembership, true, "group")
				.addTimestamp("lastMessageDate")
				.addReference("groupFolders", groupFolder, true, "group")
		;

		groupMembershipDefinition
				.addEnum("groupMemberType", "user", "group", "roleMember", "userContainer")
				.addReference("user", user, false)
				.addReference("group", group, false)
				.addReference("role", role, false)
				.addReference("organizationUnit", organizationUnit, false)
				.addReference("organizationUnitTypesFilter", organizationUnitType, true)
		;

		groupFolder
				.addReference("group", group, false, "groupFolders")
				.addText("name")
				.addBoolean("moderatorReadAccess")
				.addBoolean("moderatorWriteAccess")
				.addText("subjectTags") //message/chat subjects, file tags,
				.addEnum("groupFolderType", "chat", "messages", "files", "calendar")
				.addReference("messages", message, true, "groupFolder")
			/*
				Automatic folders:
					-Conference recordings (if calendar folder exists)
					-All files (from chats, messages, file folders)
			 */
		;

		applicationFolder
				.addText("name")
				.addReference("application", application, false)
				.addEnum("applicationFolderType", "chat", "messages", "files", "calendar")
				.addReference("parent", applicationFolder, false, "children")
				.addReference("children", applicationFolder, true, "parent")
				.addReference("messages", message, true, "applicationFolder")

		;


		userGroupMembership
				.addReference("group", group, false, "userMemberships")
				.addReference("user", user, false, "allGroupMemberships")
				.addEnum("groupMembershipRole", "participant", "moderator", "mentor", "owner")
				.addBoolean("membershipRejected")
				.addEnum("groupMembershipNotificationSetting", "standard", "silent");


		message
				.addEnum("messageRecipientType", "private", "groupFolder", "applicationFolder") //todo message group as recipient?
				.addReference("privateRecipients", user, true, "privateMessages")
				.addReference("groupFolder", groupFolder, false, "messages")
				.addReference("applicationFolder", applicationFolder, false, "messages")
				.addText("subject") //todo translatable text?
				.addText("message") //todo translatable text?
				.addEnum("messageType", "message", "chatMessage", "appointment", "appointmentSeries", "file")
				.addFile("file")
				.addReference("appointment", appointment, false, "message")
				.addReference("appointmentSeries", appointmentSeries, false, "message")
		;

		appointment
				.addReference("message", message, false, "appointment")
				.addReference("appointmentSeries", appointmentSeries, false, "appointments")
				.addBoolean("webConference") //conference object -> recording
				.addDateTime("startDateTime")
				.addDateTime("endDateTime")
				.addBoolean("fullDayEvent")
		;

		appointmentSeries
				.addReference("message", message, false, "appointmentSeries")
				.addReference("appointments", appointment, true, "appointmentSeries")
				.addEnum("appointmentSeriesType", "daily", "weekly", "monthly", "yearly")
				.addInteger("repeatEvery")
				.addDateTime("startDateTime")
				.addDateTime("endDateTime")
				.addBoolean("fullDayEvent")
				.addLocalDate("seriesEndDate");


		//news board
		newsBoardMessage
				.addBoolean("published")
				.addText("htmlMessage")
				.addText("language")
				.addReference("images", newsBoardMessageImage, true, true)
				.addReference("translations", newsBoardMessageTranslation, true, true)
				.addReference("organizationField", organizationField, false)
				.addReference("organizationUnit", organizationUnit, false)
		;

		newsBoardMessageImage
				.addFile("file")
				.addFile("thumbnail")
				.addText("fileName")
				.addBoolean("embedded")
				.addInteger("position")
		;

		newsBoardMessageTranslation
				.addReference("message", newsBoardMessage, false, "translations")
				.addText("language")
				.addText("translation")
		;

		//chat server
		chatChannel
				.addText("title")
				.addReference("chatMessages", chatMessage, true, "chatChannel")
		;

		chatMessage
				.addReference("chatChannel", chatChannel, false, "chatMessages")
				.addReference("author", user, false)
				.addText("message")
		;


		return schema;
	}
}
