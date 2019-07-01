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

import java.util.concurrent.ConcurrentHashMap;

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
		if (poolByDomain.putIfAbsent(ldapParameters, new LdapPoolWrapper(ldapParameters)) == null) {
			logger.debug("Initialize LDAP pool for: " + ldapParameters.toString());
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
	public LdapConnectionContext getAuthenticatedConnectionContext(Parameters ldapParameters) throws Exception {
		LdapConnectionContext ldapConCtx = getConnectionContext(ldapParameters);

		if (ldapParameters.ldapServer.login == null || ldapParameters.ldapServer.login.isEmpty()) {
			return ldapConCtx;
		}

		BindRequest bindRequest = new BindRequestImpl();
		bindRequest.setSimple(true);
		bindRequest.setName(ldapParameters.ldapServer.login);
		bindRequest.setCredentials(ldapParameters.ldapServer.password);

		long ldSearchTime = System.currentTimeMillis();
		BindResponse response = ldapConCtx.ldapCon.bind(bindRequest);
		ldSearchTime = System.currentTimeMillis() - ldSearchTime;

		if (ResultCodeEnum.SUCCESS != response.getLdapResult().getResultCode()
				|| !ldapConCtx.ldapCon.isAuthenticated()) {
			String errorMsg = "Fail to bind on: " + ldapParameters.toString() + " (" + ldSearchTime + "ms)";
			if (response.getLdapResult().getDiagnosticMessage() != null
					&& !response.getLdapResult().getDiagnosticMessage().isEmpty()) {
				errorMsg += " - " + response.getLdapResult().getDiagnosticMessage();
			}
			logger.error(errorMsg);
			throw new Exception(errorMsg);
		}

		logger.info("Bind success on: " + ldapParameters.toString() + "(" + ldSearchTime + "ms)");
		return ldapConCtx;
	}

	public void releaseConnectionContext(LdapConnectionContext ldapConCtx) throws Exception {
		if (ldapConCtx.ldapCon == null) {
			return;
		}

		LdapPoolWrapper pool = poolByDomain.get(ldapConCtx.ldapParameters);
		if (pool == null) {
			logger.warn(
					"No LDAP connection pool for: " + ldapConCtx.ldapParameters.toString() + ", closing connection");

			ldapConCtx.ldapCon.close();
			return;
		}

		if (ldapConCtx.ldapCon.isAuthenticated()) {
			ldapConCtx.ldapCon.anonymousBind();
		}

		pool.getPool().releaseConnection(ldapConCtx.ldapCon);
	}

	public void resetPool(Parameters ldapParameters) {
		logger.info("Reset LDAP pool for domain: " + ldapParameters.toString());

		LdapPoolWrapper pool = poolByDomain.remove(ldapParameters);
		if (pool == null) {
			logger.warn("No LDAP connection pool for: " + ldapParameters.toString());
			return;
		}

		try {
			pool.getPool().close();
		} catch (Exception e) {
			logger.error("Fail to close LDAP pool for: " + ldapParameters.toString(), e);
		}
	}
}
