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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.directory.api.ldap.codec.api.ConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Timer;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.exceptions.DirectoryConnectionFailed;
import net.bluemind.system.importation.commons.managers.UserManager;
import net.bluemind.system.importation.commons.scanner.IImportLogger;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.i18n.Messages;
import net.bluemind.system.ldap.importation.metrics.MetricsHolder;
import net.bluemind.system.ldap.importation.search.LdapUserSearchFilter;
import net.bluemind.user.api.User;

public class LdapHelper {
	private static final Logger logger = LoggerFactory.getLogger(LdapHelper.class);

	private static final MetricsHolder metrics = MetricsHolder.get();

	private static final long LDAP_TIMEOUT = 10000;

	private LdapHelper() {
	}

	public static Optional<UserManager> getLdapUser(LdapParameters ldapParameters, ItemValue<Domain> domain,
			String userLogin, ItemValue<User> bmUser, MailFilter mailFilter) {
		Timer conTimer = metrics.forOperation("getLdapUser");
		long time = metrics.clock.monotonicTime();
		try (LdapConProxy ldapCon = connectLdap(ldapParameters)) {
			EntryCursor result = ldapCon.search(ldapParameters.ldapDirectory.baseDn,
					new LdapUserSearchFilter().getSearchFilterByName(ldapParameters, userLogin), SearchScope.SUBTREE,
					"*", UserManagerImpl.LDAP_MEMBER_OF, ldapParameters.ldapDirectory.extIdAttribute);

			conTimer.record(metrics.clock.monotonicTime() - time, TimeUnit.NANOSECONDS);
			if (result.next()) {
				Entry entry = result.get();

				Optional<UserManager> optionalUserManager = UserManagerImpl.build(ldapParameters, domain, entry);
				optionalUserManager.ifPresent(userManagerImp -> userManagerImp.update(bmUser, mailFilter));
				return optionalUserManager;
			}
		} catch (Exception e) {
			logger.error("Fail to get LDAP user: " + userLogin + "@" + domain.value.name, e);
		}

		return Optional.empty();
	}

	public static void checkLDAPUserFilter(String userFilter) throws ServerFault {
		try {
			FilterParser.parse(userFilter);
		} catch (ParseException e) {
			logger.error("Fail to check user LDAP filter", e);
			throw new ServerFault("Filtre des utilisateurs LDAP invalide: " + e.getMessage());
		}
	}

	public static void checkLDAPGroupFilter(String groupFilter) throws ServerFault {
		try {
			FilterParser.parse(groupFilter);
		} catch (ParseException e) {
			logger.error("Fail to check group LDAP filter", e);
			throw new ServerFault("Filtre des groupes LDAP invalide: " + e.getMessage());
		}
	}

	public static LdapConProxy connectLdap(Parameters ldapParameters) {
		return connectLdap(ldapParameters, Optional.empty());
	}

	public static LdapConProxy connectLdap(Parameters ldapParameters, Optional<ImportLogger> importLogger)
			throws ServerFault {
		LdapConProxy ldapCon = null;
		try {
			ldapCon = getLdapCon(ldapParameters);

			BindRequest bindRequest = new BindRequestImpl();
			bindRequest.setSimple(true);
			bindRequest.setName(ldapParameters.ldapServer.login);
			bindRequest.setCredentials(ldapParameters.ldapServer.password);

			BindResponse response = ldapCon.bind(bindRequest);
			if (ResultCodeEnum.SUCCESS != response.getLdapResult().getResultCode() || !ldapCon.isAuthenticated()) {
				importLogger.ifPresent(il -> il.withoutStatus()
						.error(Messages.serverConnectionFail(ldapParameters.ldapServer.getLdapHost().get(0).hostname,
								ldapParameters.ldapServer.getLdapHost().get(0).port,
								response.getLdapResult().getResultCode() + " "
										+ response.getLdapResult().getDiagnosticMessage())));
				throw new DirectoryConnectionFailed(
						"Fail to connect to server: " + ldapParameters.ldapServer.getLdapHost().get(0).hostname + ":"
								+ ldapParameters.ldapServer.getLdapHost().get(0).port + " - "
								+ response.getLdapResult().getResultCode() + " "
								+ response.getLdapResult().getDiagnosticMessage());
			}
		} catch (LdapException e) {
			importLogger.ifPresent(il -> il.withoutStatus()
					.error(Messages.serverConnectionFail(ldapParameters.ldapServer.getLdapHost().get(0).hostname,
							ldapParameters.ldapServer.getLdapHost().get(0).port, e.getMessage())));
			throw new DirectoryConnectionFailed(
					"Fail to connect to server: " + ldapParameters.ldapServer.getLdapHost().get(0).hostname + ":"
							+ ldapParameters.ldapServer.getLdapHost().get(0).port + " - " + e.getMessage());
		}

		return ldapCon;
	}

	private static LdapConProxy getLdapCon(Parameters ldapParameters) throws ServerFault {
		LdapConnectionConfig config = getLdapConnectionConfig(ldapParameters);
		return new LdapConProxy(config);
	}

	private static LdapConnectionConfig getLdapConnectionConfig(Parameters ldapParameters) {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(ldapParameters.ldapServer.getLdapHost().get(0).hostname);
		config.setLdapPort(ldapParameters.ldapServer.getLdapHost().get(0).port);
		config.setTimeout(LDAP_TIMEOUT);

		switch (ldapParameters.ldapServer.protocol) {
		case TLS:
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

	public static String checkMandatoryAttribute(IImportLogger importLogger, Entry entry, String attribute) {
		if (!entry.containsAttribute(attribute)) {
			importLogger.error(Messages.missingAttribute(entry.getDn(), attribute));

			throw new ServerFault("Unable to manage entry: " + entry.getDn() + ", missing attribute: " + attribute,
					ErrorCode.INVALID_PARAMETER);
		}

		String attrValue = null;
		try {
			attrValue = entry.get(attribute).getString();
		} catch (LdapInvalidAttributeValueException liave) {
			importLogger.error(Messages.attributeMustBeString(entry.getDn(), attribute));

			ServerFault sf = new ServerFault("Unable to manage entry: " + entry.getDn() + ", attribute: " + attribute
					+ " must be a string value", liave);
			sf.setCode(ErrorCode.INVALID_PARAMETER);
			throw sf;
		}

		if (attrValue.trim().isEmpty()) {
			importLogger.error(Messages.attributeMustNotBeEmpty(entry.getDn(), attribute));

			ServerFault sf = new ServerFault(
					"Unable to manage entry: " + entry.getDn() + ", attribute: " + attribute + " must not be empty");
			sf.setCode(ErrorCode.INVALID_PARAMETER);
			throw sf;
		}

		return attrValue;
	}
}