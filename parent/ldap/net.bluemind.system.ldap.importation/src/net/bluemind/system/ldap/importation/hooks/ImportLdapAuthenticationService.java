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
package net.bluemind.system.ldap.importation.hooks;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.domain.api.Domain;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.hooks.ImportAuthenticationService;
import net.bluemind.system.importation.commons.pool.LdapPoolByDomain;
import net.bluemind.system.importation.commons.pool.LdapPoolByDomain.LdapConnectionContext;
import net.bluemind.system.ldap.importation.Activator;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.LdapUuidMapper;
import net.bluemind.system.ldap.importation.search.LdapUserSearchFilter;

public class ImportLdapAuthenticationService extends ImportAuthenticationService {
	private static final Logger logger = LoggerFactory.getLogger(ImportLdapAuthenticationService.class);

	@Override
	protected String getDirectoryKind() {
		return "LDAP";
	}

	@Override
	protected String getPrefix() {
		return LdapConstants.EXTID_PREFIX;
	}

	@Override
	protected Parameters getParameters(Domain domain, Map<String, String> domainSettings) {
		return LdapParameters.build(domain, domainSettings);
	}

	@Override
	protected Optional<UuidMapper> getUuidMapper(String externalId) {
		return LdapUuidMapper.fromExtId(externalId);
	}

	@Override
	protected String getUserDnByUserLogin(Parameters parameters, String domainName, String userLogin) {
		String ldapUserLogin = null;

		LdapPoolByDomain ldapPoolByDomain = Activator.getLdapPoolByDomain();
		Optional<LdapConnectionContext> ldapConCtx = Optional.empty();
		try {
			ldapConCtx = ldapPoolByDomain.getAuthenticatedConnectionContext(parameters);

			if (ldapConCtx.isPresent()) {
				EntryCursor result = ldapConCtx.get().ldapCon.search(parameters.ldapDirectory.baseDn,
						new LdapUserSearchFilter().getSearchFilter(parameters, Optional.empty(), userLogin, null),
						SearchScope.SUBTREE, "dn");

				if (result.next()) {
					ldapUserLogin = result.get().getDn().getName();
				}
			}
		} catch (RuntimeException re) {
			if (re.getCause() != null && re.getCause() instanceof InterruptedException) {
				logger.error(String.format("Getting an interrupted exception, reseting pool for %s", parameters), re);
				ldapPoolByDomain.resetPool(parameters);
			}

			throw re;
		} catch (Exception e) {
			logger.error("Fail to get LDAP DN for user: " + userLogin + "@" + domainName, e);
			ldapConCtx.ifPresent(lcc -> lcc.setError());
			return null;
		} finally {
			ldapConCtx.ifPresent(lcc -> releaseConnection(ldapPoolByDomain, parameters, lcc));
		}

		if (ldapUserLogin == null) {
			logger.error("Unable to find {}@{}", userLogin, domainName);
		}

		return ldapUserLogin;
	}

	private static final Cache<String, String> uuidToDnCache = CacheBuilder.newBuilder()
			.expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();

	@Override
	protected String getUserDnByUuid(Parameters parameters, String uuid) throws Exception {
		String ldapUserLogin = uuidToDnCache.getIfPresent(uuid);
		if (ldapUserLogin != null) {
			return ldapUserLogin;
		}

		LdapPoolByDomain ldapPoolByDomain = Activator.getLdapPoolByDomain();

		Optional<LdapConnectionContext> ldapConCtx = Optional.empty();
		try {
			ldapConCtx = ldapPoolByDomain.getAuthenticatedConnectionContext(parameters);

			if (ldapConCtx.isPresent()) {
				String filter = new LdapUserSearchFilter().getSearchFilter(parameters, Optional.empty(), null, uuid);
				EntryCursor result = ldapConCtx.get().ldapCon.search(parameters.ldapDirectory.baseDn, filter,
						SearchScope.SUBTREE, "dn");

				if (result.next()) {
					ldapUserLogin = result.get().getDn().getName();
				} else {
					logger.warn("uuid " + uuid + " not found with filter " + filter);
				}
			}
		} catch (RuntimeException re) {
			if (re.getCause() != null && re.getCause() instanceof InterruptedException) {
				logger.error(String.format("Getting an interrupted exception, reseting pool for %s", parameters), re);
				ldapPoolByDomain.resetPool(parameters);
			}

			throw re;
		} catch (Exception e) {
			logger.error(String.format("Error searching external ID %s", uuid), e);
			ldapConCtx.ifPresent(lcc -> lcc.setError());
			throw e;
		} finally {
			ldapConCtx.ifPresent(lcc -> releaseConnection(ldapPoolByDomain, parameters, lcc));
		}

		if (ldapUserLogin == null) {
			logger.error("Unable to find {}", uuid);
		} else {
			uuidToDnCache.put(uuid, ldapUserLogin);
		}

		return ldapUserLogin;
	}

	@Override
	protected boolean checkAuth(Parameters parameters, String userDn, String userPassword) {
		LdapPoolByDomain ldapPoolByDomain = Activator.getLdapPoolByDomain();
		LdapConnectionContext ldapConCtx = null;

		try {
			ldapConCtx = ldapPoolByDomain.getConnectionContext(parameters);

			BindRequest bindRequest = new BindRequestImpl();
			bindRequest.setSimple(true);
			bindRequest.setName(userDn);
			bindRequest.setCredentials(userPassword);

			long ldSearchTime = System.currentTimeMillis();
			BindResponse response = ldapConCtx.ldapCon.bind(bindRequest);
			ldSearchTime = System.currentTimeMillis() - ldSearchTime;

			if (ResultCodeEnum.SUCCESS != response.getLdapResult().getResultCode()
					|| !ldapConCtx.ldapCon.isAuthenticated()) {
				logger.error(
						"Loggin failed on: {}:{}:{}, result: {}, message: {}, authenticated: {}, user dn: {},ldapAuth: {}ms",
						ldapConCtx.getConnectedProtocol().name(), ldapConCtx.ldapConnectionConfig.getLdapHost(),
						ldapConCtx.ldapConnectionConfig.getLdapPort(), response.getLdapResult().getResultCode().name(),
						response.getLdapResult().getDiagnosticMessage(), ldapConCtx.ldapCon.isAuthenticated(), userDn,
						ldSearchTime);
				return false;
			}

			logger.info("Loggin success on: {}:{}:{}, user dn: {}, ldapAuth: {}ms",
					ldapConCtx.getConnectedProtocol().name(), ldapConCtx.ldapConnectionConfig.getLdapHost(),
					ldapConCtx.ldapConnectionConfig.getLdapPort(), userDn, ldSearchTime);
			return true;
		} catch (RuntimeException re) {
			if (re.getCause() != null && re.getCause() instanceof InterruptedException) {
				logger.error(String.format("Getting an interrupted exception, reseting pool for %s", parameters), re);
				ldapPoolByDomain.resetPool(parameters);
			}

			throw re;
		} catch (Exception e) {
			logger.error("Fail to check LDAP authentication", e);
			ldapConCtx = ldapConCtx.setError();
			return false;
		} finally {
			// https://docs.oracle.com/javase/tutorial/essential/exceptions/finally.html
			releaseConnection(ldapPoolByDomain, parameters, ldapConCtx);
		}
	}

	private void releaseConnection(LdapPoolByDomain ldapPoolByDomain, Parameters parameters,
			LdapConnectionContext ldapConCtx) {
		try {
			ldapPoolByDomain.releaseConnectionContext(ldapConCtx);
		} catch (Exception e) {
			logger.error(String.format("Unable to release LDAP connection for %s, closing this connection", parameters),
					e);
		}
	}
}
