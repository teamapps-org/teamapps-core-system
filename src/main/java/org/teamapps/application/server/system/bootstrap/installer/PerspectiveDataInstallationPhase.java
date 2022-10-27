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
package org.teamapps.application.server.system.bootstrap.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.application.ApplicationBuilder;
import org.teamapps.application.api.application.ApplicationGroup;
import org.teamapps.application.api.application.BaseApplicationBuilder;
import org.teamapps.application.api.application.perspective.PerspectiveBuilder;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.bootstrap.ApplicationInfo;
import org.teamapps.application.server.system.bootstrap.ApplicationInfoDataElement;
import org.teamapps.application.tools.KeyCompare;
import org.teamapps.application.server.system.utils.ValueCompare;
import org.teamapps.application.ux.IconUtils;
import org.teamapps.model.controlcenter.*;
import org.teamapps.universaldb.index.numeric.NumericFilter;
import org.teamapps.universaldb.index.text.TextFilter;
import org.teamapps.universaldb.pojo.Entity;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PerspectiveDataInstallationPhase implements ApplicationInstallationPhase {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void checkApplication(ApplicationInfo applicationInfo) {
		try {
			if (!applicationInfo.getErrors().isEmpty()) {
				return;
			}
			BaseApplicationBuilder baseApplicationBuilder = applicationInfo.getBaseApplicationBuilder();
			if (!(baseApplicationBuilder instanceof ApplicationBuilder)) {
				applicationInfo.setPerspectiveData(new ApplicationInfoDataElement());
				return;
			}
			ApplicationBuilder applicationBuilder = (ApplicationBuilder) baseApplicationBuilder;
			List<PerspectiveBuilder> perspectives = applicationBuilder.getPerspectiveBuilders();
			if (perspectives == null) {
				applicationInfo.addError("Missing perspectives");
				return;
			}
			if (perspectives.isEmpty() && applicationBuilder.getRequiredPerspectives().isEmpty()) {
				applicationInfo.addWarning("No perspective available");
			}
			for (PerspectiveBuilder builder : perspectives) {
				if (builder.getName() == null || builder.getTitleKey() == null) {
					applicationInfo.addError("Missing perspective meta data for perspective: " + builder.getName());
					return;
				}
			}
			ApplicationInfoDataElement dataInfo = new ApplicationInfoDataElement();
			Application application = applicationInfo.getApplication();
			List<ApplicationPerspective> applicationPerspectives = application == null ? Collections.emptyList() : ApplicationPerspective.filter()
					.application(NumericFilter.equalsFilter(application.getId()))
					.execute();

			dataInfo.setData(perspectives.stream().map(PerspectiveBuilder::getName).collect(Collectors.joining("\n")));
			KeyCompare<PerspectiveBuilder, ApplicationPerspective> keyCompare = new KeyCompare<>(perspectives, applicationPerspectives, PerspectiveBuilder::getName, ApplicationPerspective::getName);
			List<PerspectiveBuilder> newPerspectives = keyCompare.getAEntriesNotInB();
			dataInfo.setDataAdded(newPerspectives.stream().map(PerspectiveBuilder::getName).collect(Collectors.toList()));
			List<ApplicationPerspective> removedPerspectives = keyCompare.getBEntriesNotInA();
			dataInfo.setDataRemoved(removedPerspectives.stream().map(ApplicationPerspective::getName).collect(Collectors.toList()));
			applicationInfo.setPerspectiveData(dataInfo);
		} catch (Exception e) {
			applicationInfo.addError("Error checking perspectives:" + e.getMessage());
			LOGGER.error("Error checking perspectives:", e);
		}
	}

	@Override
	public void installApplication(ApplicationInfo applicationInfo) {
		BaseApplicationBuilder baseApplicationBuilder = applicationInfo.getBaseApplicationBuilder();
		if (!(baseApplicationBuilder instanceof ApplicationBuilder)) {
			return;
		}
		ApplicationBuilder applicationBuilder = (ApplicationBuilder) baseApplicationBuilder;
		List<PerspectiveBuilder> perspectives = applicationBuilder.getPerspectiveBuilders();
		Application application = applicationInfo.getApplication();
		List<ApplicationPerspective> applicationPerspectives = ApplicationPerspective.filter()
				.application(NumericFilter.equalsFilter(application.getId()))
				.execute();

		KeyCompare<PerspectiveBuilder, ApplicationPerspective> keyCompare = new KeyCompare<>(perspectives, applicationPerspectives, PerspectiveBuilder::getName, ApplicationPerspective::getName);
		List<PerspectiveBuilder> newPerspectives = keyCompare.getAEntriesNotInB();
		newPerspectives.forEach(perspective -> ApplicationPerspective.create()
				.setApplication(application)
				.setName(perspective.getName())
				.setIcon(IconUtils.encodeNoStyle(perspective.getIcon()))
				.setTitleKey(perspective.getTitleKey())
				.setDescriptionKey(perspective.getDescriptionKey())
				.setAutoProvision(perspective.autoProvisionPerspective())
				.setToolbarPerspectiveMenu(perspective.useToolbarPerspectiveMenu())
				.save());

		List<ApplicationPerspective> removedPerspectives = keyCompare.getBEntriesNotInA();
		removedPerspectives.forEach(perspective -> {
			perspective.getManagedPerspectives().forEach(Entity::delete);
			perspective.delete();
		});

		List<PerspectiveBuilder> existingPerspectives = keyCompare.getAEntriesInB();
		for (PerspectiveBuilder perspective : existingPerspectives) {
			ApplicationPerspective applicationPerspective = keyCompare.getB(perspective);
			if (ValueCompare
					.create(perspective.getTitleKey(), applicationPerspective.getTitleKey())
					.check(perspective.getDescriptionKey(), applicationPerspective.getDescriptionKey())
					.check(IconUtils.encodeNoStyle(perspective.getIcon()), applicationPerspective.getIcon())
					.check(perspective.autoProvisionPerspective(), applicationPerspective.getAutoProvision())
					.check(perspective.useToolbarPerspectiveMenu(), applicationPerspective.getToolbarPerspectiveMenu())
					.isDifferent()
			) {
				applicationPerspective
						.setTitleKey(perspective.getTitleKey())
						.setDescriptionKey(perspective.getDescriptionKey())
						.setIcon(IconUtils.encodeNoStyle(perspective.getIcon()))
						.setAutoProvision(perspective.autoProvisionPerspective())
						.setToolbarPerspectiveMenu(perspective.useToolbarPerspectiveMenu())
						.save();
			}
		}

		List<ManagedApplication> installedAsMainApplication = application.getInstalledAsMainApplication();

		if (installedAsMainApplication.isEmpty()) {
			ManagedApplicationGroup applicationGroup = getApplicationGroup(applicationBuilder.getPreferredApplicationGroup());
			ManagedApplication managedApplication = ManagedApplication.create()
					.setMainApplication(application)
					.setApplicationGroup(applicationGroup)
					.setSingleApplication(applicationInfo.isUnmanagedPerspectives())
					.save();

			List<ApplicationPerspective> autoProvisionPerspectives = application.getPerspectives().stream().filter(ApplicationPerspective::getAutoProvision).collect(Collectors.toList());
			for (ApplicationPerspective perspective : autoProvisionPerspectives) {
				ManagedApplicationPerspective.create()
						.setManagedApplication(managedApplication)
						.setApplicationPerspective(perspective)
						.save();
			}
		}

		installedAsMainApplication = application.getInstalledAsMainApplication();

		List<String> requiredPerspectives = applicationBuilder.getRequiredPerspectives();
		if (!requiredPerspectives.isEmpty()) {
			for (ManagedApplication managedApplication : installedAsMainApplication) {
				Set<String> availablePerspectives = managedApplication.getPerspectives().stream().map(p -> p.getApplicationPerspective().getName()).collect(Collectors.toSet());
				for (int i = 0; i < requiredPerspectives.size(); i++) {
					String perspectiveName = requiredPerspectives.get(i);
					if (!availablePerspectives.contains(perspectiveName)) {
						ApplicationPerspective perspective = getPerspective(perspectiveName, application);
						ManagedApplicationPerspective.create()
								.setManagedApplication(managedApplication)
								.setApplicationPerspective(perspective)
								.setListingPosition(i)
								.save();
					}
				}
			}
		}

	}

	private ApplicationPerspective getPerspective(String name, Application preferredApplication) {
		return preferredApplication.getPerspectives().stream().filter(p -> p.getName().equals(name)).findFirst().orElse(
				ApplicationPerspective.filter().name(TextFilter.textEqualsFilter(name)).executeExpectSingleton()
		);
	}

	private ManagedApplicationGroup getApplicationGroup(ApplicationGroup preferredGroup) {
		if (preferredGroup == null && ManagedApplicationGroup.getCount() == 0) {
			ManagedApplicationGroup.create().setIcon(IconUtils.encodeNoStyle(ApplicationIcons.HOME)).setTitleKey(Dictionary.APPLICATIONS).save();
		}
		if (preferredGroup == null) {
			return ManagedApplicationGroup.getAll().get(0);
		} else {
			ManagedApplicationGroup applicationGroup = ManagedApplicationGroup.filter().name(TextFilter.textEqualsFilter(preferredGroup.getName())).executeExpectSingleton();
			if (applicationGroup == null) {
				applicationGroup = ManagedApplicationGroup.create()
						.setName(preferredGroup.getName())
						.setTitleKey(preferredGroup.getTitleKey())
						.setIcon(IconUtils.encodeNoStyle(preferredGroup.getIcon()))
						.save();
			}
			return applicationGroup;
		}
	}

	@Override
	public void loadApplication(ApplicationInfo applicationInfo) {

	}

}
