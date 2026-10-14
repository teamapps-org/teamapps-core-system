package org.teamapps.application.server.system.launcher;

import org.junit.Test;
import org.teamapps.application.api.application.ApplicationBuilder;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.application.WindowApplicationBuilder;
import org.teamapps.application.api.application.perspective.PerspectiveBuilder;
import org.teamapps.application.api.privilege.ApplicationPrivilegeProvider;
import org.teamapps.application.server.system.bootstrap.LoadedApplication;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.privilege.PrivilegeApplicationKey;
import org.teamapps.application.server.system.session.ManagedApplicationSessionData;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.model.controlcenter.Application;
import org.teamapps.model.controlcenter.ApplicationPerspective;
import org.teamapps.model.controlcenter.ManagedApplication;
import org.teamapps.model.controlcenter.ManagedApplicationPerspective;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WindowApplicationLauncherTest {

    @Test
    public void ordinaryApplicationsStillRequireAnAccessibleProvisionedPerspective() throws Exception {
        Fixture f = new Fixture();
        ApplicationBuilder builder = mock(ApplicationBuilder.class);
        when(f.loaded.getBaseApplicationBuilder()).thenReturn(builder);
        when(builder.isApplicationAccessible(any())).thenReturn(true);
        assertFalse(f.accessible());

        // A foreign provisioned perspective grants visibility independently of the old app gate.
        Application owner = mock(Application.class);
        LoadedApplication foreignApp = mock(LoadedApplication.class);
        ApplicationPerspective definition = mock(ApplicationPerspective.class);
        ManagedApplicationPerspective provisioned = mock(ManagedApplicationPerspective.class);
        PerspectiveBuilder perspective = mock(PerspectiveBuilder.class);
        when(f.managed.getPerspectives()).thenReturn(List.of(provisioned));
        when(provisioned.getManagedApplication()).thenReturn(f.managed);
        when(provisioned.getApplicationPerspective()).thenReturn(definition);
        when(definition.getApplication()).thenReturn(owner);
        when(definition.getName()).thenReturn("foreignPerspective");
        when(f.registry.getLoadedApplication(owner)).thenReturn(foreignApp);
        when(foreignApp.getPerspectiveBuilder("foreignPerspective")).thenReturn(perspective);
        when(perspective.isPerspectiveAccessible(f.privileges)).thenReturn(true);
        when(builder.isApplicationAccessible(any())).thenReturn(false);
        assertTrue(f.accessible());
        when(perspective.isPerspectiveAccessible(f.privileges)).thenReturn(false);
        assertFalse(f.accessible());
        verify(builder, never()).isApplicationAccessible(any());
    }

    @Test
    public void windowAccessIsExplicitAndHiddenOrUnloadedEntriesRemainHidden() throws Exception {
        Fixture f = new Fixture();
        WindowApplicationBuilder builder = f.windowBuilder();
        assertFalse(f.accessible());
        when(builder.isWindowAccessible(f.privileges)).thenReturn(true);
        assertTrue(f.accessible());
        when(f.managed.isHidden()).thenReturn(true);
        assertFalse(f.accessible());
        when(f.managed.isHidden()).thenReturn(false);
        when(f.registry.getLoadedApplication(f.application)).thenReturn(null);
        assertFalse(f.accessible());
    }

    @Test
    public void repeatedWindowLaunchesDoNotSelectAnApplicationOrCreateDesktopOrMobileNavigation() throws Exception {
        for (boolean mobile : List.of(false, true)) {
            Fixture f = new Fixture();
            f.set("mobileView", mobile);
            WindowApplicationBuilder builder = f.windowBuilder();
            when(builder.isWindowAccessible(f.privileges)).thenReturn(true);
            ApplicationData data = mock(ApplicationData.class);
            ManagedApplicationSessionData session = mock(ManagedApplicationSessionData.class);
            ApplicationInstanceData context = mock(ApplicationInstanceData.class);
            when(data.getLoadedApplication()).thenReturn(f.loaded);
            when(data.getManagedApplication()).thenReturn(f.managed);
            when(data.getApplicationSessionData()).thenReturn(session);
            when(session.getUnmanagedApplicationData()).thenReturn(context);

            f.open(data);
            f.open(data);
            verify(builder, times(2)).showWindow(context);
            verify(f.user, never()).addOpenApplicationsCount();
            verify(f.user, never()).setRootComponent(any());
            verify(f.user, never()).setDarkTheme(anyBoolean());
            // Selection, tab maps and responsive application are deliberately absent:
            // touching the ordinary launch path would fail this test.

            when(builder.isWindowAccessible(f.privileges)).thenReturn(false);
            f.open(data);
            verify(builder, times(2)).showWindow(context);
            verify(session, times(2)).getUnmanagedApplicationData();
        }
    }

    private static class Fixture {
        final ApplicationLauncher launcher = mock(ApplicationLauncher.class, CALLS_REAL_METHODS);
        final SystemRegistry registry = mock(SystemRegistry.class);
        final UserSessionData user = mock(UserSessionData.class, RETURNS_DEEP_STUBS);
        final Application application = mock(Application.class);
        final ManagedApplication managed = mock(ManagedApplication.class);
        final LoadedApplication loaded = mock(LoadedApplication.class);
        final ApplicationPrivilegeProvider privileges = mock(ApplicationPrivilegeProvider.class);

        Fixture() throws Exception {
            set("registry", registry);
            set("userSessionData", user);
            when(managed.getMainApplication()).thenReturn(application);
            when(managed.getPerspectives()).thenReturn(List.of());
            when(registry.getLoadedApplication(application)).thenReturn(loaded);
            when(user.getUserPrivileges().getApplicationPrivilegeProvider(any(PrivilegeApplicationKey.class))).thenReturn(privileges);
        }

        WindowApplicationBuilder windowBuilder() {
            WindowApplicationBuilder builder = mock(WindowApplicationBuilder.class);
            when(loaded.getBaseApplicationBuilder()).thenReturn(builder);
            return builder;
        }

        void set(String name, Object value) throws Exception {
            Field field = ApplicationLauncher.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(launcher, value);
        }

        boolean accessible() throws Exception {
            Method method = ApplicationLauncher.class.getDeclaredMethod("isApplicationAccessible", ManagedApplication.class);
            method.setAccessible(true);
            return (boolean) method.invoke(launcher, managed);
        }

        void open(ApplicationData data) throws Exception {
            Method method = ApplicationLauncher.class.getDeclaredMethod("openApplication", ApplicationData.class);
            method.setAccessible(true);
            method.invoke(launcher, data);
        }
    }
}
