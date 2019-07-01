/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.addressbook.ldap.service.internal.utils;

import org.apache.directory.api.ldap.codec.api.ConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.ldap.api.ConnectionStatus;
import net.bluemind.addressbook.ldap.api.LdapParameters;
import net.bluemind.addressbook.ldap.api.fault.LdapAddressBookErrorCode;
import net.bluemind.addressbook.ldap.service.internal.LdapAddressBookService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.lib.ldap.LdapProtocol;

public class LdapHelper {

	private static final Logger logger = LoggerFactory.getLogger(LdapAddressBookService.class);

	private static final long LDAP_TIMEOUT = 10000;

	public static ConnectionStatus checkLDAPParameters(LdapParameters params) throws ServerFault {

		ConnectionStatus cs = new ConnectionStatus();

		try (LdapConProxy ldapCon = connectLdap(params)) {
			checkBaseDn(params.baseDn, ldapCon);
			cs.status = true;
		} catch (LdapAddressBookFault e) {
			logger.error(e.getMessage(), e);
			cs.status = false;
			cs.errorCode = e.errorCode;
			cs.errorMsg = e.getMessage();
		} catch (Exception e) {
			cs.errorCode = LdapAddressBookErrorCode.UNKNOWN;
			cs.errorMsg = e.getMessage();
		}

		return cs;
	}

	private static void checkBaseDn(String ldapBaseDn, LdapConProxy ldapCon) throws Exception {
		SearchRequestImpl searchRequest = new SearchRequestImpl();
		searchRequest.setScope(SearchScope.ONELEVEL);
		if (ldapBaseDn == null || ldapBaseDn.length() == 0) {
			searchRequest.setBase(new Dn());
		} else {
			searchRequest.setBase(new Dn(ldapBaseDn));
		}
		searchRequest.setFilter("(objectclass=*)");
		SearchCursor result = ldapCon.search(searchRequest);

		try {
			if (!result.next()) {
				throw new LdapAddressBookFault("Base DN not found, check existence or set server default search base",
						LdapAddressBookErrorCode.INVALID_LDAP_BASEDN);
			}
		} finally {
			result.close();
		}

	}

	public static LdapConProxy connectLdap(LdapParameters params) {
		LdapConProxy ldapCon = null;

		try {
			ldapCon = getLdapCon(params);

			BindRequest bindRequest = new BindRequestImpl();
			bindRequest.setSimple(true);

			if (params.loginDn != null) {
				bindRequest.setName(params.loginDn);
				bindRequest.setCredentials(params.loginPw);
			}

			BindResponse response = ldapCon.bind(bindRequest);

			if (ResultCodeEnum.SUCCESS != response.getLdapResult().getResultCode() || !ldapCon.isAuthenticated()) {
				throw new LdapAddressBookFault("LDAP connection failed: " + response.getLdapResult().getResultCode(),
						LdapAddressBookErrorCode.INVALID_LDAP_CREDENTIAL);
			}
		} catch (LdapException e) {
			throw new LdapAddressBookFault("Fail to connect to LDAP server",
					LdapAddressBookErrorCode.INVALID_LDAP_HOSTNAME);
		}

		return ldapCon;
	}

	private static LdapConProxy getLdapCon(LdapParameters params) throws ServerFault {
		LdapConnectionConfig config = getLdapConnectionConfig(params);
		return new LdapConProxy(config);
	}

	private static LdapProtocol getProtocol(String protocol) {
		if (protocol == null) {
			return LdapProtocol.PLAIN;
		}

		try {
			return LdapProtocol.getProtocol(protocol);
		} catch (IllegalArgumentException i) {
			logger.error("Invalid protocol {}, use: {}", protocol, LdapProtocol.PLAIN.toString());
			return LdapProtocol.PLAIN;
		}
	}

	private static LdapConnectionConfig getLdapConnectionConfig(LdapParameters params) {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(params.hostname);
		config.setTimeout(LDAP_TIMEOUT);

		switch (getProtocol(params.protocol)) {
		case TLS:
			config.setLdapPort(389);

			config.setUseTls(true);
			config.setUseSsl(false);
			break;
		case SSL:
			config.setLdapPort(636);

			config.setUseTls(false);
			config.setUseSsl(true);
			break;
		default:
			config.setLdapPort(389);

			config.setUseTls(false);
			config.setUseSsl(false);
			break;
		}

		if (params.allCertificate) {
			config.setTrustManagers(new NoVerificationTrustManager());
		}

		ConfigurableBinaryAttributeDetector detector = new DefaultConfigurableBinaryAttributeDetector();
		if (params.type == LdapParameters.DirectoryType.ad) {
			detector.addBinaryAttribute(LdapParameters.AD_UUID);
			detector.addBinaryAttribute("thumbnailPhoto");
		}
		config.setBinaryAttributeDetector(detector);

		return config;
	}

}
