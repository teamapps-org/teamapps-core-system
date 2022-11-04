package org.teamapps.application.server.controlcenter.sessions;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.core.TeamAppsCore;
import org.teamapps.databinding.MutableValue;
import org.teamapps.uisession.statistics.app.SessionStatsPerspective;
import org.teamapps.uisession.statistics.app.SessionStatsSharedBaseTableModel;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.perspective.Perspective;
import org.teamapps.ux.application.view.View;

public class SessionsOverviewPerspective extends AbstractManagedApplicationPerspective {

	private final PerspectiveSessionData perspectiveSessionData;
	private final UserSessionData userSessionData;

	public SessionsOverviewPerspective(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		perspectiveSessionData = (PerspectiveSessionData) getApplicationInstanceData();
		userSessionData = perspectiveSessionData.getManagedApplicationSessionData().getUserSessionData();
		createUi();
	}

	private void createUi() {
		Perspective perspective = getPerspective();
		TeamAppsCore teamAppsCore = userSessionData.getRegistry().getServerRegistry().getTeamAppsCore();
		SessionStatsSharedBaseTableModel baseTableModel = new SessionStatsSharedBaseTableModel(teamAppsCore.getSessionManager());
		SessionStatsPerspective sessionStatsPerspective = new SessionStatsPerspective(baseTableModel);
		View listView = perspective.addView(View.createView(StandardLayout.CENTER, ApplicationIcons.GRAPH_CLAW, "Sessions", sessionStatsPerspective.getTable()));
		View sessionView = perspective.addView(View.createView(StandardLayout.RIGHT, ApplicationIcons.USER, "Session", sessionStatsPerspective.getDetailVerticalLayout()));
		sessionView.addLocalButtonGroup(sessionStatsPerspective.getDetailsToolbarButtonGroup());
	}
}
