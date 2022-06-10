package org.teamapps.application.server.system.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.application.perspective.ApplicationPerspective;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.application.AbstractManagedApplicationPerspective;
import org.teamapps.application.server.system.session.ManagedApplicationSessionData;
import org.teamapps.application.server.system.session.PerspectiveSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.common.format.Color;
import org.teamapps.databinding.MutableValue;
import org.teamapps.model.controlcenter.ManagedApplicationPerspective;
import org.teamapps.ux.application.ResponsiveApplication;
import org.teamapps.ux.application.layout.StandardLayout;
import org.teamapps.ux.application.perspective.Perspective;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.animation.PageTransition;
import org.teamapps.ux.component.flexcontainer.VerticalLayout;
import org.teamapps.ux.component.itemview.SimpleItemGroup;
import org.teamapps.ux.component.itemview.SimpleItemView;
import org.teamapps.ux.component.mobile.MobileLayout;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.template.BaseTemplateRecord;
import org.teamapps.ux.component.toolbar.Toolbar;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;
import org.teamapps.ux.component.tree.Tree;
import org.teamapps.ux.model.ListTreeModel;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationInstance implements PerspectiveByNameLauncher {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final UserSessionData userSessionData;
	private final ApplicationData applicationData;
	private final Component applicationLauncher;
	private final MutableValue<ManagedApplicationPerspective> selectedPerspective;
	private boolean mobileInstance;
	private ToolbarButton backButton;
	private MobileLayout mobileLayout;
	private final Map<PerspectiveSessionData, ApplicationPerspective> applicationPerspectiveByPerspectiveBuilder = new HashMap<>();
	private View mobileApplicationMenu;
	private List<PerspectiveSessionData> sortedPerspectives;
	private Tree<PerspectiveSessionData> applicationMenuTree;

	public ApplicationInstance(UserSessionData userSessionData, ApplicationData applicationData, Component applicationLauncher, MutableValue<ManagedApplicationPerspective> selectedPerspective) {
		this.userSessionData = userSessionData;
		this.applicationData = applicationData;
		this.applicationLauncher = applicationLauncher;
		this.selectedPerspective = selectedPerspective;
		init();
	}

	private void init() {
		ManagedApplicationSessionData applicationSessionData = applicationData.getApplicationSessionData();
		sortedPerspectives = new ArrayList<>();
		List<ManagedApplicationPerspective> managedApplicationPerspectives = applicationData.getManagedApplication().getPerspectives().stream()
				.sorted(Comparator.comparingInt(ManagedApplicationPerspective::getListingPosition))
				.collect(Collectors.toList());
		for (ManagedApplicationPerspective managedApplicationPerspective : managedApplicationPerspectives) {
			if (managedApplicationPerspective.getApplicationPerspective() != null) {
				PerspectiveSessionData perspectiveSessionData = applicationSessionData.createPerspectiveSessionData(managedApplicationPerspective, this);
				if (perspectiveSessionData == null) {
					LOGGER.error("Missing application loader for:" + managedApplicationPerspective.getApplicationPerspective());
				}
				if (perspectiveSessionData != null && perspectiveSessionData.getPerspectiveBuilder() !=null &&  perspectiveSessionData.getPerspectiveBuilder().isPerspectiveAccessible(perspectiveSessionData)) {
					sortedPerspectives.add(perspectiveSessionData);
				}
			}
		}
	}

	public Component createApplication() {
		ManagedApplicationSessionData applicationSessionData = applicationData.getApplicationSessionData();
		ResponsiveApplication responsiveApplication = applicationSessionData.getResponsiveApplication();
		if (applicationSessionData.isUnmanagedApplication()) {
			applicationSessionData.getMainApplication().getBaseApplicationBuilder().build(responsiveApplication, applicationSessionData.getUnmanagedApplicationData());
			return responsiveApplication.getUi();
		}

		boolean toolbarApplicationMenu = applicationData.getManagedApplication().getToolbarApplicationMenu();
		backButton = toolbarApplicationMenu ? null : new ToolbarButton(BaseTemplate.LIST_ITEM_LARGE_ICON_SINGLE_LINE, new BaseTemplateRecord(ApplicationIcons.NAVIGATE_LEFT, getLocalized(Dictionary.BACK), null));
		mobileLayout = toolbarApplicationMenu ? null : new MobileLayout();

		if (toolbarApplicationMenu) {
			SimpleItemView<PerspectiveSessionData> applicationMenu = createApplicationMenu(sortedPerspectives);
			applicationSessionData.setApplicationToolbarMenuComponent(applicationMenu);

			applicationMenu.onItemClicked.addListener(event -> {
				PerspectiveSessionData perspectiveSessionData = event.getItem().getPayload();
				showPerspective(perspectiveSessionData);
			});
		} else {
			applicationMenuTree = createApplicationMenuTree(sortedPerspectives);
			View applicationMenu = View.createView(StandardLayout.LEFT, ApplicationIcons.RADIO_BUTTON_GROUP, getLocalized(Dictionary.MENU), null);
			responsiveApplication.addApplicationView(applicationMenu);
			applicationMenu.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.84f));
			VerticalLayout verticalLayout = new VerticalLayout();
			applicationMenu.setComponent(verticalLayout);

			Toolbar toolbar = new Toolbar();
			ToolbarButtonGroup buttonGroup = toolbar.addButtonGroup(new ToolbarButtonGroup());
			buttonGroup.setShowGroupSeparator(false);
			backButton.setVisible(false);
			buttonGroup.addButton(backButton);
			verticalLayout.addComponent(toolbar);
			mobileLayout.setContent(applicationMenuTree);
			verticalLayout.addComponentFillRemaining(mobileLayout);

			backButton.onClick.addListener(() -> {
				backButton.setVisible(false);
				mobileLayout.setContent(applicationMenuTree, PageTransition.MOVE_TO_RIGHT_VS_MOVE_FROM_LEFT, 500);
			});

			applicationMenuTree.onNodeSelected.addListener(this::showPerspective);
		}

		if (!sortedPerspectives.isEmpty()) {
			showPerspective(sortedPerspectives.get(0));
		}

		return responsiveApplication.getUi();
	}

	public Component createMobileApplication() {
		mobileInstance = true;
		ManagedApplicationSessionData applicationSessionData = applicationData.getApplicationSessionData();
		MobileApplicationNavigation mobileNavigation = applicationSessionData.getMobileNavigation();
		mobileNavigation.setApplicationLauncher(applicationLauncher);
		ResponsiveApplication responsiveApplication = applicationSessionData.getResponsiveApplication();
		if (applicationSessionData.isUnmanagedApplication()) {
			applicationSessionData.getMainApplication().getBaseApplicationBuilder().build(responsiveApplication, applicationSessionData.getUnmanagedApplicationData());
			return responsiveApplication.getUi();
		}


		mobileApplicationMenu = View.createView(StandardLayout.LEFT, ApplicationIcons.RADIO_BUTTON_GROUP, getLocalized(Dictionary.APPLICATION_MENU), null);
		mobileApplicationMenu.getPanel().setBodyBackgroundColor(Color.WHITE.withAlpha(0.84f));
		responsiveApplication.addApplicationView(mobileApplicationMenu);
		mobileLayout = new MobileLayout();
		mobileApplicationMenu.setComponent(mobileLayout);
		mobileNavigation.setApplicationMenu(mobileApplicationMenu);

		Tree<PerspectiveSessionData> tree = createApplicationMenuTree(sortedPerspectives);
		mobileLayout.setContent(tree);

		tree.onNodeSelected.addListener(this::showMobilePerspective);
		mobileNavigation.onBackOperation.addListener(() -> mobileLayout.setContent(tree, PageTransition.MOVE_TO_RIGHT_VS_MOVE_FROM_LEFT, 500));
		mobileNavigation.onShowViewRequest().fire(mobileApplicationMenu);

		return responsiveApplication.getUi();
	}

	@Override
	public ApplicationPerspective showApplicationPerspective(String perspectiveName) {
		PerspectiveSessionData perspectiveSessionData = sortedPerspectives.stream()
				.filter(p -> p.getPerspectiveBuilder().getName().equals(perspectiveName))
				.findFirst().orElse(null);
		if (perspectiveSessionData == null) {
			return null;
		} else {
			if (mobileInstance) {
				return showMobilePerspective(perspectiveSessionData);
			} else {
				return showPerspective(perspectiveSessionData);
			}
		}
	}

	private ApplicationPerspective showPerspective(PerspectiveSessionData perspectiveSessionData) {
		ManagedApplicationPerspective currentPerspective = perspectiveSessionData.getManagedApplicationPerspective();
		selectedPerspective.set(currentPerspective);
		ApplicationLauncher.THREAD_LOCAL_MANAGED_PERSPECTIVE.set(currentPerspective);
		LOGGER.info("Open perspective");
		ResponsiveApplication responsiveApplication = perspectiveSessionData.getManagedApplicationSessionData().getResponsiveApplication();
		ApplicationPerspective applicationPerspective = applicationPerspectiveByPerspectiveBuilder.get(perspectiveSessionData);
		if (applicationPerspective == null) {
			applicationPerspective = perspectiveSessionData.getPerspectiveBuilder().build(perspectiveSessionData, null);
			applicationPerspectiveByPerspectiveBuilder.put(perspectiveSessionData, applicationPerspective);
			responsiveApplication.addPerspective(applicationPerspective.getPerspective());

			if (applicationPerspective.getPerspectiveMenuPanel() != null) {
				if (perspectiveSessionData.getManagedApplicationPerspective().isToolbarPerspectiveMenu()) {
					Component perspectiveMenuPanel = applicationPerspective.getPerspectiveMenuPanel();
					perspectiveMenuPanel.setCssStyle("height","300px");
					if (perspectiveMenuPanel instanceof Tree) {
						Tree menu = (Tree) perspectiveMenuPanel;
						menu.onNodeSelected.addListener(() -> {

						});
					}
				} else if (mobileLayout == null) {
					View perspectiveMenuView = View.createView(StandardLayout.LEFT, ApplicationIcons.RADIO_BUTTON_GROUP, getLocalized(Dictionary.APPLICATION_MENU), null);
					applicationPerspective.getPerspective().addView(perspectiveMenuView);
					perspectiveMenuView.setComponent(applicationPerspective.getPerspectiveMenuPanel());
				}
			}
		} else {
			applicationPerspective.getOnPerspectiveRefreshRequested().fire();
		}
		if (applicationMenuTree != null && !perspectiveSessionData.equals(applicationMenuTree.getSelectedNode())) {
			applicationMenuTree.setSelectedNode(perspectiveSessionData);
		}
		if (applicationPerspective.getPerspectiveMenuPanel() != null && perspectiveSessionData.getManagedApplicationPerspective().getToolbarPerspectiveMenu()) {
			perspectiveSessionData.getManagedApplicationSessionData().setPerspectiveToolbarMenuComponent(applicationPerspective.getPerspectiveToolbarMenuPanel() != null ? applicationPerspective.getPerspectiveToolbarMenuPanel() : applicationPerspective.getPerspectiveMenuPanel());
		} else {
			perspectiveSessionData.getManagedApplicationSessionData().setPerspectiveToolbarMenuComponent(null);
		}
		responsiveApplication.showPerspective(applicationPerspective.getPerspective());

		if (mobileLayout != null && applicationPerspective.getPerspectiveMenuPanel() != null && !perspectiveSessionData.getManagedApplicationPerspective().getToolbarPerspectiveMenu()) {
			backButton.setVisible(true);
			mobileLayout.setContent(applicationPerspective.getPerspectiveMenuPanel(), PageTransition.MOVE_TO_LEFT_VS_MOVE_FROM_RIGHT, 500);
		}
		if (applicationPerspective instanceof AbstractManagedApplicationPerspective) {
			AbstractManagedApplicationPerspective managedApplicationPerspective = (AbstractManagedApplicationPerspective) applicationPerspective;
			managedApplicationPerspective.onPerspectiveInitialized.fire();
		}

		return applicationPerspective;
	}

	private ApplicationPerspective showMobilePerspective(PerspectiveSessionData perspectiveSessionData) {
		ManagedApplicationPerspective currentPerspective = perspectiveSessionData.getManagedApplicationPerspective();
		selectedPerspective.set(currentPerspective);
		ApplicationLauncher.THREAD_LOCAL_MANAGED_PERSPECTIVE.set(currentPerspective);
		LOGGER.info("Open perspective");
		ResponsiveApplication responsiveApplication = perspectiveSessionData.getManagedApplicationSessionData().getResponsiveApplication();
		ApplicationPerspective applicationPerspective = applicationPerspectiveByPerspectiveBuilder.get(perspectiveSessionData);
		if (applicationPerspective == null) {
			applicationPerspective = perspectiveSessionData.getPerspectiveBuilder().build(perspectiveSessionData, null);
			applicationPerspectiveByPerspectiveBuilder.put(perspectiveSessionData, applicationPerspective);
			responsiveApplication.addPerspective(applicationPerspective.getPerspective());
		} else {
			applicationPerspective.getOnPerspectiveRefreshRequested().fire();
		}
		Perspective perspective = applicationPerspective.getPerspective();
		if (perspective.getFocusedView() == null && applicationPerspective.getPerspectiveMenuPanel() != null) {
			perspective.setFocusedView(mobileApplicationMenu);
		}
		responsiveApplication.showPerspective(perspective);
		if (applicationPerspective.getPerspectiveMenuPanel() != null) {
			if (mobileApplicationMenu.equals(perspective.getFocusedView())) {
				mobileLayout.setContent(applicationPerspective.getPerspectiveMenuPanel(), PageTransition.MOVE_TO_LEFT_VS_MOVE_FROM_RIGHT, 500);
			} else {
				mobileLayout.setContent(applicationPerspective.getPerspectiveMenuPanel());
			}
			perspectiveSessionData.getManagedApplicationSessionData().getMobileNavigation().setBackOperationAvailable(true);
		}

		return applicationPerspective;
	}

	private Tree<PerspectiveSessionData> createApplicationMenuTree(List<PerspectiveSessionData> sortedPerspectives) {
		ListTreeModel<PerspectiveSessionData> treeModel = new ListTreeModel<>(sortedPerspectives);
		Tree<PerspectiveSessionData> tree = new Tree<>(treeModel);
		tree.setShowExpanders(false);
		tree.setEntryTemplate(BaseTemplate.LIST_ITEM_VERY_LARGE_ICON_TWO_LINES);
		tree.setPropertyExtractor((perspectiveSessionData, propertyName) -> {
			switch (propertyName) {
				case BaseTemplate.PROPERTY_BADGE:
					return null; //todo
				case BaseTemplate.PROPERTY_ICON:
					return perspectiveSessionData.getIcon();
				case BaseTemplate.PROPERTY_CAPTION:
					return perspectiveSessionData.getTitle();
				case BaseTemplate.PROPERTY_DESCRIPTION:
					return perspectiveSessionData.getDescription();
				default:
					return null;
			}
		});
		return tree;
	}

	private SimpleItemView<PerspectiveSessionData> createApplicationMenu(List<PerspectiveSessionData> sortedPerspectives) {
		SimpleItemView<PerspectiveSessionData> itemView = new SimpleItemView<>();
		SimpleItemGroup<PerspectiveSessionData> itemGroup = itemView.addSingleColumnGroup(ApplicationIcons.WINDOWS, getLocalized(Dictionary.APPLICATION_PERSPECTIVE));
		itemGroup.setItemTemplate(BaseTemplate.LIST_ITEM_VERY_LARGE_ICON_TWO_LINES);
		sortedPerspectives.forEach(p -> {
			itemGroup.addItem(p.getIcon(), p.getTitle(), p.getDescription()).setPayload(p);
		});
		return itemView;
	}

	public String getLocalized(String key, Object... objects) {
		return userSessionData.getLocalizationProvider().getLocalized(key, objects);
	}


}
