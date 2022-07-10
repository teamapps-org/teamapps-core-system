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
package org.teamapps.application.server.system.bootstrap;

import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.animation.PageTransition;
import org.teamapps.ux.component.field.DisplayField;
import org.teamapps.ux.component.flexcontainer.FlexSizeUnit;
import org.teamapps.ux.component.flexcontainer.FlexSizingPolicy;
import org.teamapps.ux.component.flexcontainer.VerticalLayout;
import org.teamapps.ux.component.rootpanel.RootPanel;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ApplicationRootPanel extends RootPanel {

	private final DisplayField stylesField;
	private Component component;
	private String baseStyles = "";
	private Map<String, String> applicationStyles = new HashMap<>();

	public ApplicationRootPanel() {
		stylesField = new DisplayField(false, true);
		stylesField.setRemoveStyleTags(false);
		stylesField.setVisible(false);
	}

	public void setBaseStyles(String baseStyles) {
		this.baseStyles = baseStyles;
	}

	public void setApplicationStyles(String applicationName, String styles) {
		if (styles == null) styles = "";
		applicationStyles.put(applicationName, styles);
	}


	private void updateStyles() {
		String css = baseStyles + "\n";
		String applicationsCss = applicationStyles.values().stream().collect(Collectors.joining("\n"));
		stylesField.setValue("<style>" + css + applicationsCss + "</style>");
		stylesField.render();
	}

	@Override
	public void setContent(Component component) {
		setContent(component, null, 0);
	}

	@Override
	public void setContent(Component component, PageTransition animation, long animationDuration) {
		this.component = component;
		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.addComponentFillRemaining(component);
		updateStyles();
		stylesField.render();
		verticalLayout.addComponent(stylesField, new FlexSizingPolicy(0, FlexSizeUnit.PIXEL, 0, 0));
		super.setContent(verticalLayout, animation, animationDuration);
	}

	@Override
	public Component getContent() {
		return component;
	}
}
