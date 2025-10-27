package org.teamapps.application.server.system.privilege;

import org.teamapps.application.api.privilege.*;
import org.teamapps.model.controlcenter.OrganizationFieldView;
import org.teamapps.model.controlcenter.OrganizationUnitView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MultiOrganizationFieldPrivilegeProvider implements ApplicationPrivilegeProvider {

	private final List<ApplicationPrivilegeProvider> applicationPrivileges;

	private Map<OrganizationFieldView, ApplicationPrivilegeProvider> privilegeProviderByOrgField;

	public MultiOrganizationFieldPrivilegeProvider(Map<OrganizationFieldView, ApplicationPrivilegeProvider> privilegeProviderByOrgField) {
		this.privilegeProviderByOrgField = privilegeProviderByOrgField;
		applicationPrivileges = new ArrayList<>();
		applicationPrivileges.addAll(privilegeProviderByOrgField.values());
	}

	public Map<OrganizationFieldView, ApplicationPrivilegeProvider> getPrivilegeProviderByOrgField() {
		return privilegeProviderByOrgField;
	}

	@Override
	public boolean isAllowed(SimplePrivilege simplePrivilege) {
		return applicationPrivileges.stream().anyMatch(p -> p.isAllowed(simplePrivilege));
	}

	@Override
	public boolean isAllowed(SimpleOrganizationalPrivilege group, OrganizationUnitView organizationUnitView) {
		return applicationPrivileges.stream().anyMatch(p -> p.isAllowed(group, organizationUnitView));
	}

	@Override
	public boolean isAllowed(SimpleCustomObjectPrivilege group, PrivilegeObject privilegeObject) {
		return applicationPrivileges.stream().anyMatch(p -> p.isAllowed(group, privilegeObject));
	}

	@Override
	public boolean isAllowed(StandardPrivilegeGroup group, Privilege privilege) {
		return applicationPrivileges.stream().anyMatch(p -> p.isAllowed(group, privilege));
	}

	@Override
	public boolean isAllowed(OrganizationalPrivilegeGroup group, Privilege privilege, OrganizationUnitView organizationUnitView) {
		return applicationPrivileges.stream().anyMatch(p -> p.isAllowed(group, privilege, organizationUnitView));
	}

	@Override
	public boolean isAllowed(CustomObjectPrivilegeGroup group, Privilege privilege, PrivilegeObject privilegeObject) {
		return applicationPrivileges.stream().anyMatch(p -> p.isAllowed(group, privilege, privilegeObject));
	}

	@Override
	public boolean isAllowed(RoleAssignmentDelegatedCustomPrivilegeGroup group, Privilege privilege, PrivilegeObject privilegeObject) {
		return applicationPrivileges.stream().anyMatch(p -> p.isAllowed(group, privilege, privilegeObject));
	}

	@Override
	public List<OrganizationUnitView> getAllowedUnits(SimpleOrganizationalPrivilege simplePrivilege) {
		return applicationPrivileges.stream().flatMap(p -> p.getAllowedUnits(simplePrivilege).stream()).distinct().toList();
	}

	@Override
	public List<OrganizationUnitView> getAllowedUnits(OrganizationalPrivilegeGroup group, Privilege privilege) {
		return applicationPrivileges.stream().flatMap(p -> p.getAllowedUnits(group, privilege).stream()).distinct().toList();
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(SimpleCustomObjectPrivilege simplePrivilege) {
		return applicationPrivileges.stream().flatMap(p -> p.getAllowedPrivilegeObjects(simplePrivilege).stream()).distinct().toList();
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(CustomObjectPrivilegeGroup group, Privilege privilege) {
		return applicationPrivileges.stream().flatMap(p -> p.getAllowedPrivilegeObjects(group, privilege).stream()).distinct().toList();
	}

	@Override
	public List<PrivilegeObject> getAllowedPrivilegeObjects(RoleAssignmentDelegatedCustomPrivilegeGroup group, Privilege privilege) {
		return applicationPrivileges.stream().flatMap(p -> p.getAllowedPrivilegeObjects(group, privilege).stream()).distinct().toList();
	}

	@Override
	public Map<OrganizationFieldView, ApplicationPrivilegeProvider> getInheritedOrganizationFieldPrivilegeProviderMap() {
		return privilegeProviderByOrgField;
	}

}
