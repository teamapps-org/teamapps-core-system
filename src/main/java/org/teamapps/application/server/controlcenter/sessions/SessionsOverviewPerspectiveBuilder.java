package org.teamapps.application.server.controlcenter.sessions;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.perspective.AbstractPerspectiveBuilder;
import org.teamapps.application.api.application.perspective.ApplicationPerspective;
import org.teamapps.application.api.privilege.ApplicationPrivilegeProvider;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.controlcenter.Privileges;
import org.teamapps.application.server.controlcenter.roles.RolesPerspective;
import org.teamapps.databinding.MutableValue;

public class SessionsOverviewPerspectiveBuilder extends AbstractPerspectiveBuilder {

	public SessionsOverviewPerspectiveBuilder() {
		super("sessionsOverview", ApplicationIcons.GRAPH_CLAW, "sessionsOverview.title", "sessionsOverview.desc");
	}

	@Override
	public boolean isPerspectiveAccessible(ApplicationPrivilegeProvider applicationPrivilegeProvider) {
		return applicationPrivilegeProvider.isReadAccess(Privileges.ROLES_PERSPECTIVE);
	}

	@Override
	public boolean autoProvisionPerspective() {
		return true;
	}

	@Override
	public ApplicationPerspective build(ApplicationInstanceData applicationInstanceData, MutableValue<String> mutableValue) {
		return new SessionsOverviewPerspective(applicationInstanceData, mutableValue);
	}
}