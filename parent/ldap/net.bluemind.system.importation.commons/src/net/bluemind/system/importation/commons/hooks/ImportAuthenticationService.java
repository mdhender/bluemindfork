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
package net.bluemind.system.importation.commons.hooks;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.authentication.provider.IAuthProvider;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.exceptions.GetDnFailure;
import net.bluemind.user.api.User;

public abstract class ImportAuthenticationService implements IAuthProvider {
	private static final Logger logger = LoggerFactory.getLogger(ImportAuthenticationService.class);

	private static final Cache<UuidMapper, String> uidToDN = CacheBuilder.newBuilder()
			.expireAfterWrite(20, TimeUnit.MINUTES).initialCapacity(1024).build();
	private static final Cache<String, String> dnToPass = CacheBuilder.newBuilder()
			.expireAfterWrite(20, TimeUnit.MINUTES).initialCapacity(1024).build();

	@Override
	public int priority() {
		return 100;
	}

	@Override
	public AuthResult check(IAuthContext authContext) {
		ItemValue<User> userItem = authContext.getUser();
		if (userExistsInDB(userItem) && (userItem.externalId == null || !userItem.externalId.startsWith(getPrefix()))) {
			// User exists in BM DB but not imported from directory
			return AuthResult.UNKNOWN;
		}

		if (Strings.isNullOrEmpty(authContext.getUserPassword())) {
			logger.error("{} authentication refused null or empty password for {}", getDirectoryKind(),
					authContext.getRealUserLogin());
			return (userExistsInDB(userItem) ? AuthResult.NO : AuthResult.UNKNOWN);
		}

		ItemValue<Domain> domain = authContext.getDomain();
		Parameters parameters;
		try {
			Map<String, String> domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IDomainSettings.class, domain.uid).get();
			parameters = getParameters(domain.value, domainSettings);
		} catch (ServerFault e) {
			logger.error("Unable to load parameters for domain {}", domain.uid, e);
			return (userExistsInDB(userItem) ? AuthResult.NO : AuthResult.UNKNOWN);
		}

		if (!parameters.enabled) {
			return (userExistsInDB(userItem) ? AuthResult.NO : AuthResult.UNKNOWN);
		}

		if (userExistsInDB(userItem)) {
			return authImportedUser(domain, parameters, authContext);
		}

		return authNotImportedUser(domain, parameters, authContext);
	}

	private boolean userExistsInDB(ItemValue<User> userItem) {
		return userItem != null;
	}

	/**
	 * Authenticate directory already imported user
	 * 
	 * @param domain
	 * @param parameters
	 * @param authContext
	 * @return
	 */
	private AuthResult authImportedUser(ItemValue<Domain> domain, Parameters parameters, IAuthContext authContext) {
		String userDn;
		try {
			userDn = getUserDnFromExtId(parameters, domain, authContext.getUser());
		} catch (GetDnFailure e) {
			return AuthResult.NO;
		}

		if (userDn == null) {
			return AuthResult.NO;
		}

		String cachedPass = dnToPass.getIfPresent(userDn);
		if (cachedPass != null && cachedPass.equals(authContext.getUserPassword())) {
			logger.info("Allowed directory user {} from dnToPass cache system", userDn);
			return AuthResult.YES;
		}

		if (checkAuth(parameters, userDn, authContext.getUserPassword())) {
			dnToPass.put(userDn, authContext.getUserPassword());
			return AuthResult.YES;
		}

		return AuthResult.NO;
	}

	/**
	 * Try to authenticate unexistant BlueMind user against directory to check if it
	 * exists in directory
	 * 
	 * @param domain
	 * @param parameters
	 * @param authContext
	 * @return
	 */
	private AuthResult authNotImportedUser(ItemValue<Domain> domain, Parameters parameters, IAuthContext authContext) {
		try {
			logger.info("User {} not found in database, search login in {}", authContext.getRealUserLogin(),
					getDirectoryKind());
			String userDn = getUserDnByUserLogin(parameters, domain.value.name, authContext.getRealUserLogin());

			if (userDn == null) {
				return AuthResult.UNKNOWN;
			}

			if (checkAuth(parameters, userDn, authContext.getUserPassword())) {
				dnToPass.put(userDn, authContext.getUserPassword());
				return AuthResult.YES;
			}
		} catch (Exception e) {
			logger.error("Unable to search for user login {} in {}", authContext.getRealUserLogin(), getDirectoryKind(),
					e);
			return AuthResult.UNKNOWN;
		}

		return AuthResult.NO;
	}

	/**
	 * @param parameters
	 * @param domain
	 * @param authContext
	 * @return null if not a directory user, else user directory DN
	 * @throws UuidSearchException
	 */
	private String getUserDnFromExtId(Parameters parameters, ItemValue<Domain> domain, ItemValue<User> userItem)
			throws GetDnFailure {
		Optional<UuidMapper> bmUserUid = getUuidMapper(userItem.externalId);
		if (!bmUserUid.isPresent()) {
			return null;
		}

		String udn = uidToDN.getIfPresent(bmUserUid);

		long ldSearchTime = 0;
		if (udn == null) {
			ldSearchTime = System.currentTimeMillis();

			try {
				udn = getUserDnByUuid(parameters, bmUserUid.get().getGuid());
			} catch (Exception e) {
				throw new GetDnFailure();
			}

			if (udn != null) {
				uidToDN.put(bmUserUid.get(), udn);
			}

			ldSearchTime = System.currentTimeMillis() - ldSearchTime;
		}

		if (udn == null) {
			logger.error("Unable to find DN for extId {}, user {}@{}. Time: {}ms.", bmUserUid.get().getExtId(),
					userItem.value.login, domain.value.name, ldSearchTime);
			throw new GetDnFailure();
		}

		logger.info("Found: {}, searched for extId {}, u: {}@{}. Time: {}ms.", udn, bmUserUid.get().getExtId(),
				userItem.value.login, domain.value.name, ldSearchTime);

		return udn;
	}

	/**
	 * Get directory kind
	 * 
	 * @return
	 */
	protected abstract String getDirectoryKind();

	/**
	 * Get directory external ID prefix
	 * 
	 * @return
	 */
	protected abstract String getPrefix();

	/**
	 * Get domain directory parameters
	 * 
	 * @param value
	 * @param domainSettings
	 * @return
	 */
	protected abstract Parameters getParameters(Domain domain, Map<String, String> domainSettings);

	/**
	 * Get user UuidMapper from user external ID
	 * 
	 * @param externalId
	 * @return
	 */
	protected abstract Optional<UuidMapper> getUuidMapper(String externalId);

	/**
	 * Get user directory DN from user login
	 * 
	 * @param parameters
	 * @param domainName
	 * @param userLogin
	 * @return
	 */
	protected abstract String getUserDnByUserLogin(Parameters parameters, String domainName, String userLogin);

	/**
	 * Get user directory DN from user external ID
	 * 
	 * @param Parameters
	 * @param uuid
	 * @return
	 * @throws Exception
	 */
	protected abstract String getUserDnByUuid(Parameters parameters, String uuid) throws Exception;

	/**
	 * Check user authentication against directory
	 * 
	 * @param parameters
	 * @param userDn
	 * @param userPassword
	 * @return
	 */
	protected abstract boolean checkAuth(Parameters parameters, String userDn, String userPassword);
}
