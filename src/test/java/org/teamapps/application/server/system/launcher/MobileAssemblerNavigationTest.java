package org.teamapps.application.server.system.launcher;

import org.junit.Test;
import org.teamapps.application.api.localization.ApplicationLocalizationProvider;
import org.teamapps.dto.UiMobileLayout;
import org.teamapps.testutil.UxTestUtil;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.application.perspective.Perspective;
import org.teamapps.ux.component.flexcontainer.VerticalLayout;
import org.teamapps.ux.component.mobile.MobileLayout;
import org.teamapps.ux.component.panel.Panel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class MobileAssemblerNavigationTest {
	@Test
	public void repeatedViewSwitchesStillReturnDirectlyToMenuAndHome() {
		UxTestUtil.doWithMockedSessionContext(() -> {
			Fixture f = new Fixture();
			for (int i = 0; i < 30; i++) { f.assembler.showView(f.master); f.assembler.showView(f.detail); }
			assertEquals(4, f.state().getNavigationState().getEntries().size());
			f.assembler.navigateBack(); assertSame(f.master.getPanel(), f.layout.getContent());
			f.assembler.navigateBack(); assertSame(f.menu.getPanel(), f.layout.getContent());
			f.assembler.navigateBack(); assertSame(f.home, f.layout.getContent());
			f.request(f.menu.getPanel().getId()); assertSame(f.menu.getPanel(), f.layout.getContent());
			f.request(f.master.getPanel().getId()); assertSame(f.master.getPanel(), f.layout.getContent());
		}).join();
	}

	@Test
	public void browserAndSwipeRequestsUseNestedMenuBackAndCanRestoreTheSubmenu() {
		UxTestUtil.doWithMockedSessionContext(() -> {
			Fixture f = new Fixture();
			AtomicInteger backwards = new AtomicInteger(), forwards = new AtomicInteger();
			f.navigation.onBackOperation.addListener(backwards::incrementAndGet);
			f.navigation.setForwardOperation(forwards::incrementAndGet);
			f.navigation.setBackOperationAvailable(true);
			f.assembler.showView(f.detail);
			f.request(f.menu.getPanel().getId()); // Browser history menu can jump over multiple slots.
			assertSame(f.menu.getPanel(), f.layout.getContent());
			assertEquals(1, backwards.get());
			assertFalse(f.navigation.isBackOperationAvailable());
			f.request(f.menu.getPanel().getId() + ":submenu");
			assertEquals(1, forwards.get());
			assertTrue(f.navigation.isBackOperationAvailable());
			f.request("home");
			assertEquals(2, backwards.get());
			assertSame(f.home, f.layout.getContent());
		}).join();
	}

	@Test
	public void directGoBackKeepsItsOriginalContractAndUnknownTargetsDoNothing() {
		UxTestUtil.doWithMockedSessionContext(() -> {
			Fixture f = new Fixture();
			f.navigation.setBackOperationAvailable(true);
			f.assembler.showView(f.menu);
			f.assembler.navigateToEntry("unknown"); assertSame(f.menu.getPanel(), f.layout.getContent());
			f.assembler.goBack(); assertSame(f.home, f.layout.getContent());
			assertTrue(f.navigation.isBackOperationAvailable());
		}).join();
	}

	@Test
	public void perspectiveViewsUseTheExistingOrderAndRemovedActiveViewsRejectNavigation() {
		UxTestUtil.doWithMockedSessionContext(() -> {
			Fixture f = new Fixture();
			f.assembler.handleApplicationViewRemoved(null, f.master);
			f.assembler.handleApplicationViewRemoved(null, f.detail);
			Perspective perspective = Perspective.createPerspective();
			perspective.addViews(f.master, f.detail);
			f.assembler.handlePerspectiveChange(null, perspective, null, new ArrayList<>(List.of(f.master, f.detail)), List.of(), List.of());
			f.assembler.showView(f.detail);
			f.request(f.master.getPanel().getId()); assertSame(f.master.getPanel(), f.layout.getContent());
			f.request(f.menu.getPanel().getId()); assertSame(f.menu.getPanel(), f.layout.getContent());
			f.assembler.showView(f.detail);
			f.assembler.handleViewRemoved(null, true, perspective, f.detail);
			assertNull(f.state().getNavigationState());
			assertSame(f.detail.getPanel(), f.layout.getContent()); // Preserve the original removal contract.
		}).join();
	}

	private static class Fixture {
		final MobileApplicationNavigation navigation = new MobileApplicationNavigation();
		final Panel home = new Panel();
		final View menu = View.createView(), master = View.createView(), detail = View.createView();
		final MobileAssembler assembler;
		final MobileLayout layout;
		Fixture() {
			navigation.setApplicationLauncher(home); navigation.setApplicationMenu(menu);
			assembler = new MobileAssembler(navigation, mock(ApplicationLocalizationProvider.class));
			layout = (MobileLayout) ((VerticalLayout) assembler.createApplication(null)).getComponents().get(1);
			assembler.handleApplicationViewAdded(null, menu);
			assembler.handleApplicationViewAdded(null, master);
			assembler.handleApplicationViewAdded(null, detail);
		}
		UiMobileLayout state() { return (UiMobileLayout) layout.createUiComponent(); }
		void request(String id) {
			layout.handleUiEvent(new UiMobileLayout.NavigationRequestedEvent(layout.getId(), state().getNavigationState().getRevision(), id));
		}
	}
}
