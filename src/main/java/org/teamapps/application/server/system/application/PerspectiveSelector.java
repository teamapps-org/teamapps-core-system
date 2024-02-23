/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
 * ---
 * Copyright (C) 2020 - 2024 TeamApps.org
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
package org.teamapps.application.server.system.application;

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.perspective.PerspectiveBuilder;
import org.teamapps.application.api.application.perspective.PerspectiveMenuPanel;
import org.teamapps.databinding.MutableValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PerspectiveSelector extends AbstractManagedApplicationPerspective {

	private final List<PerspectiveBuilder> perspectiveBuilders;

	public PerspectiveSelector(ApplicationInstanceData applicationInstanceData, MutableValue<String> perspectiveInfoBadgeValue, PerspectiveBuilder... perspectives) {
		super(applicationInstanceData, perspectiveInfoBadgeValue);
		perspectiveBuilders = Arrays.asList(perspectives);
		createUi();
	}

	private void createUi() {
		List<PerspectiveBuilder> perspectiveBuilders = getPerspectives().stream().filter(builder -> builder.isPerspectiveAccessible(getApplicationInstanceData())).collect(Collectors.toList());
		PerspectiveBuilder selectedPerspective = !perspectiveBuilders.isEmpty() ? perspectiveBuilders.get(0) : null;
		PerspectiveMenuPanel menuPanel = PerspectiveMenuPanel.createMenuPanel(getApplicationInstanceData(), perspectiveBuilders);
		setPerspectiveMenuPanel(menuPanel.getComponent(), menuPanel.getButtonMenu());
		onPerspectiveInitialized.addListener(() -> menuPanel.openPerspective(selectedPerspective));
	}

	public List<PerspectiveBuilder> getPerspectives() {
		return perspectiveBuilders;
	}



}
