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
package net.bluemind.system.ldap.importation.internal.tools;

import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.name.Dn;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.importation.internal.l10n.Messages;

public class LdapParametersValidator {
	/**
	 * @param current
	 * @param locale
	 * @throws ServerFault
	 */
	public static void validate(Map<String, String> current, Locale locale) throws ServerFault {
		if (!Boolean.valueOf(current.get(LdapProperties.import_ldap_enabled.name()))) {
			return;
		}

		checkLdapEnabled(current.get(LdapProperties.import_ldap_enabled.name()));
		checkLdapHostname(current.get(LdapProperties.import_ldap_hostname.name()), locale);
		checkLdapProtocol(current.get(LdapProperties.import_ldap_protocol.name()), locale);
		checkLdapAllCertificate(current.get(LdapProperties.import_ldap_accept_certificate.name()));
		checkLdapLoginDn(current.get(LdapProperties.import_ldap_login_dn.name()), locale);
		checkLdapBaseDn(current.get(LdapProperties.import_ldap_base_dn.name()), locale);
		checkLdapUserFilter(current.get(LdapProperties.import_ldap_user_filter.name()), locale);
		checkLdapGroupFilter(current.get(LdapProperties.import_ldap_group_filter.name()), locale);
	}

	/**
	 * @param enabled
	 * @throws ServerFault
	 */
	private static void checkLdapEnabled(String enabled) throws ServerFault {
		if (enabled == null) {
			return;
		}

		if (enabled.equalsIgnoreCase("true") || enabled.equalsIgnoreCase("false")) {
			return;
		}

		throw new ServerFault("Enabled value must be null, true or false", ErrorCode.INVALID_PARAMETER);
	}

	/**
	 * @param allCertificate
	 * @throws ServerFault
	 */
	public static void checkLdapAllCertificate(String allCertificate) throws ServerFault {
		if (allCertificate == null) {
			return;
		}

		if (allCertificate.equalsIgnoreCase("true") || allCertificate.equalsIgnoreCase("false")) {
			return;
		}

		throw new ServerFault("All certificate value must be null, true or false", ErrorCode.INVALID_PARAMETER);
	}

	/**
	 * @param protocol
	 * @param locale
	 * @throws ServerFault
	 */
	public static void checkLdapProtocol(String protocol, Locale locale) throws ServerFault {
		if (protocol == null) {
			throw new ServerFault(Messages.get(locale).nullLdapProtocol(), ErrorCode.INVALID_PARAMETER);
		}

		try {
			LdapProtocol.getProtocol(protocol);
		} catch (IllegalArgumentException i) {
			throw new ServerFault(Messages.get(locale).invalidProtocol() + ": " + protocol,
					ErrorCode.INVALID_PARAMETER);
		}
	}

	/**
	 * @param ldapHostname
	 * @param locale
	 * @throws ServerFault
	 */
	public static void checkLdapHostname(String ldapHostname, Locale locale) throws ServerFault {
		if (ldapHostname == null) {
			throw new ServerFault(Messages.get(locale).invalidHostname(), ErrorCode.INVALID_PARAMETER);
		}

		if (ldapHostname.trim().isEmpty()) {
			throw new ServerFault(Messages.get(locale).invalidHostname(), ErrorCode.INVALID_PARAMETER);
		}
	}

	/**
	 * @param baseDn
	 * @param locale
	 * @throws ServerFault
	 */
	public static void checkLdapBaseDn(String baseDn, Locale locale) throws ServerFault {
		if (baseDn == null) {
			return;
		}

		try {
			new Dn(baseDn);
		} catch (LdapInvalidDnException e) {
			throw new ServerFault(Messages.get(locale).invalidBaseDn(), ErrorCode.INVALID_PARAMETER);
		}
	}

	/**
	 * @param loginDn
	 * @param locale
	 * @throws ServerFault
	 */
	public static void checkLdapLoginDn(String loginDn, Locale locale) throws ServerFault {
		if (loginDn == null) {
			return;
		}

		try {
			new Dn(loginDn);
		} catch (LdapInvalidDnException e) {
			throw new ServerFault(Messages.get(locale).invalidLoginDn(), ErrorCode.INVALID_PARAMETER);
		}
	}

	/**
	 * @param filter
	 * @param locale
	 * @throws ServerFault
	 */
	public static void checkLdapUserFilter(String filter, Locale locale) throws ServerFault {
		try {
			FilterParser.parse(filter);
		} catch (ParseException e) {
			throw new ServerFault(Messages.get(locale).invalidUserFilter(), ErrorCode.INVALID_PARAMETER);
		}
	}

	public static void checkLdapGroupFilter(String filter, Locale locale) throws ServerFault {
		try {
			FilterParser.parse(filter);
		} catch (ParseException e) {
			throw new ServerFault(Messages.get(locale).invalidGroupFilter(), ErrorCode.INVALID_PARAMETER);
		}
	}

	/**
	 * @param previous
	 * @param current
	 * @throws ServerFault
	 */
	public static void noChanges(Map<String, String> previous, Map<String, String> current) throws ServerFault {
		unchanged("Domain admin can't disable LDAP", LdapProperties.import_ldap_enabled.name(), previous, current);
		unchanged("Domain admin can't update LDAP server hostname", LdapProperties.import_ldap_hostname.name(),
				previous, current);
		unchanged("Domain admin can't update LDAP protocol", LdapProperties.import_ldap_protocol.name(), previous,
				current);
		unchanged("Domain admin can't update LDAP all certificate",
				LdapProperties.import_ldap_accept_certificate.name(), previous, current);
		unchanged("Domain admin can't update LDAP base DN", LdapProperties.import_ldap_base_dn.name(), previous,
				current);
		unchanged("Domain admin can't update LDAP user login", LdapProperties.import_ldap_login_dn.name(), previous,
				current);
		unchanged("Domain admin can't update LDAP user password", LdapProperties.import_ldap_password.name(), previous,
				current);
		unchanged("Domain admin can't update LDAP external ID", LdapProperties.import_ldap_ext_id_attribute.name(),
				previous, current);
		unchanged("Domain admin can't update LDAP split domain group",
				LdapProperties.import_ldap_relay_mailbox_group.name(), previous, current);
		unchanged("Domain admin can't update LDAP users filter", LdapProperties.import_ldap_user_filter.name(),
				previous, current);
		unchanged("Domain admin can't update LDAP groups filter", LdapProperties.import_ldap_group_filter.name(),
				previous, current);
		unchanged("Domain admin can't update LDAP last update", LdapProperties.import_ldap_lastupdate.name(), previous,
				current);
	}

	/**
	 * @param errorMessage
	 * @param propertyName
	 * @param previousProperties
	 * @param properties
	 * @throws ServerFault
	 */
	private static void unchanged(String errorMessage, String propertyName, Map<String, String> previousProperties,
			Map<String, String> properties) throws ServerFault {
		String previousValue = previousProperties.get(propertyName);
		String value = properties.get(propertyName);

		if (previousValue == null && value == null) {
			return;
		}

		if (previousValue != null && previousValue.equals(value)) {
			return;
		}

		throw new ServerFault(errorMessage, ErrorCode.INVALID_PARAMETER);
	}
}
