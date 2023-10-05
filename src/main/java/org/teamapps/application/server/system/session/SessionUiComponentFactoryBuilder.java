package org.teamapps.application.server.system.session;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.model.controlcenter.Application;

public interface SessionUiComponentFactoryBuilder {

	SessionUiComponentFactory build(ApplicationInstanceData applicationInstanceData, SystemRegistry systemRegistry, Application application);
}
