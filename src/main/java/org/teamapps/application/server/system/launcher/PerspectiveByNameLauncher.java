package org.teamapps.application.server.system.launcher;

import org.teamapps.application.api.application.perspective.ApplicationPerspective;

public interface PerspectiveByNameLauncher {

	ApplicationPerspective showApplicationPerspective(String perspectiveName);
}
