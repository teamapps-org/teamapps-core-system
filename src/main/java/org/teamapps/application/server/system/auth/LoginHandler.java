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
package org.teamapps.application.server.system.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.password.SecurePasswordHash;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.server.system.bootstrap.LogoutHandler;
import org.teamapps.application.server.system.bootstrap.SystemRegistry;
import org.teamapps.application.server.system.launcher.ApplicationLauncher;
import org.teamapps.application.server.system.session.UserSessionData;
import org.teamapps.application.server.system.template.PropertyProviders;
import org.teamapps.application.server.system.template.Templates;
import org.teamapps.common.format.Color;
import org.teamapps.model.controlcenter.User;
import org.teamapps.model.controlcenter.UserAccessToken;
import org.teamapps.model.controlcenter.UserAccountStatus;
import org.teamapps.universaldb.UniversalDB;
import org.teamapps.universaldb.index.enumeration.EnumFilterType;
import org.teamapps.universaldb.index.text.TextFilter;
import org.teamapps.ux.component.field.*;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.format.HorizontalElementAlignment;
import org.teamapps.ux.component.format.Spacing;
import org.teamapps.ux.component.infiniteitemview.InfiniteItemView;
import org.teamapps.ux.component.infiniteitemview.ListInfiniteItemViewModel;
import org.teamapps.ux.component.itemview.ItemViewRowJustification;
import org.teamapps.ux.component.itemview.ItemViewVerticalItemAlignment;
import org.teamapps.ux.component.linkbutton.LinkButton;
import org.teamapps.ux.component.panel.ElegantPanel;
import org.teamapps.ux.component.panel.Panel;
import org.teamapps.ux.component.rootpanel.RootPanel;
import org.teamapps.ux.session.SessionContext;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.*;

public class LoginHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final SystemRegistry systemRegistry;
	private final LogoutHandler logoutHandler;
	private List<String> rankedLanguages;

	public LoginHandler(SystemRegistry systemRegistry, LogoutHandler logoutHandler) {
		this.systemRegistry = systemRegistry;
		this.logoutHandler = logoutHandler;
	}

	public LoginHandler(SystemRegistry systemRegistry, LogoutHandler logoutHandler, UserSessionData userSessionData) {
		this.systemRegistry = systemRegistry;
		this.logoutHandler = logoutHandler;
		this.rankedLanguages = userSessionData.getRankedLanguages();
	}

	public void handleNewSession(SessionContext context) {
		if (rankedLanguages == null) {
			rankedLanguages = new ArrayList<>();
			rankedLanguages.add(context.getLocale().getLanguage());
			rankedLanguages.add("en");
		}
		RootPanel rootPanel = context.addRootPanel();

		Map<String, Object> clientParameters = context.getClientInfo().getClientParameters();
		if (clientParameters != null && clientParameters.containsKey("ATOK")) {
			for (AuthenticationHandler authenticationHandler : systemRegistry.getAuthenticationHandlers()) {
				User authenticatedUser = authenticationHandler.authenticate(context, clientParameters);
				if (authenticatedUser != null) {
					rootPanel.setContent(new Panel());
					handleSuccessfulLogin(authenticatedUser, rootPanel, context);
					return;
				}
			}
		}
		createLoginView(context, rootPanel);
	}

	/*
		Login options to implement:
			Authentication:
				-allow client secure token
				-wrong password limiter: seconds -> login progress
			Registration:
				-allow unknown users
				-auto confirm new users
			Password reset
				-allow sms password reset
				-allow email password reset
			Accept policies:
				Privacy policy
				Terms of use
	 */

	public void createLoginView(SessionContext context, RootPanel rootPanel) {
		String backgroundUrl = systemRegistry.getSystemConfig().getThemingConfig().getLoginBackgroundUrl();
		context.registerBackgroundImage("login", backgroundUrl, backgroundUrl);
		context.setBackgroundImage("login", 0);

		List<UserAccessToken> secureLoginTokens = getSecureLoginTokens();
		if (!secureLoginTokens.isEmpty()) {
			showSecureTokenLogin(secureLoginTokens, context, rootPanel);
		} else {
			showPasswordLogin(context, rootPanel);
		}
		context.setBackgroundImage("login", 0);
	}

	private User authenticate(String login, String password) {
		if (login == null || login.isBlank() || password == null || password.isBlank()) {
			return null;
		} else {
			User user = User.filter()
					.login(TextFilter.textEqualsIgnoreCaseFilter(login))
					.userAccountStatus(EnumFilterType.NOT_EQUALS, UserAccountStatus.INACTIVE)
					.executeExpectSingleton();
			if (user != null) {
				String hash = user.getPassword();
				if (SecurePasswordHash.createDefault().verifyPassword(password, hash)) {
					return user;
				}
			}
		}
		return null;
	}

	private void showPasswordLogin(SessionContext context, RootPanel rootPanel) {
		ElegantPanel elegantPanel = new ElegantPanel();
		elegantPanel.setMaxContentWidth(450);

		ResponsiveForm<?> loginForm = new ResponsiveForm<>(120, 150, 0);
		elegantPanel.setContent(loginForm);
		ResponsiveFormLayout formLayout = loginForm.addResponsiveFormLayout(400);

		DisplayField headerField = new DisplayField(false, true);
		headerField.setValue("<span style='font-size:150%'>" + getLocalized(Dictionary.LOGIN) + "</span>");

		DisplayField errorField = new DisplayField(false, true);
		errorField.setValue("<span style='font-size:120%;color:#961900'>&nbsp;</span>");

		TextField loginField = new TextField();
		PasswordField passwordField = new PasswordField();
		CheckBox stayLoggedIn = new CheckBox(getLocalized(Dictionary.KEEP_ME_LOGGED_INTHIS_PC_IS_SECURE));
		stayLoggedIn.setCheckColor(Color.MATERIAL_BLUE_400);
		loginField.setAutofill(true);
		passwordField.setAutofill(true);

		Button buttonLogin = Button.create(getLocalized(Dictionary.LOGIN));
		buttonLogin.setColor(Color.WHITE.withAlpha(1f));


		LinkButton buttonResetPassword = new LinkButton(getLocalized(Dictionary.RESET_PASSWORD));
		buttonResetPassword.setCssStyle("color", Color.MATERIAL_GREY_900.toHtmlColorString());


		LinkButton buttonRegister = new LinkButton(getLocalized(Dictionary.REGISTER));
		buttonRegister.setCssStyle("color", Color.MATERIAL_GREY_900.toHtmlColorString());

		formLayout.addSection().setDrawHeaderLine(false).setPadding(new Spacing(0, 0, 0, 0)).setCollapsible(false);
		formLayout.addLabelField(headerField).setColSpan(2);
		formLayout.addLabelField(errorField).setColSpan(2);
		formLayout.addLabelAndField(ApplicationIcons.USER, getLocalized(Dictionary.USER_NAME), loginField);
		formLayout.addLabelAndField(ApplicationIcons.KEY, getLocalized(Dictionary.PASSWORD), passwordField);
		formLayout.addLabelAndField(null, null, stayLoggedIn);

		formLayout.addLabelAndComponent(null, null, buttonLogin);

		formLayout.addLabelAndComponent(buttonResetPassword).field.getRowDefinition().setTopPadding(20);
		formLayout.addLabelAndComponent(buttonRegister).field.getRowDefinition().setTopPadding(10);

		rootPanel.setContent(elegantPanel);

		Runnable onLogin = () -> {
			if (checkFieldIsNotEmpty(loginField) && checkFieldIsNotEmpty(passwordField)) {
				User user = authenticate(loginField.getValue(), passwordField.getValue());
				if (user != null) {
					if (stayLoggedIn.getValue()) {
						createSecureLoginToken(user);
					}
					handleSuccessfulLogin(user, rootPanel, context);
				} else {
					errorField.setValue("<span style='font-size:120%;color:#961900'>" + getLocalized(Dictionary.WRONG_USER_NAME_OR_PASSWORD) + "</span>");
				}
			}
		};

		loginField.onSpecialKeyPressed.addListener(key -> {
			if (SpecialKey.ENTER == key) {
				passwordField.focus();
			}
		});
		passwordField.onSpecialKeyPressed.addListener(key -> {
			if (key == SpecialKey.ENTER) {
				onLogin.run();
			}
		});
		buttonLogin.onClicked.addListener(onLogin);
		buttonResetPassword.onClicked.addListener(() -> showPasswordResetView());
		buttonRegister.onClicked.addListener(() -> showRegistrationView());
	}


	private void showSecureTokenLogin(List<UserAccessToken> loginTokens, SessionContext context, RootPanel rootPanel) {
		ElegantPanel elegantPanel = new ElegantPanel();
		InfiniteItemView<User> itemView = new InfiniteItemView<>(Templates.LOGIN_TEMPLATE, 170, 170);
		ListInfiniteItemViewModel<User> itemViewModel = new ListInfiniteItemViewModel<>();

		Map<User, UserAccessToken> accessTokenByUser = new HashMap<>();
		loginTokens.stream()
				.filter(token -> token.getUser() != null)
				.filter(token -> token.getUser().getUserAccountStatus() != UserAccountStatus.INACTIVE)
				.forEach(token -> accessTokenByUser.put(token.getUser(), token));
		List<User> users = new ArrayList<>(accessTokenByUser.keySet());
		users.add(User.create().setFirstName(getLocalized(Dictionary.OTHER_USER)));
		itemViewModel.setRecords(users);

		itemView.setModel(itemViewModel);
		itemView.setAutoHeight(true);
		itemView.setRowHeight(175);
		itemView.setVerticalItemAlignment(ItemViewVerticalItemAlignment.CENTER);
		itemView.setItemJustification(ItemViewRowJustification.CENTER);
		itemView.setItemPropertyProvider(PropertyProviders.createSimpleUserPropertyProvider(systemRegistry));
		elegantPanel.setContent(itemView);
		elegantPanel.setHorizontalContentAlignment(HorizontalElementAlignment.CENTER);

		rootPanel.setContent(elegantPanel);

		itemView.onItemClicked.addListener(data -> {
			User user = data.getRecord();
			if (user.getUserAccountStatus() == UserAccountStatus.INACTIVE) {
				return;
			}
			if (!user.isStored()) {
				showPasswordLogin(context, rootPanel);
			} else {
				UserAccessToken userAccessToken = accessTokenByUser.get(user);
				userAccessToken
						.setLastUsed(Instant.now())
						.setUserAgentLastUsed(SessionContext.current().getClientInfo().getUserAgent())
						.save();
				handleSuccessfulLogin(user, rootPanel, context);
			}
		});
	}

	private void showRegistrationView() {
		/*
			first and last name,
			e-mail OR mobile
		 */
	}

	private void showPasswordResetView() {
		/*
			first and last name,
			e-mail OR mobile
		 */
	}

	private void showAuthCodeView() {
		/*
			e-mail or mobile auth code
			login
			password
			password repeat
		 */
	}

	private void createSecureLoginToken(User user) {
		SessionContext context = SessionContext.current();
		UserAccessToken loginToken = getSecureLoginTokens().stream().filter(token -> token.getUser() != null && token.getUser().equals(user)).findAny().orElse(null);
		if (loginToken == null) {
			String token = "TOK" + UUID.randomUUID().toString().replace("-", "");
			context.addClientToken(token);
			UserAccessToken.create().setUser(user).setSecureToken(token).setUserAgentOnCreation(context.getClientInfo().getUserAgent()).setValid(true).save();
		}
	}

	private List<UserAccessToken> getSecureLoginTokens() {
		List<UserAccessToken> loginTokens = new ArrayList<>();
		Set<String> clientTokens = SessionContext.current().getClientInfo().getClientTokens();
		if (clientTokens != null && !clientTokens.isEmpty()) {
			for (String clientToken : clientTokens) {
				UserAccessToken loginToken = UserAccessToken.filter().secureToken(TextFilter.textEqualsFilter(clientToken)).executeExpectSingleton();
				if (loginToken != null && loginToken.getValid()) {
					loginTokens.add(loginToken);
				}
			}
		}
		return loginTokens;
	}

	private boolean checkFieldIsNotEmpty(TextField field) {
		String value = field.getValue();
		if (value == null || value.isEmpty()) {
			field.setCustomFieldMessages(Collections.singletonList(new FieldMessage(FieldMessage.Position.BELOW, FieldMessage.Visibility.ON_FOCUS, FieldMessage.Severity.ERROR, getLocalized(Dictionary.THIS_FIELD_MUST_NOT_BE_EMPTY))));
			return false;
		} else {
			field.clearCustomFieldMessages();
		}
		return true;
	}

	private String getLocalized(String value) {
		return systemRegistry.getDictionary().getLocalizationValue(value, rankedLanguages);
	}


	private void handleSuccessfulLogin(User user, RootPanel rootPanel, SessionContext context) {
		try {
			UserSessionData userSessionData = new UserSessionData(user, context, systemRegistry, rootPanel);
			UniversalDB.setUserId(userSessionData.getUser().getId());
			String userInfo = user.getId() + "-" + user.getLastName() + "-" + user.getFirstName();
			LOGGER.info("User logged in: {}", userInfo);
			context.setName(userInfo);
			if (systemRegistry.getSessionRegistryHandler() != null) {
				systemRegistry.getSessionRegistryHandler().handleAuthenticatedUser(userSessionData, context);
			}
			new ApplicationLauncher(userSessionData, logoutHandler);
		} finally {
			UniversalDB.setUserId(0);
		}

	}
}
