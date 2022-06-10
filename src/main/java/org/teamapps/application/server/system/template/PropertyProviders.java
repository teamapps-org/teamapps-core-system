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
package org.teamapps.application.server.system.template;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.privilege.ApplicationRole;
import org.teamapps.application.api.privilege.Privilege;
import org.teamapps.application.api.privilege.PrivilegeGroup;
import org.teamapps.application.api.privilege.PrivilegeObject;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.application.ux.localize.TranslatableTextUtils;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.icons.Icon;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.translation.TranslatableText;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.session.SessionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class PropertyProviders {

	public static PropertyProvider<Application> createApplicationPropertyProvider(UserSessionData userSessionData) {
		return (application, propertyNames) -> {
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(application);
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, userSessionData.decodeIcon(application.getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, localizationProvider.getLocalized(application.getTitleKey()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, localizationProvider.getLocalized(application.getDescriptionKey()));
			return map;
		};
	}

	public static PropertyProvider<ApplicationPerspective> createApplicationPerspectivePropertyProvider(UserSessionData userSessionData) {
		return (applicationPerspective, propertyNames) -> {
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(applicationPerspective.getApplication());
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, userSessionData.decodeIcon(applicationPerspective.getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, localizationProvider.getLocalized(applicationPerspective.getTitleKey()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, localizationProvider.getLocalized(applicationPerspective.getDescriptionKey()));
			return map;
		};
	}

	public static PropertyProvider<ApplicationPrivilege> createApplicationPrivilegePropertyProvider(UserSessionData userSessionData) {
		return (applicationPrivilege, propertyNames) -> {
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(applicationPrivilege.getPrivilegeGroup().getApplication());
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, userSessionData.decodeIcon(applicationPrivilege.getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, localizationProvider.getLocalized(applicationPrivilege.getTitleKey()));
			return map;
		};
	}

	public static PropertyProvider<ApplicationPrivilegeGroup> createApplicationPrivilegeGroupPropertyProvider(UserSessionData userSessionData) {
		return (applicationPrivilegeGroup, propertyNames) -> {
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(applicationPrivilegeGroup.getApplication());
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, userSessionData.decodeIcon(applicationPrivilegeGroup.getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, localizationProvider.getLocalized(applicationPrivilegeGroup.getTitleKey()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, localizationProvider.getLocalized(applicationPrivilegeGroup.getDescriptionKey()));
			return map;
		};
	}

	public static PropertyProvider<PrivilegeGroup> createPrivilegeGroupPropertyProvider(UserSessionData userSessionData, Supplier<Application> applicationSupplier) {
		return (privilegeGroup, propertyNames) -> {
			Application application = applicationSupplier.get();
			if (application == null) {
				return new HashMap<>();
			}
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(application);
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, privilegeGroup.getIcon());
			map.put(BaseTemplate.PROPERTY_CAPTION, localizationProvider.getLocalized(privilegeGroup.getTitleKey()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, localizationProvider.getLocalized(privilegeGroup.getDescriptionKey()));
			return map;
		};
	}

	public static PropertyProvider<Privilege> createPrivilegePropertyProvider(UserSessionData userSessionData, Supplier<Application> applicationSupplier) {
		return (privilege, propertyNames) -> {
			Application application = applicationSupplier.get();
			if (application == null) {
				return new HashMap<>();
			}
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(application);
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, privilege.getIcon());
			map.put(BaseTemplate.PROPERTY_CAPTION, localizationProvider.getLocalized(privilege.getTitleKey()));
			return map;
		};
	}

	public static PropertyProvider<PrivilegeObject> createPrivilegeObjectPropertyProvider() {
		return (privilegeObject, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, privilegeObject.getIcon());
			map.put(BaseTemplate.PROPERTY_CAPTION, privilegeObject.getTitleKey());
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, privilegeObject.getDescriptionKey());
			return map;
		};
	}

	public static PropertyProvider<ApplicationVersion> createSimpleApplicationVersionPropertyProvider() {
		return (version, propertyNames) -> {
			SessionContext context = SessionContext.current();
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, context.getIconProvider().decodeIcon(version.getApplication().getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, version.getVersion());
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, version.getReleaseNotes());
			return map;
		};
	}

	public static PropertyProvider<ApplicationVersion> createApplicationVersionPropertyProvider(UserSessionData userSessionData) {
		return (version, propertyNames) -> {
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(version.getApplication());
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, userSessionData.decodeIcon(version.getApplication().getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, localizationProvider.getLocalized(version.getApplication().getTitleKey()) + ": " + version.getVersion());
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, version.getReleaseNotes());
			return map;
		};
	}

	public static PropertyProvider<ApplicationRole> createApplicationRolePropertyProvider(UserSessionData userSessionData, Application application) {
		return (applicationRole, propertyNames) -> {
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(application);
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, applicationRole.getIcon());
			map.put(BaseTemplate.PROPERTY_CAPTION, localizationProvider.getLocalized(applicationRole.getTitleKey()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, localizationProvider.getLocalized(applicationRole.getDescriptionKey()));
			return map;
		};
	}

	public static PropertyProvider<ApplicationRole> createApplicationRolePropertyProvider(ApplicationInstanceData applicationInstanceData) {
		return (applicationRole, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, applicationRole.getIcon());
			map.put(BaseTemplate.PROPERTY_CAPTION, applicationInstanceData.getLocalized(applicationRole.getTitleKey()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, applicationInstanceData.getLocalized(applicationRole.getDescriptionKey()));
			return map;
		};
	}

	public static PropertyProvider<ManagedApplication> createManagedApplicationPropertyProvider(UserSessionData userSessionData) {
		return (managedApplication, propertyNames) -> {
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(managedApplication.getMainApplication());
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, managedApplication.getIcon() != null ? userSessionData.decodeIcon(managedApplication.getIcon()) : userSessionData.decodeIcon(managedApplication.getMainApplication().getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, managedApplication.getTitleKey() != null ? localizationProvider.getLocalized(managedApplication.getTitleKey()) : localizationProvider.getLocalized(managedApplication.getMainApplication().getTitleKey()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, managedApplication.getDescriptionKey() != null ? localizationProvider.getLocalized(managedApplication.getDescriptionKey()) : localizationProvider.getLocalized(managedApplication.getMainApplication().getDescriptionKey()));
			map.put(BaseTemplate.PROPERTY_BADGE, managedApplication.getHidden() ? localizationProvider.getLocalized(Dictionary.HIDDEN) : null);
			return map;
		};
	}

	public static PropertyProvider<ManagedApplicationPerspective> createManagedApplicationPerspectivePropertyProvider(UserSessionData userSessionData) {
		return (managedApplicationPerspective, propertyNames) -> {
			ApplicationLocalizationProvider localizationProvider = userSessionData.getApplicationLocalizationProvider(managedApplicationPerspective.getApplicationPerspective().getApplication());
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, managedApplicationPerspective.getIconOverride() != null ? userSessionData.decodeIcon(managedApplicationPerspective.getIconOverride()) : userSessionData.decodeIcon(managedApplicationPerspective.getApplicationPerspective().getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, managedApplicationPerspective.getTitleKeyOverride() != null ? localizationProvider.getLocalized(managedApplicationPerspective.getTitleKeyOverride()) : localizationProvider.getLocalized(managedApplicationPerspective.getApplicationPerspective().getTitleKey()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, managedApplicationPerspective.getDescriptionKeyOverride() != null ? localizationProvider.getLocalized(managedApplicationPerspective.getDescriptionKeyOverride()) : localizationProvider.getLocalized(managedApplicationPerspective.getApplicationPerspective().getDescriptionKey()));
			return map;
		};
	}

	public static PropertyProvider<ManagedApplicationGroup> createManagedApplicationGroupPropertyProvider(ApplicationInstanceData applicationInstanceData) {
		return (managedApplicationGroup, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, IconUtils.decodeIcon(managedApplicationGroup.getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, applicationInstanceData.getLocalized(managedApplicationGroup.getTitleKey()));
			return map;
		};
	}

	public static PropertyProvider<OrganizationField> createOrganizationFieldPropertyProvider(ApplicationInstanceData applicationInstanceData) {
		return (organizationFieldView, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, IconUtils.decodeIcon(organizationFieldView.getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, applicationInstanceData.getLocalized(organizationFieldView.getTitle()));
			return map;
		};
	}

	public static PropertyProvider<OrganizationUnitType> creatOrganizationUnitTypePropertyProvider(ApplicationInstanceData applicationInstanceData) {
		Function<TranslatableText, String> translatableTextExtractor = TranslatableTextUtils.createTranslatableTextExtractor(applicationInstanceData);
		return (unitType, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, IconUtils.decodeIcon(unitType.getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, translatableTextExtractor.apply(unitType.getName()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, translatableTextExtractor.apply(unitType.getAbbreviation()));
			return map;
		};
	}


	public static PropertyProvider<OrganizationUnit> creatOrganizationUnitPropertyProvider(ApplicationInstanceData applicationInstanceData) {
		Function<TranslatableText, String> translatableTextExtractor = TranslatableTextUtils.createTranslatableTextExtractor(applicationInstanceData);
		return (unit, propertyNames) -> {
			String prefix = "";
			String abbreviation = translatableTextExtractor.apply(unit.getType().getAbbreviation());
			if (abbreviation != null) {
				prefix = abbreviation + "-";
			}
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, unit.getIcon() != null ? IconUtils.decodeIcon(unit.getIcon()) : IconUtils.decodeIcon(unit.getType().getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, prefix + translatableTextExtractor.apply(unit.getName()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, translatableTextExtractor.apply(unit.getType().getName()));
			return map;
		};
	}

	public static PropertyProvider<OrganizationUnit> creatOrganizationUnitWithPathPropertyProvider(ApplicationInstanceData applicationInstanceData) {
		PropertyProvider<OrganizationUnit> unitPropertyProvider = creatOrganizationUnitPropertyProvider(applicationInstanceData);
		return (unit, propertyNames) -> {
			Map<String, Object> map = unitPropertyProvider.getValues(unit, propertyNames);
			String path = (String) map.get(BaseTemplate.PROPERTY_CAPTION);
			int level = 0;
			OrganizationUnit parent = unit.getParent();
			while (parent != null && level < 3) {
				Map<String, Object> parentMap = unitPropertyProvider.getValues(parent, propertyNames);
				path = parentMap.get(BaseTemplate.PROPERTY_CAPTION) + "/" + path;
						level++;
				parent = parent.getParent();
			}
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, path);
			return map;
		};
	}

	public static PropertyProvider<Role> createRolePropertyProvider(ApplicationInstanceData applicationInstanceData) {
		return (role, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, IconUtils.decodeIcon(role.getIcon()));
			map.put(BaseTemplate.PROPERTY_CAPTION, applicationInstanceData.getLocalized(role.getTitle()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, role.getOrganizationField() != null ? applicationInstanceData.getLocalized(role.getOrganizationField().getTitle()) : null);
			return map;
		};
	}

	public static PropertyProvider<User> createUserPropertyProvider(ApplicationInstanceData applicationInstanceData) {
		Function<TranslatableText, String> translatableTextExtractor = TranslatableTextUtils.createTranslatableTextExtractor(applicationInstanceData.getUser().getRankedLanguages());
		return (user, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			String userProfilePictureLink = applicationInstanceData.getComponentFactory().createUserAvatarLink(user.getId(), false);
			if (userProfilePictureLink != null) {
				map.put(BaseTemplate.PROPERTY_IMAGE, userProfilePictureLink);
			} else {
				map.put(BaseTemplate.PROPERTY_IMAGE, "/ta-media/user-silhouette.png");
			}
			map.put(BaseTemplate.PROPERTY_CAPTION, user.getFirstName() + " " + user.getLastName());
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, translatableTextExtractor.apply(user.getOrganizationUnit() != null ? user.getOrganizationUnit().getName() : null));
			return map;
		};
	}

	public static PropertyProvider<Integer> createUserIdPropertyProvider(ApplicationInstanceData applicationInstanceData) {
		Function<TranslatableText, String> translatableTextExtractor = TranslatableTextUtils.createTranslatableTextExtractor(applicationInstanceData.getUser().getRankedLanguages());
		return (userId, propertyNames) -> {
			User user = User.getById(userId);
			Map<String, Object> map = new HashMap<>();
			if (!user.isStored()) {
				return map;
			}
			String userProfilePictureLink = applicationInstanceData.getComponentFactory().createUserAvatarLink(userId, false);
			if (userProfilePictureLink != null) {
				map.put(BaseTemplate.PROPERTY_IMAGE, userProfilePictureLink);
			} else {
				map.put(BaseTemplate.PROPERTY_IMAGE, "/ta-media/user-silhouette.png");
			}
			map.put(BaseTemplate.PROPERTY_CAPTION, user.getFirstName() + " " + user.getLastName());
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, translatableTextExtractor.apply(user.getOrganizationUnit() != null ? user.getOrganizationUnit().getName() : null));
			return map;
		};
	}

	public static PropertyProvider<User> createSimpleUserPropertyProvider(SystemRegistry registry) {
		return (user, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			String userProfilePictureLink = registry.getBaseResourceLinkProvider().getUserProfilePictureLink(user);
			if (userProfilePictureLink != null) {
				map.put(BaseTemplate.PROPERTY_IMAGE, userProfilePictureLink);
			} else {
				map.put(BaseTemplate.PROPERTY_IMAGE, "/ta-media/user-silhouette.png");
			}
			map.put(BaseTemplate.PROPERTY_CAPTION, user.getFirstName());
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, user.getLastName());
			return map;
		};
	}

	public static PropertyProvider<UserRoleAssignment> createUserRoleAssignmentPropertyProvider(UserSessionData userSessionData) {
		Function<TranslatableText, String> translatableTextExtractor = TranslatableTextUtils.createTranslatableTextExtractor(userSessionData.getRankedLanguages());
		return (assignment, propertyNames) -> {
			String prefix = "";
			String abbreviation = translatableTextExtractor.apply(assignment.getOrganizationUnit().getType().getAbbreviation());
			if (abbreviation != null) {
				prefix = abbreviation + "-";
			}
			Map<String, Object> map = new HashMap<>();
			String userProfilePictureLink = userSessionData.getRegistry().getBaseResourceLinkProvider().getUserProfilePictureLink(assignment.getUser());
			if (userProfilePictureLink != null) {
				map.put(BaseTemplate.PROPERTY_IMAGE, userProfilePictureLink);
			} else {
				map.put(BaseTemplate.PROPERTY_ICON, IconUtils.decodeIcon(assignment.getRole().getIcon()));
			}
			map.put(BaseTemplate.PROPERTY_CAPTION, assignment.getUser().getFirstName() + " " + assignment.getUser().getLastName());
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, prefix + translatableTextExtractor.apply(assignment.getOrganizationUnit().getName()));
			map.put(BaseTemplate.PROPERTY_BADGE, translatableTextExtractor.apply(assignment.getRole().getTitle()));
			return map;
		};
	}

	public static PropertyProvider<UserRoleAssignment> createUserRoleAssignmentPropertyProviderNoUserDisplay(UserSessionData userSessionData) {
		Function<TranslatableText, String> translatableTextExtractor = TranslatableTextUtils.createTranslatableTextExtractor(userSessionData.getRankedLanguages());
		return (assignment, propertyNames) -> {
			String prefix = "";
			String abbreviation = translatableTextExtractor.apply(assignment.getOrganizationUnit().getType().getAbbreviation());
			if (abbreviation != null) {
				prefix = abbreviation + "-";
			}
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, assignment.getRole() != null ? IconUtils.decodeIcon(assignment.getRole().getIcon()) : null);
			map.put(BaseTemplate.PROPERTY_CAPTION, translatableTextExtractor.apply(assignment.getRole().getTitle()));
			map.put(BaseTemplate.PROPERTY_DESCRIPTION, prefix + translatableTextExtractor.apply(assignment.getOrganizationUnit().getName()));
			return map;
		};
	}

	public static PropertyProvider<String> createStringPropertyProvider(Icon icon) {
		return (s, propertyNames) -> {
			Map<String, Object> map = new HashMap<>();
			map.put(BaseTemplate.PROPERTY_ICON, icon);
			map.put(BaseTemplate.PROPERTY_CAPTION, s);
			return map;
		};
	}
}
