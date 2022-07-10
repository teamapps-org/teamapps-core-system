/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
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
package org.teamapps.application.server.system.server;

import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.ux.session.SessionContext;

public interface SystemSessionHandler {

	void handleNewSession(SessionContext context);

	void handleAuthenticatedUser(UserSessionData userSessionDat, SessionContext context);

	void handleLogout(UserSessionData userSessionDat, SessionContext context);
}
