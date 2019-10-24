/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.authentication.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.authentication.api.ValidationKind;
import net.bluemind.authentication.api.incore.IInCoreAuthentication;
import net.bluemind.authentication.provider.IAuthProvider;
import net.bluemind.authentication.provider.IAuthProvider.AuthResult;
import net.bluemind.authentication.provider.IAuthProvider.IAuthContext;
import net.bluemind.authentication.provider.ILoginSessionValidator;
import net.bluemind.authentication.provider.ILoginValidationListener;
import net.bluemind.authentication.service.internal.AuthContextCache;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.role.service.IInternalRoles;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.SystemState;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;
import net.bluemind.user.service.IInCoreUser;

public class Authentication implements IAuthentication, IInCoreAuthentication {

	private static final Logger logger = LoggerFactory.getLogger(Authentication.class);

	private static final Splitter atSplitter = Splitter.on('@').trimResults().omitEmptyStrings();

	private final SecurityContext securityContext;
	private final List<IAuthProvider> authProviders;
	private final List<ILoginValidationListener> loginListeners;
	private final List<ILoginSessionValidator> sessionValidators;

	private BmContext context;

	public Authentication(BmContext context, List<IAuthProvider> authProviders,
			List<ILoginValidationListener> loginListeners, List<ILoginSessionValidator> sessionValidators)
			throws ServerFault {
		this.context = context;
		this.securityContext = context.getSecurityContext();
		this.authProviders = authProviders;
		this.loginListeners = loginListeners;
		this.sessionValidators = sessionValidators;
	}

	private static class AuthContext implements IAuthContext {
		public final SecurityContext securityContext;
		public final ItemValue<Domain> domain;
		public final ItemValue<User> user;
		public final String localPart;
		public final String userPassword;

		public AuthContext(SecurityContext securityContext, ItemValue<Domain> domain, ItemValue<User> user,
				String localPart, String userPassword) {
			this.securityContext = securityContext;
			this.domain = domain;
			this.user = user;
			this.localPart = localPart;
			this.userPassword = userPassword;
		}

		@Override
		public SecurityContext getSecurityContext() {
			return securityContext;
		}

		@Override
		public ItemValue<Domain> getDomain() {
			return domain;
		}

		@Override
		public ItemValue<User> getUser() {
			return user;
		}

		@Override
		public String getRealUserLogin() {
			return user != null ? user.value.login : localPart;
		}

		@Override
		public String getUserPassword() {
			return userPassword;
		}
	}

	@Override
	public LoginResponse login(String login, String password, String origin) throws ServerFault {
		return loginWithParams(login, password, origin, false);
	}

	@Override
	public LoginResponse loginWithParams(String login, String password, String origin, Boolean interactive)
			throws ServerFault {
		if (interactive == null) {
			interactive = false;
		}
		logger.debug("try login with l: '{}', o: '{}'", login, origin);

		SystemState systemState = context.su().provider().instance(IInstallation.class).getSystemState();
		if (systemState != SystemState.CORE_STATE_RUNNING) {
			if (!context.getSecurityContext().isAdmin() && !"admin0@global.virt".equals(login)) {
				LoginResponse maintenanceResponse = new LoginResponse();
				maintenanceResponse.status = Status.Bad;
				logger.warn("Authentication denied for user {} while system is in maintenance mode", login);
				maintenanceResponse.message = "Authentication denied while system is in maintenance mode";
				return maintenanceResponse;
			}
		}

		AuthContext authContext = buildAuthContext(login, password);

		LoginResponse resp = new LoginResponse();

		if (authContext == null) {
			resp.status = Status.Bad;
			logger.error("authContext not constructed for login: {} origin: {} remoteIps: {}", login, origin,
					securityContext.getRemoteAddresses());
			return resp;
		}

		SecurityContext context = Sessions.get().getIfPresent(password);

		AuthResult result = AuthResult.UNKNOWN;
		if (context != null) {
			if (authContext.user != null && authContext.user.uid.equals(context.getSubject())) {
				logger.debug("login with token by {}", authContext.user);
				result = AuthResult.YES;
			}

			if (authContext.user != null && !authContext.user.uid.equals(context.getSubject())) {
				logger.debug("login with token by {} but user doesnt match session", authContext.user);
			}
		}

		if (result != AuthResult.YES) {
			result = checkProviders(authContext, origin);
		}

		// user created JIT
		if (result == AuthResult.YES && authContext.user == null) {
			authContext = buildAuthContext(login, password);
		}

		if (authContext == null || authContext.user == null) {
			resp.status = Status.Bad;
			logger.error("authContext.user is null for login: {} origin: {} remoteIps: {}", login, origin,
					securityContext.getRemoteAddresses());
			return resp;
		}

		switch (result) {
		case YES:
			resp.status = Status.Ok;
			resp.latd = authContext.user.value.login + "@" + authContext.domain.uid;
			Map<String, String> settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IUserSettings.class, authContext.domain.uid).get(authContext.user.uid);

			if (context == null) {
				logger.info("login: '{}', origin: '{}', from: '{}' successfully authentified", login, origin,
						securityContext.getRemoteAddresses());
				resp.authKey = UUID.randomUUID().toString();
				context = buildSecurityContext(resp.authKey, authContext.user, authContext.domain.uid, settings, origin,
						interactive);

				for (ILoginSessionValidator v : sessionValidators) {
					try {
						context = v.validateAndModifySession(context);
					} catch (ServerFault e) {
						resp.status = Status.Bad;
						resp.message = e.getMessage();
						return resp;
					}
				}

				if (logger.isDebugEnabled()) {
					logger.debug("[{}] authentified with token : {}", login, context);
				}
				Sessions.get().put(resp.authKey, context);

			} else {
				logger.debug("login: '{}', origin: '{}', from: '{}' successfully authentified with session token",
						login, origin, securityContext.getRemoteAddresses());
				resp.authKey = context.getSessionId();
			}

			resp.authUser = AuthUser.create(context.getContainerUid(), context.getSubject(),
					authContext.user.displayName, authContext.user.value, new HashSet<>(context.getRoles()),
					context.getRolesByOrgUnits(), settings);
			break;
		default:
			resp.status = Status.Bad;
			logger.error("result auth is {} for for login: {} origin: {} remoteIps: {}", result, login, origin,
					securityContext.getRemoteAddresses());
		}

		// update the security context ?
		return resp;
	}

	private AuthContext buildAuthContext(String login, String password) throws ServerFault {
		if (Strings.isNullOrEmpty(login)) {
			logger.error("empty login forbidden");
			return null;
		}
		if (Strings.isNullOrEmpty(password)) {
			logger.error("empty password forbidden for login: {}", login);
			return null;
		}

		try {
			IAuthContext nakedAuthContext = AuthContextCache.getInstance().getCache().get(login, () -> {
				return loadFromDb(login);
			}).orElse(null);

			if (nakedAuthContext == null) {
				return null;
			}

			return new AuthContext(context.getSecurityContext(), nakedAuthContext.getDomain(),
					nakedAuthContext.getUser(), nakedAuthContext.getRealUserLogin(), password);

		} catch (ExecutionException e) {
			throw new ServerFault(e);
		}

	}

	private Optional<IAuthContext> loadFromDb(String login) {
		Iterator<String> splitted = atSplitter.split(login).iterator();
		String localPart = splitted.next();
		String domainPart = splitted.hasNext() ? splitted.next() : "global.virt";
		boolean isStandardDomain = !domainPart.equals("global.virt");

		ServerSideServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDomains domainService = sp.instance(IDomains.class);
		ItemValue<Domain> theDomain = domainService.findByNameOrAliases(domainPart);
		if (theDomain == null) {
			logger.error("Domain {} not found.", domainPart);
			return Optional.empty();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Found domain {}", theDomain.uid);
		}

		ItemValue<User> internalUser = getUser(login, localPart, domainPart, isStandardDomain, sp, theDomain);
		if (internalUser == null) {
			return Optional.empty();
		}

		AuthContext authContext = new AuthContext(null, theDomain, internalUser, localPart, null);
		Optional<IAuthContext> ret = Optional.of(authContext);
		AuthContextCache.getInstance().getCache().put(login, ret);
		return ret;
	}

	private ItemValue<User> getUser(String login, String localPart, String domainPart, boolean isStandardDomain,
			ServerSideServiceProvider sp, ItemValue<Domain> theDomain) {
		IUser userService = sp.instance(IUser.class, theDomain.uid);

		ItemValue<User> internalUser = null;

		if (!isStandardDomain || domainPart.equals(theDomain.uid)) {
			internalUser = userService.byLogin(localPart);
		}
		if (internalUser == null) {
			internalUser = userService.byEmail(login);
		}

		if (internalUser == null) {
			logger.warn("no user found for login {}", login);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("found user {}, domain {} for login {}", internalUser.value.login, theDomain.uid, login);
			}

			if (internalUser.value.archived) {
				logger.warn("user {} is archived", login);
				internalUser = null;
			}
		}
		return internalUser;
	}

	private AuthResult checkProviders(AuthContext authContext, String origin) throws ServerFault {
		if (logger.isDebugEnabled()) {
			logger.debug("[{}@{}] Auth attempt from {}", authContext.localPart, authContext.domain.value.name, origin);
		}

		AuthResult result = AuthResult.UNKNOWN;

		if (authContext.domain == null) {
			logger.warn("[{}@{}] authenticate: {}", authContext.getRealUserLogin(), authContext.domain.value.name,
					result);
			return AuthResult.NO;
		}
		IAuthProvider matchingProvider = null;
		for (IAuthProvider provider : authProviders) {
			result = provider.check(authContext);
			if (logger.isDebugEnabled()) {
				logger.debug("[{}@{}] {} result: {}", authContext.getRealUserLogin(), authContext.domain.value.name,
						provider, result);
			}

			if (result == AuthResult.YES || result == AuthResult.NO) {
				matchingProvider = provider;
				break;
			}

		}

		if (result == AuthResult.YES) {
			for (ILoginValidationListener vl : loginListeners) {
				vl.onValidLogin(matchingProvider, authContext.getRealUserLogin(), authContext.domain.uid,
						authContext.userPassword);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("[{}@{}] authenticate: {}", authContext.getRealUserLogin(), authContext.domain.value.name,
					result);
		}

		return result;

	}

	@Override
	public void logout() throws ServerFault {
		if (securityContext.getSessionId() != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("logout user {} session {}", securityContext.getSubject(), securityContext.getSessionId());
			}
			Sessions.get().invalidate(securityContext.getSessionId());
		} else {
			logger.debug("try to logout without session");
		}
	}

	@Override
	public LoginResponse su(String login) throws ServerFault {
		return suWithParams(login, false);
	}

	@Override
	public LoginResponse suWithParams(String login, Boolean inter) throws ServerFault {
		boolean interactive = inter != null ? inter : false;
		String performer = securityContext.getSubject();
		if (interactive && !securityContext.isDomainGlobal()) {
			AuthUser currentUser = getCurrentUser();
			performer = String.format("%s (%s)", securityContext.getSubject(), currentUser.displayName);
		}
		logger.info("sudo as '{}' by {} from origin '{}'", login, performer, securityContext.getOrigin());

		Iterator<String> splitted = atSplitter.split(login).iterator();
		String localPart = splitted.next();
		String domainPart = splitted.next();

		IUser userService = ServerSideServiceProvider.getProvider(securityContext).instance(IUser.class, domainPart);

		ItemValue<User> user = userService.byLogin(localPart);

		if (user == null) {
			logger.error("Cannot find user with login {} in {}", localPart, domainPart);
			LoginResponse resp = new LoginResponse();
			resp.status = Status.Bad;
			return resp;
		} else if (user.value.archived) {
			logger.error("user with login {} in {} is archived, su refused", localPart, domainPart);
			LoginResponse resp = new LoginResponse();
			resp.status = Status.Bad;
			return resp;
		} else {
			new RBACManager(context).forDomain(domainPart).forEntry(user.uid).check(BasicRoles.ROLE_SUDO);

			LoginResponse resp = new LoginResponse();
			resp.latd = user.value.login + "@" + domainPart;
			resp.status = Status.Ok;
			resp.authKey = UUID.randomUUID().toString();

			ServerSideServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			Map<String, String> settings = sp.instance(IUserSettings.class, domainPart).get(user.uid);

			SecurityContext context = buildSecurityContext(resp.authKey, user, domainPart, settings,
					securityContext.getOrigin(), interactive);

			resp.authUser = AuthUser.create(context.getContainerUid(), context.getSubject(), user.displayName,
					user.value, new HashSet<>(context.getRoles()), context.getRolesByOrgUnits(), settings);
			Sessions.get().put(resp.authKey, context);
			return resp;
		}
	}

	@Override
	public AuthUser getCurrentUser() throws ServerFault {
		RBACManager.forContext(context).checkNotAnoynmous();

		IUser userService = ServerSideServiceProvider.getProvider(securityContext).instance(IUser.class,
				securityContext.getContainerUid());

		ItemValue<User> userItem = userService.getComplete(securityContext.getSubject());

		if (userItem == null) {
			logger.error("userItem of the current user is null");
			return null;
		}
		Map<String, String> settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, securityContext.getContainerUid()).get(userItem.uid);

		return AuthUser.create(securityContext.getContainerUid(), securityContext.getSubject(), userItem.displayName,
				userItem.value, new HashSet<>(securityContext.getRoles()), securityContext.getRolesByOrgUnits(),
				settings);
	}

	/**
	 * 
	 * This method is empty as {@link Sessions#sessionContext(String)} is called
	 * from the rest layer.
	 * 
	 * @see net.bluemind.authentication.api.IAuthentication#ping()
	 */
	@Override
	public void ping() throws ServerFault {
	}

	private SecurityContext buildSecurityContext(String authKey, ItemValue<User> user, String domainUid,
			Map<String, String> config, String origin, boolean interactive) throws ServerFault {
		ServerSideServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		List<String> groups = sp.instance(IUser.class, domainUid).memberOfGroups(user.uid);

		Map<String, Set<String>> rolesByOUs = Collections.emptyMap();
		if (user.value.accountType == AccountType.FULL) {
			IOrgUnits orgUnits = sp.instance(IOrgUnits.class, domainUid);
			List<OrgUnitPath> ous = orgUnits.listByAdministrator(user.uid, groups);
			rolesByOUs = ous.stream() //
					.collect(Collectors.toMap( //
							ouPath -> ouPath.uid, // key: orgUnit.uid
							ouPath -> orgUnits.getAdministratorRoles(ouPath.uid, user.uid, groups))); // roles
		}

		return new SecurityContext(authKey, user.uid, groups, new ArrayList<>(getRoles(user.uid, groups, domainUid)),
				rolesByOUs, domainUid, config.get("lang"), origin, interactive);
	}

	private Set<String> getRoles(String userUid, List<String> groups, String domainUid) throws ServerFault {
		IServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		if ("global.virt".equals(domainUid)) {
			return sp.instance(IInternalRoles.class).resolve(ImmutableSet.<String>builder()
					.add(SecurityContext.ROLE_SYSTEM).add(BasicRoles.ROLE_SELF_CHANGE_PASSWORD).build());
		} else {
			return sp.instance(IInCoreUser.class, domainUid).directResolvedRoles(userUid, groups);
		}
	}

	@Override
	public SecurityContext buildContext(String sid, String domainUid, String userUid) throws ServerFault {
		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domainUid).getComplete(userUid);
		Map<String, String> settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, domainUid).get(userUid);
		return buildSecurityContext(sid, user, domainUid, settings, securityContext.getOrigin(), false);
	}

	@Override
	public ValidationKind validate(String login, String password, String origin) throws ServerFault {
		AuthContext authContext = buildAuthContext(login, password);
		if (authContext == null || authContext.user == null) {
			logger.error("validate failed for login: {} origin: {} remoteIps: {}", login, origin,
					securityContext.getRemoteAddresses());
			return ValidationKind.NONE;
		}

		// check session
		SecurityContext context = Sessions.get().getIfPresent(password);
		if (context != null && context.getSessionId().equals(password)
				&& context.getSubject().equals(authContext.user.uid)) {
			return ValidationKind.TOKEN;
		}

		if (checkProviders(authContext, origin) == AuthResult.YES) {
			return ValidationKind.PASSWORD;
		} else {
			logger.error("validate password or token failed for login: {} origin: {} remoteIps: {}", login, origin,
					securityContext.getRemoteAddresses());
			return ValidationKind.NONE;
		}
	}

}
