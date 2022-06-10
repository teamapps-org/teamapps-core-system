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

import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.common.format.Color;
import org.teamapps.icon.material.MaterialIcon;
import org.teamapps.icons.Icon;
import org.teamapps.ux.application.ResponsiveApplication;
import org.teamapps.ux.application.ResponsiveApplicationToolbar;
import org.teamapps.ux.application.assembler.ApplicationAssembler;
import org.teamapps.ux.application.perspective.Perspective;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.application.view.ViewSize;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.animation.PageTransition;
import org.teamapps.ux.component.calendar.Calendar;
import org.teamapps.ux.component.charting.forcelayout.ForceLayoutGraph;
import org.teamapps.ux.component.flexcontainer.VerticalLayout;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.infiniteitemview.InfiniteItemView2;
import org.teamapps.ux.component.itemview.ItemView;
import org.teamapps.ux.component.itemview.SimpleItem;
import org.teamapps.ux.component.itemview.SimpleItemGroup;
import org.teamapps.ux.component.itemview.SimpleItemView;
import org.teamapps.ux.component.map.MapView;
import org.teamapps.ux.component.mobile.MobileLayout;
import org.teamapps.ux.component.panel.Panel;
import org.teamapps.ux.component.progress.MultiProgressDisplay;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.template.BaseTemplateRecord;
import org.teamapps.ux.component.timegraph.TimeGraph;
import org.teamapps.ux.component.toolbar.AbstractToolContainer;
import org.teamapps.ux.component.toolbar.Toolbar;
import org.teamapps.ux.component.toolbar.ToolbarButton;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;
import org.teamapps.ux.component.tree.Tree;
import org.teamapps.ux.component.workspacelayout.definition.LayoutItemDefinition;

import java.util.ArrayList;
import java.util.List;

public class MobileAssembler implements ApplicationAssembler {

	private static final int PAGE_TRANSITION_ANIMATION_DURATION = 300;

	private final ApplicationLocalizationProvider localizationProvider;
	private final MobileNavigation mobileNavigation;
	private VerticalLayout verticalLayout;
	private Toolbar navigationToolbar;
	private final MobileLayout mobileLayout;
	private AbstractToolContainer mainToolbar;
	private SimpleItemView<Void> viewsItemView;
	private final List<View> applicationViews = new ArrayList<>();
	private List<View> perspectiveViews = new ArrayList<>();
	private final ToolbarButtonGroup centerGroup;
	private final ToolbarButtonGroup leftGroup;
	private final ToolbarButtonGroup rightGroup;
	private final ToolbarButton navigationToolbarMenuButton;
	private View activeView;


	public MobileAssembler(MobileNavigation mobileNavigation, ApplicationLocalizationProvider localizationProvider) {
		this.localizationProvider = localizationProvider;
		this.mobileNavigation = mobileNavigation;
		verticalLayout = new VerticalLayout();
		mobileLayout = new MobileLayout();
		viewsItemView = new SimpleItemView<>();
		navigationToolbar = new Toolbar();
		verticalLayout.addComponentAutoSize(navigationToolbar);
		verticalLayout.addComponentFillRemaining(mobileLayout);
		navigationToolbar.setBackgroundColor(Color.WHITE.withAlpha(0.6f));

		leftGroup = navigationToolbar.addButtonGroup(new ToolbarButtonGroup());
		ToolbarButton backButton = new ToolbarButton(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, new BaseTemplateRecord<>(MaterialIcon.NAVIGATE_BEFORE, getLocalized(Dictionary.BACK)));
		leftGroup.addButton(backButton);
		leftGroup.setShowGroupSeparator(false);
		backButton.onClick.addListener(() -> {
			if (activeView != null && activeView.equals(mobileNavigation.getApplicationMenuView()) && mobileNavigation.isBackOperationAvailable()) {
				mobileNavigation.fireBackOperation();
			} else {
				goBack();
			}
		});

		mobileNavigation.onShowStartViewRequest().addListener(() -> {
			setNavigationToolbarVisible(true);
			if (!perspectiveViews.isEmpty()) {
				showView(perspectiveViews.get(0));
			} else {
				showView(mobileNavigation.getApplicationMenuView());
			}
		});

		mobileNavigation.onShowViewRequest().addListener(view -> {
			setNavigationToolbarVisible(true);
			showView(view);
		});

		centerGroup = navigationToolbar.addButtonGroup(new ToolbarButtonGroup());
		//todo workaround until distribute option is available
		centerGroup.addButton(new ToolbarButton(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, new BaseTemplateRecord<>("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")));
		centerGroup.setShowGroupSeparator(false);
		ToolbarButton viewsButton = new ToolbarButton(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, new BaseTemplateRecord<>(MaterialIcon.TAB, getLocalized(Dictionary.VIEWS)));

		viewsButton.setDropDownComponent(viewsItemView);
		viewsButton.onClick.addListener(this::updateViewsDropdown);
		viewsButton.setDroDownPanelWidth(450);
		centerGroup.addButton(viewsButton);

		rightGroup = navigationToolbar.addButtonGroup(new ToolbarButtonGroup());
		rightGroup.setRightSide(true);
		rightGroup.setShowGroupSeparator(false);
		navigationToolbarMenuButton = new ToolbarButton(BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE, new BaseTemplateRecord<>(MaterialIcon.MENU, getLocalized(Dictionary.MENU)));
		rightGroup.addButton(navigationToolbarMenuButton);

		setNavigationToolbarVisible(false);
	}

	private String getLocalized(String key) {
		return localizationProvider.getLocalized(key);
	}

	private void setNavigationToolbarVisible(boolean visible) {
		navigationToolbar.setVisible(visible);
		leftGroup.setVisible(visible);
		centerGroup.setVisible(visible);
		rightGroup.setVisible(visible);
	}


	public void goBack() {

		View view = null;
		for (int i = 0; i < perspectiveViews.size(); i++) {
			if (perspectiveViews.get(i).equals(activeView)) {
				if (i > 0) {
					view = perspectiveViews.get(i - 1);
				} else if (!applicationViews.isEmpty()) {
					view = applicationViews.get(applicationViews.size() - 1);
				}
				break;
			}
		}
		if (view == null) {
			for (int i = 0; i < applicationViews.size(); i++) {
				if (applicationViews.get(i).equals(activeView)) {
					if (i > 0) {
						view = applicationViews.get(i - 1);
					}
					break;
				}
			}
		}
		activeView = view;
		if (activeView == null) {
			setNavigationToolbarVisible(false);
			mobileLayout.setContent(mobileNavigation.getApplicationLauncher(), PageTransition.MOVE_TO_RIGHT_VS_MOVE_FROM_LEFT, PAGE_TRANSITION_ANIMATION_DURATION);
		} else {
			setNavigationToolbarVisible(true);
			mobileLayout.setContent(activeView.getPanel(), PageTransition.MOVE_TO_RIGHT_VS_MOVE_FROM_LEFT, PAGE_TRANSITION_ANIMATION_DURATION);
		}
	}

	public void showView(View view) {
		if (view == null) {
			activeView = view;
			setNavigationToolbarVisible(false);
			mobileLayout.setContent(mobileNavigation.getApplicationLauncher(), PageTransition.MOVE_TO_RIGHT_VS_MOVE_FROM_LEFT, PAGE_TRANSITION_ANIMATION_DURATION);
			return;
		}

		if (view.equals(activeView)) {
			return;
		}

		List<View> views = new ArrayList<>(applicationViews);
		views.addAll(perspectiveViews);
		int lastPos = -1;
		int newPos = 0;
		for (int i = 0; i < views.size(); i++) {
			if (activeView != null && activeView.equals(views.get(i))) {
				lastPos = i;
			}
			if (view.equals(views.get(i))) {
				newPos = i;
			}
		}

		if (newPos > lastPos) {
			mobileLayout.setContent(view.getPanel(), PageTransition.MOVE_TO_LEFT_VS_MOVE_FROM_RIGHT, PAGE_TRANSITION_ANIMATION_DURATION);
		} else {
			mobileLayout.setContent(view.getPanel(), PageTransition.MOVE_TO_RIGHT_VS_MOVE_FROM_LEFT, PAGE_TRANSITION_ANIMATION_DURATION);
		}
		activeView = view;
	}

	public void showInitialView() {
		View view = null;
		if (!applicationViews.isEmpty()) {
			view = applicationViews.get(0);
		} else if (!perspectiveViews.isEmpty()) {
			view = perspectiveViews.get(0);
		}
		if (view != null) {
			mobileLayout.setContent(view.getPanel(), PageTransition.MOVE_TO_LEFT_VS_MOVE_FROM_RIGHT, PAGE_TRANSITION_ANIMATION_DURATION);
			activeView = view;
		}
	}

	@Override
	public void setWorkSpaceToolbar(ResponsiveApplicationToolbar toolbar) {
		mainToolbar = toolbar.getToolbar();
		navigationToolbarMenuButton.setDropDownComponent(mainToolbar);
		mainToolbar.onButtonClick.addListener(() -> {
			//todo hide drop down panel
		});
	}

	@Override
	public Component createApplication(ResponsiveApplication application) {
		return verticalLayout;
	}

	@Override
	public MultiProgressDisplay getMultiProgressDisplay() {
		return null;
	}

	@Override
	public void handleApplicationViewAdded(ResponsiveApplication application, View view) {
		applicationViews.add(view);
	}

	@Override
	public void handleApplicationViewRemoved(ResponsiveApplication application, View view) {
		applicationViews.remove(view);
	}

	private void updateViewsDropdown() {
		viewsItemView.removeAllGroups();
		SimpleItemGroup<Void> appGroup = viewsItemView.addSingleColumnGroup(ApplicationIcons.WINDOWS, getLocalized(Dictionary.APPLICATIONS));
		appGroup.setItemTemplate(BaseTemplate.LIST_ITEM_VERY_LARGE_ICON_TWO_LINES);

		appGroup.addItem(ApplicationIcons.WINDOW_EXPLORER, getLocalized(Dictionary.APPLICATION_LAUNCHER), getLocalized(Dictionary.OPEN_NEW_APPLICATION)).onClick.addListener(() -> {
			showView(null);
		});
		applicationViews.forEach(view -> appGroup.addItem(createViewButton(view)));
		SimpleItemGroup<Void> viewGroup = viewsItemView.addSingleColumnGroup(ApplicationIcons.WINDOWS, getLocalized(Dictionary.APPLICATIONS));
		viewGroup.setItemTemplate(BaseTemplate.LIST_ITEM_VERY_LARGE_ICON_TWO_LINES);
		perspectiveViews.forEach(view -> viewGroup.addItem(createViewButton(view)));
	}

	@Override
	public void handlePerspectiveChange(ResponsiveApplication application, Perspective perspective, Perspective previousPerspective, List<View> activeViews, List<View> addedViews, List<View> removedViews) {
		addedViews.forEach(view -> mobileLayout.preloadView(view.getPanel()));
		perspectiveViews = activeViews;

		if (!perspectiveViews.isEmpty()) {
			View view = perspective.getFocusedView() != null ? perspective.getFocusedView() : perspectiveViews.get(0);
			activeView = view;
			mobileLayout.setContent(view.getPanel(), PageTransition.MOVE_TO_LEFT_VS_MOVE_FROM_RIGHT, PAGE_TRANSITION_ANIMATION_DURATION);
			setNavigationToolbarVisible(true);
		}
	}


	private SimpleItem<Void> createViewButton(View view) {
		SimpleItem<Void> item;
		if (view.equals(mobileNavigation.getApplicationMenuView())) {
			item = new SimpleItem<>(ApplicationIcons.WINDOW_SIDEBAR, getLocalized(Dictionary.APPLICATION_MENU), getLocalized(Dictionary.SELECT_APPLICATION_PERSPECTIVE));
		} else {
			item = getViewTypeItem(view.getPanel());
		}
		item.onClick.addListener(() -> showView(view));
		return item;
	}

	private SimpleItem<Void> getViewTypeItem(Panel panel) {
		String title = panel.getTitle();
		Icon icon = panel.getIcon();
		Component content = panel.getContent();
		if (content == null) {
			return new SimpleItem<>(panel.getIcon(), panel.getTitle(), null);
		}
		if (content instanceof Table) {
			return new SimpleItem<>(icon != null ? icon : ApplicationIcons.SPREADSHEET, title, getLocalized(Dictionary.TABLE));
		}
		if (content instanceof ItemView || content instanceof InfiniteItemView2) {
			return new SimpleItem<>(icon != null ? icon : ApplicationIcons.LIST_STYLE_BULLETS, title, getLocalized(Dictionary.LIST));
		}
		if (content instanceof ResponsiveForm) {
			return new SimpleItem<>(icon != null ? icon : ApplicationIcons.FORM, title, getLocalized(Dictionary.FORM));
		}
		if (content instanceof Calendar) {
			return new SimpleItem<>(icon != null ? icon : ApplicationIcons.CALENDAR, title, getLocalized(Dictionary.CALENDAR));
		}
		if (content instanceof TimeGraph) {
			return new SimpleItem<>(icon != null ? icon : ApplicationIcons.CHART_LINE, title, getLocalized(Dictionary.TIMELINE));
		}
		if (content instanceof Tree) {
			return new SimpleItem<>(icon != null ? icon : ApplicationIcons.TEXT_TREE, title, getLocalized(Dictionary.TREE));
		}
		if (content instanceof MapView) {
			return new SimpleItem<>(icon != null ? icon : ApplicationIcons.MAP, title, getLocalized(Dictionary.MAP));
		}
		if (content instanceof ForceLayoutGraph) {
			return new SimpleItem<>(icon != null ? icon : ApplicationIcons.GRAPH_CONNECTION_DIRECTED, title, getLocalized(Dictionary.NETWORK));
		}
		return new SimpleItem<>(panel.getIcon(), panel.getTitle(), null);
	}


	@Override
	public void handleLayoutChange(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, LayoutItemDefinition layout) {

	}

	@Override
	public void handleViewAdded(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view) {
		if (isActivePerspective) {
			mobileLayout.preloadView(view.getPanel());
			perspectiveViews.add(view);
		}
	}

	@Override
	public void handleViewRemoved(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view) {
		if (isActivePerspective) {
			perspectiveViews.remove(view);
		}
	}

	@Override
	public void handleViewVisibilityChange(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view, boolean visible) {
		if (isActivePerspective) {

		}
	}

	@Override
	public void handleViewFocusRequest(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view, boolean ensureVisible) {
		if (isActivePerspective) {
			showView(view);
		}
	}

	@Override
	public void handleViewSizeChange(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view, ViewSize viewSize) {

	}

	@Override
	public void handleViewTabTitleChange(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view, String title) {

	}

	@Override
	public void handleViewLayoutPositionChange(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view, String position) {

	}

	@Override
	public void handleApplicationToolbarButtonGroupAdded(ResponsiveApplication application, ToolbarButtonGroup buttonGroup) {
	}
}
