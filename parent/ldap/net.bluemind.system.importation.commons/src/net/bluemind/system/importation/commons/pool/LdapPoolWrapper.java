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

import java.time.Duration;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.api.ldap.codec.api.ConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.Parameters.Server.Host;
import net.bluemind.system.importation.commons.exceptions.NoLdapHostAvailableFault;

public class LdapPoolWrapper {
	@SuppressWarnings("serial")
	private static class StartTlsFault extends ServerFault {
	}

	private static final Logger logger = LoggerFactory.getLogger(LdapPoolWrapper.class);
	private static final long LDAP_TIMEOUT = 3000;

	private LdapConnectionPool pool;
	private Parameters ldapParameters;
	private LdapConnectionConfig ldapConnectionConfig;

	public LdapPoolWrapper(Parameters parameters) {
		this.ldapParameters = parameters;
	}

	public synchronized LdapConnectionPool getPool() {
		if (pool == null) {
			initPoolFromHosts();

			if (pool == null) {
				logger.error("No LDAP hosts available for: {}", ldapParameters);
				throw new NoLdapHostAvailableFault("No LDAP hosts available for: " + ldapParameters.toString());
			}

			logger.info("Connected to LDAP: {}", ldapConnectionConfig.getLdapHost());
		}

		return pool;
	}

	private void initPoolFromHosts() {
		List<Host> ldapHosts = ldapParameters.ldapServer.getLdapHost();
		if (ldapHosts == null || ldapHosts.isEmpty()) {
			throw new IllegalArgumentException("At least one LDAP host must be defined!");
		}

		Iterator<Host> ldapHostsIterator = ldapHosts.iterator();
		while (pool == null && ldapHostsIterator.hasNext()) {
			Host ldapHost = ldapHostsIterator.next();

			ldapConnectionConfig = setLdapConnectionConfig(ldapParameters, ldapHost);

			try {
				tryConnection(ldapHost, ldapConnectionConfig);
			} catch (StartTlsFault stf) {
				logger.error("Unable to connect tls:{}:{}", ldapHost.hostname, ldapHost.port, stf);

				if (ldapParameters.ldapServer.protocol == LdapProtocol.TLSPLAIN) {
					ldapConnectionConfig.setUseTls(false);
					tryConnection(ldapHost, ldapConnectionConfig);
				}
			}
		}
	}

	private void tryConnection(Host ldapHost, LdapConnectionConfig ldapConnectionConfig) {
		if (logger.isInfoEnabled()) {
			logger.info("Trying to connect to: {}", getLdapConnectionURI(ldapHost, ldapConnectionConfig));
		}

		DefaultPoolableLdapConnectionFactory bpcf = new DefaultPoolableLdapConnectionFactory(ldapConnectionConfig);

		LdapConnectionPool tmpPool = null;
		LdapConnection conn = null;

		try {
			tmpPool = new LdapConnectionPool(bpcf);
			tmpPool.setMaxWait(Duration.ofSeconds(10));

			conn = tmpPool.getConnection();
			tmpPool.releaseConnection(conn);

			pool = tmpPool;
			this.ldapConnectionConfig = ldapConnectionConfig;
		} catch (LdapException e) {
			logger.warn("Unable to connect to: " + ldapHost.hostname, e);

			try {
				tmpPool.close();
			} catch (Exception ignored) {
				// ok
			}

			if (e instanceof LdapOperationException loe && (loe.getResultCode() == ResultCodeEnum.UNAVAILABLE)
					&& ldapConnectionConfig.isUseTls()) {
				throw new StartTlsFault();
			}
		}
	}

	private String getLdapConnectionURI(Host ldapHost, LdapConnectionConfig ldapConnectionConfig) {
		String hostPort = ldapHost.hostname + ":" + ldapHost.port;

		if (ldapConnectionConfig.isUseSsl()) {
			return "ssl:" + hostPort;
		}

		return (ldapConnectionConfig.isUseTls() ? "tls:" : "plain:") + hostPort;
	}

	private LdapConnectionConfig setLdapConnectionConfig(Parameters ldapParameters, Host ldapHost) {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(ldapHost.hostname);
		config.setLdapPort(ldapHost.port);
		config.setTimeout(LDAP_TIMEOUT);

		switch (ldapParameters.ldapServer.protocol) {
		case TLS, TLSPLAIN:
			config.setUseTls(true);
			config.setUseSsl(false);
			break;
		case SSL:
			config.setUseTls(false);
			config.setUseSsl(true);
			break;
		default:
			config.setUseTls(false);
			config.setUseSsl(false);
			break;
		}

		if (ldapParameters.ldapServer.acceptAllCertificates) {
			config.setTrustManagers(new NoVerificationTrustManager());
		}

		ConfigurableBinaryAttributeDetector detector = new DefaultConfigurableBinaryAttributeDetector();
		config.setBinaryAttributeDetector(detector);

		return config;
	}

	public LdapConnectionConfig getLdapConnectionConfig() {
		return ldapConnectionConfig;
	}
}
