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
package org.teamapps.application.server.system.launcher;

import org.teamapps.event.Event;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.Component;

public class MobileApplicationNavigation implements MobileNavigation {

	public final Event<Void> onBackOperation = new Event<>();
	private final Event<Void> onShowStartViewRequest = new Event<>();
	private final Event<View> onShowViewRequest = new Event<>();
	private Component applicationLauncher;
	private View applicationMenu;
	private boolean backOperationAvailable;

	public void setApplicationLauncher(Component applicationLauncher) {
		this.applicationLauncher = applicationLauncher;
	}

	public void setApplicationMenu(View applicationMenu) {
		this.applicationMenu = applicationMenu;
	}

	@Override
	public Event<Void> onShowStartViewRequest() {
		return onShowStartViewRequest;
	}

	@Override
	public Event<View> onShowViewRequest() {
		return onShowViewRequest;
	}

	@Override
	public Component getApplicationLauncher() {
		return applicationLauncher;
	}

	@Override
	public View getApplicationMenuView() {
		return applicationMenu;
	}

	@Override
	public boolean isBackOperationAvailable() {
		return backOperationAvailable;
	}

	@Override
	public void fireBackOperation() {
		backOperationAvailable = false;
		onBackOperation.fire();
	}

	public void setBackOperationAvailable(boolean backOperationAvailable) {
		this.backOperationAvailable = backOperationAvailable;
	}
}
