package org.teamapps.application.server.system.privilege;

import org.teamapps.application.api.privilege.*;
import org.teamapps.application.ui.privilege.EntityPrivileges;
import org.teamapps.model.controlcenter.OrganizationField;
import org.teamapps.model.controlcenter.OrganizationFieldView;
import org.teamapps.model.controlcenter.OrganizationUnitView;
import org.teamapps.universaldb.pojo.Entity;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MultiOrganizationFieldEntityPrivileges<ENTITY extends Entity<?>> implements EntityPrivileges<ENTITY> {

	private final OrganizationalPrivilegeGroup organizationalPrivilegeGroup;
	private final Function<ENTITY, OrganizationUnitView> unitByEntityFunction;
	private final Map<OrganizationFieldView, ApplicationPrivilegeProvider> applicationPrivilegeProviderMap;
	private final Function<ENTITY, OrganizationField> organizationFieldExtractor;

	public MultiOrganizationFieldEntityPrivileges(OrganizationalPrivilegeGroup organizationalPrivilegeGroup, Function<ENTITY, OrganizationUnitView> unitByEntityFunction, Map<OrganizationFieldView, ApplicationPrivilegeProvider> applicationPrivilegeProviderMap, Function<ENTITY, OrganizationField> organizationFieldExtractor) {
		this.organizationalPrivilegeGroup = organizationalPrivilegeGroup;
		this.unitByEntityFunction = unitByEntityFunction;
		this.applicationPrivilegeProviderMap = applicationPrivilegeProviderMap;
		this.organizationFieldExtractor = organizationFieldExtractor;
	}

	@Override
	public boolean isCreateAllowed() {
		return applicationPrivilegeProviderMap.values().stream().anyMatch(p -> !p.getAllowedUnits(organizationalPrivilegeGroup, Privilege.CREATE).isEmpty());
	}

	@Override
	public boolean isSaveOptionAvailable(ENTITY entity, ENTITY synchronizedEntityCopy) {
		if (entity.isStored()) {
			OrganizationUnitView orgUnit = unitByEntityFunction.apply(entity);
			return isAllowed(entity, organizationalPrivilegeGroup, Privilege.UPDATE, orgUnit);
		} else {
			return !getAllowedUnits(synchronizedEntityCopy, organizationalPrivilegeGroup, Privilege.CREATE).isEmpty();
		}
	}

	@Override
	public boolean isSaveAllowed(ENTITY entity, ENTITY synchronizedEntityCopy) {
		Privilege privilege = entity.isStored() ? Privilege.UPDATE : Privilege.CREATE;
		OrganizationUnitView orgUnit = unitByEntityFunction.apply(entity);
		return isAllowed(entity, organizationalPrivilegeGroup, privilege, orgUnit);
	}

	@Override
	public boolean isDeleteAllowed(ENTITY entity) {
		OrganizationUnitView orgUnit = unitByEntityFunction.apply(entity);
		return isAllowed(entity, organizationalPrivilegeGroup, Privilege.DELETE, orgUnit);
	}

	@Override
	public boolean isRestoreAllowed(ENTITY entity) {
		OrganizationUnitView orgUnit = unitByEntityFunction.apply(entity);
		return isAllowed(entity, organizationalPrivilegeGroup, Privilege.RESTORE, orgUnit);
	}

	@Override
	public boolean isModificationHistoryAllowed(ENTITY entity) {
		OrganizationUnitView orgUnit = unitByEntityFunction.apply(entity);
		return isAllowed(entity, organizationalPrivilegeGroup, Privilege.SHOW_MODIFICATION_HISTORY, orgUnit);
	}

	private ApplicationPrivilegeProvider getApplicationPrivilegeProvider(ENTITY entity) {
		OrganizationField organizationField = organizationFieldExtractor.apply(entity);
		if (organizationField == null) {
			return new AllowNonePrivilegeProvider();
		}
		ApplicationPrivilegeProvider privilegeProvider = applicationPrivilegeProviderMap.get(OrganizationFieldView.getById(organizationField.getId()));
		return privilegeProvider != null ? privilegeProvider : new AllowNonePrivilegeProvider();
	}

	public boolean isAllowed(ENTITY entity, SimplePrivilege simplePrivilege) {
		return getApplicationPrivilegeProvider(entity).isAllowed(simplePrivilege);
	}


	public boolean isAllowed(ENTITY entity, OrganizationalPrivilegeGroup group, Privilege privilege, OrganizationUnitView organizationUnitView) {
		return getApplicationPrivilegeProvider(entity).isAllowed(group, privilege, organizationUnitView);
	}


	public List<OrganizationUnitView> getAllowedUnits(ENTITY entity, OrganizationalPrivilegeGroup group, Privilege privilege) {
		return getApplicationPrivilegeProvider(entity).getAllowedUnits(group, privilege);
	}


}
