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
package net.bluemind.system.ldap.export;

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.codec.api.ConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.export.objects.DomainDirectoryRoot;

public class LdapHelper {
	private static final Logger logger = LoggerFactory.getLogger(LdapHelper.class);

	public static final String DIRECTORY_ROOT_DN = "uid=admin,dc=local";
	private static final String CONFIG_ROOT_DN = "uid=admin,cn=config";

	public static LdapConnection connectDirectory(ItemValue<Server> ldapHost) throws ServerFault {
		return connectLdap(ldapHost, DIRECTORY_ROOT_DN, Token.admin0());
	}

	public static LdapConnection connectConfigDirectory(ItemValue<Server> ldapHost) throws ServerFault {
		return connectLdap(ldapHost, CONFIG_ROOT_DN, Token.admin0());
	}

	private static LdapConnection connectLdap(ItemValue<Server> ldapHost, String configDirectoryRootDn,
			String credential) throws ServerFault {
		LdapConnection ldapCon;

		try {
			ldapCon = getLdapCon(ldapHost.value.address());

			BindRequest bindRequest = new BindRequestImpl();
			bindRequest.setSimple(true);
			bindRequest.setName(configDirectoryRootDn);
			bindRequest.setCredentials(credential);

			BindResponse response = ldapCon.bind(bindRequest);

			if (ResultCodeEnum.SUCCESS != response.getLdapResult().getResultCode() || !ldapCon.isAuthenticated()) {
				throw new ServerFault("Fail to authenticate to LDAP server: " + ldapHost.value.address());
			}
		} catch (Exception e) {
			throw new ServerFault(e);
		}

		return ldapCon;
	}

	private static LdapConProxy getLdapCon(String adHostname) {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(adHostname);
		config.setLdapPort(389);
		config.setUseTls(true);
		config.setUseSsl(false);
		config.setTrustManagers(new NoVerificationTrustManager());

		config.setTimeout(10000);

		ConfigurableBinaryAttributeDetector detector = new DefaultConfigurableBinaryAttributeDetector();
		config.setBinaryAttributeDetector(detector);

		return new LdapConProxy(config);
	}

	public static void addLdapEntry(LdapConnection ldapCon, Entry ldapEntry) throws ServerFault {
		if (ldapEntry == null) {
			return;
		}

		try {
			ldapCon.add(ldapEntry);
		} catch (LdapEntryAlreadyExistsException e) {
			logger.warn(ldapEntry.getDn().getName() + " already exist");
		} catch (LdapException e) {
			logger.error("Fail to add entry DN: " + ldapEntry.getDn(), e);
			throw new ServerFault(e);
		}
	}

	public static void deleteTree(LdapConnection ldapCon, String dn) throws ServerFault {
		SearchCursor cursor = null;
		Entry entry = null;
		try {
			SearchRequest searchRequest = new SearchRequestImpl();
			searchRequest.setBase(new Dn(dn));
			searchRequest.setScope(SearchScope.ONELEVEL);
			searchRequest.setFilter("(objectclass=*)");
			searchRequest.addAttributes("dn");
			searchRequest.setDerefAliases(AliasDerefMode.NEVER_DEREF_ALIASES);
			cursor = ldapCon.search(searchRequest);

			while (cursor.next()) {
				Response response = cursor.get();

				if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					continue;
				}

				entry = ((SearchResultEntry) response).getEntry();
				deleteTree(ldapCon, entry.getDn().getName());
			}

			ldapCon.delete(dn);
		} catch (ServerFault sf) {
			throw sf;
		} catch (Exception e) {
			throw new ServerFault("Fail to delete LDAP entry: " + dn);
		} finally {
			try {
				cursor.close();
			} catch (Exception e) {
			}
		}
	}

	public static List<Entry> getLdapEntryFromUid(LdapConnection ldapCon, ItemValue<Domain> domain, String uid,
			String... attrs) throws LdapException, CursorException {
		List<Entry> entries = new ArrayList<>();

		SearchRequestImpl sr = new SearchRequestImpl();
		sr.setScope(SearchScope.SUBTREE);
		sr.setBase(new Dn(new DomainDirectoryRoot(domain).getDn()));
		sr.setFilter("(bmUid=" + uid + ")");
		sr.addAttributes(attrs);

		SearchCursor cursor = ldapCon.search(sr);
		while (cursor.next()) {
			Response response = cursor.get();

			if (response.getType() == MessageTypeEnum.SEARCH_RESULT_ENTRY) {
				entries.add(((SearchResultEntry) response).getEntry());
			}
		}

		LdapResult ldapResult = cursor.getSearchResultDone().getLdapResult();
		if (ldapResult.getResultCode() != ResultCodeEnum.SUCCESS) {
			throw new ServerFault("Error on search for bmUid:" + uid + " - " + ldapResult.getResultCode() + " "
					+ ldapResult.getDiagnosticMessage());
		}

		return entries;
	}

	public static void modifyLdapEntry(LdapConnection ldapCon, ModifyRequest modifyRequest) throws ServerFault {
		if (modifyRequest == null || modifyRequest.getModifications().isEmpty()) {
			return;
		}

		try {
			ModifyResponse mr = ldapCon.modify(modifyRequest);
			LdapResult result = mr.getLdapResult();

			if (result.getResultCode() != ResultCodeEnum.SUCCESS) {
				throw new ServerFault(
						"Modify failed: " + result.getResultCode() + " => " + result.getDiagnosticMessage());
			}
		} catch (LdapException e) {
			logger.error("Fail to update entry DN: " + modifyRequest.getName().getName(), e);
			throw new ServerFault(e);
		}
	}
}
