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
