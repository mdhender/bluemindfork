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
package net.bluemind.system.importation.commons.pool;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.system.importation.commons.Parameters;

public class LdapPoolByDomain {
	public static class LdapConnectionContext {
		public final LdapConnection ldapCon;
		public final LdapConnectionConfig ldapConnectionConfig;
		public final Parameters ldapParameters;
		private boolean ldapConError = false;

		public LdapConnectionContext(LdapConnection ldapCon, LdapConnectionConfig ldapConnectionConfig,
				Parameters ldapParameters) {
			this.ldapCon = ldapCon;
			this.ldapConnectionConfig = ldapConnectionConfig;
			this.ldapParameters = ldapParameters;
		}

		public LdapProtocol getConnectedProtocol() {
			if (ldapConnectionConfig.isUseSsl()) {
				return LdapProtocol.SSL;
			}

			if (ldapConnectionConfig.isUseTls()) {
				return LdapProtocol.TLS;
			}

			return LdapProtocol.PLAIN;
		}

		/**
		 * Set ldapCon status to error
		 * 
		 * @return
		 */
		public LdapConnectionContext setError() {
			ldapConError = true;
			return this;
		}

		/**
		 * Is ldapCon status set to error
		 * 
		 * @return
		 */
		public boolean isError() {
			return ldapConError;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(LdapPoolByDomain.class);

	private static final ConcurrentHashMap<Parameters, LdapPoolWrapper> poolByDomain = new ConcurrentHashMap<>();

	/**
	 * Get anonymous LDAP connection
	 * 
	 * @param ldapParameters
	 * @return
	 * @throws Exception
	 */
	public LdapConnectionContext getConnectionContext(Parameters ldapParameters) throws Exception {
		if (poolByDomain.putIfAbsent(ldapParameters, new LdapPoolWrapper(ldapParameters)) == null
				&& logger.isDebugEnabled()) {
			logger.debug("Initialize LDAP pool for: {}", ldapParameters);
		}
		LdapPoolWrapper pool = poolByDomain.get(ldapParameters);

		return new LdapConnectionContext(pool.getPool().getConnection(), pool.ldapConnectionConfig, ldapParameters);
	}

	/**
	 * Get authenticated LDAP connection, using login/password from LDAP parameters
	 * 
	 * @param ldapParameters
	 * @return
	 * @throws Exception
	 */
	public Optional<LdapConnectionContext> getAuthenticatedConnectionContext(Parameters ldapParameters)
			throws Exception {
		LdapConnectionContext ldapConCtx = getConnectionContext(ldapParameters);

		if (ldapParameters.ldapServer.login == null || ldapParameters.ldapServer.login.isEmpty()) {
			return Optional.of(ldapConCtx);
		}

		BindRequest bindRequest = new BindRequestImpl();
		bindRequest.setSimple(true);
		bindRequest.setName(ldapParameters.ldapServer.login);
		bindRequest.setCredentials(ldapParameters.ldapServer.password);

		long ldSearchTime = System.currentTimeMillis();
		BindResponse response = null;
		try {
			response = ldapConCtx.ldapCon.bind(bindRequest);
		} catch (LdapException le) {
			logger.error(le.getMessage(), le);
			releaseConnectionContext(ldapConCtx.setError());
			return Optional.empty();
		}

		ldSearchTime = System.currentTimeMillis() - ldSearchTime;

		if (ResultCodeEnum.SUCCESS != response.getLdapResult().getResultCode()
				|| !ldapConCtx.ldapCon.isAuthenticated()) {
			String errorMsg = "Fail to bind on: " + ldapParameters.toString() + " (" + ldSearchTime + "ms)";
			if (response.getLdapResult().getDiagnosticMessage() != null
					&& !response.getLdapResult().getDiagnosticMessage().isEmpty()) {
				errorMsg += " - " + response.getLdapResult().getDiagnosticMessage();
			}

			releaseConnectionContext(ldapConCtx);

			logger.error(errorMsg);
			return Optional.empty();
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Bind success on: {} ({}ms)", ldapParameters, ldSearchTime);
		}
		return Optional.of(ldapConCtx);
	}

	public void releaseConnectionContext(LdapConnectionContext ldapConCtx) {
		if (ldapConCtx == null || ldapConCtx.ldapCon == null) {
			return;
		}

		getPoolOrCloseLdapConnexion(ldapConCtx.ldapParameters, ldapConCtx.ldapCon)
				.ifPresent(pool -> doReleaseConnection(pool, ldapConCtx)
						.ifPresent(lcc -> invalidateConnectionOrResetPool(pool, lcc)));
	}

	private Optional<LdapPoolWrapper> getPoolOrCloseLdapConnexion(Parameters ldapParameters, LdapConnection ldapCon) {
		LdapPoolWrapper pool = poolByDomain.get(ldapParameters);

		if (pool != null) {
			return Optional.of(pool);
		}

		logger.warn("No LDAP connection pool for: {}, closing connection", ldapParameters);
		try {
			ldapCon.close();
		} catch (IOException ioe) {
			// No pool found, and unable to close orphan LDAP connection...
			logger.warn("Unable to close LDAP connection for {}", ldapParameters, ioe);
		}

		return Optional.empty();
	}

	private void invalidateConnectionOrResetPool(LdapPoolWrapper pool, LdapConnectionContext ldapConCtx) {
		logger.warn("Invalidate LDAP connection from pool {}", ldapConCtx.ldapParameters);
		try {
			pool.getPool().invalidateObject(ldapConCtx.ldapCon);
		} catch (Exception e) {
			logger.error("Unable to invalidate connection from pool {}", ldapConCtx.ldapParameters, e);
			resetPool(ldapConCtx.ldapParameters);
		}
	}

	private Optional<LdapConnectionContext> doReleaseConnection(LdapPoolWrapper pool,
			LdapConnectionContext ldapConCtx) {
		if (ldapConCtx.isError()) {
			return Optional.of(ldapConCtx);
		}

		try {
			if (ldapConCtx.ldapCon.isAuthenticated()) {
				ldapConCtx.ldapCon.anonymousBind();
			}

			pool.getPool().releaseConnection(ldapConCtx.ldapCon);
			return Optional.empty();
		} catch (LdapException le) {
			logger.error("Unable to release connection from pool {}", ldapConCtx.ldapParameters, le);
		}

		return Optional.of(ldapConCtx.setError());
	}

	public void resetPool(Parameters ldapParameters) {
		logger.info("Reset LDAP pool for domain: {}", ldapParameters);

		LdapPoolWrapper pool = poolByDomain.remove(ldapParameters);
		if (pool == null) {
			logger.warn("No LDAP connection pool for: {}", ldapParameters);
			return;
		}

		try {
			pool.getPool().clear();
			pool.getPool().close();
		} catch (Exception e) {
			logger.error("Fail to close LDAP pool for: " + ldapParameters.toString(), e);
		}
	}
}
