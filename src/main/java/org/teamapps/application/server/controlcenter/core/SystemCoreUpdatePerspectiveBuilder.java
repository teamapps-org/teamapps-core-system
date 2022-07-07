package org.teamapps.application.server.controlcenter.core;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.perspective.AbstractPerspectiveBuilder;
import org.teamapps.application.api.application.perspective.ApplicationPerspective;
import org.teamapps.application.api.privilege.ApplicationPrivilegeProvider;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.databinding.MutableValue;

public class SystemCoreUpdatePerspectiveBuilder extends AbstractPerspectiveBuilder {

	public SystemCoreUpdatePerspectiveBuilder() {
		super("systemCoreUpdatePerspective", ApplicationIcons.COMPUTER_CHIP, "systemCore.title", "systemCore.desc");
	}

	@Override
	public boolean isPerspectiveAccessible(ApplicationPrivilegeProvider applicationPrivilegeProvider) {
		return applicationPrivilegeProvider.isReadAccess(Privileges.CLUSTER_PERSPECTIVE);
	}

	@Override
	public boolean autoProvisionPerspective() {
		return true;
	}

	@Override
	public ApplicationPerspective build(ApplicationInstanceData applicationInstanceData, MutableValue<String> mutableValue) {
		return new SystemCoreUpdatePerspective(applicationInstanceData, mutableValue);
	}

}