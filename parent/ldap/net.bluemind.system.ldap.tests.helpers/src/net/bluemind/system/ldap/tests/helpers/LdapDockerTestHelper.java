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
package net.bluemind.system.ldap.tests.helpers;

import java.io.IOException;
import java.io.InputStream;

import org.apache.directory.api.ldap.codec.decorators.SearchResultEntryDecorator;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.system.importation.search.PagedSearchResult;

public class LdapDockerTestHelper {
	public static final String LDAP_ROOT_DN = "dc=local";
	public static final String LDAP_LOGIN_DN = "uid=admin,dc=local";
	public static final String LDAP_LOGIN_PWD = "admin";
	private static final Logger logger = LoggerFactory.getLogger(LdapDockerTestHelper.class);

	public static void initLdapTree(Class<? extends Object> classObject, TestName testName)
			throws LdapException, DeleteTreeException, IOException {
		initLdapTree(classObject,
				"/resources/" + classObject.getSimpleName() + "/" + testName.getMethodName() + ".ldif");
	}

	public static void initLdapTree(Class<? extends Object> classObject, String resourceName)
			throws LdapInvalidDnException, LdapException, DeleteTreeException, IOException {
		LdapNetworkConnection ldapCon = getLdapCon();

		if (ldapCon.exists(new Dn(LDAP_ROOT_DN))) {
			deleteTree(ldapCon, LDAP_ROOT_DN);
		}

		createLdapEntry(ldapCon, new LdapDockerTestHelper().getClass().getResourceAsStream("/resources/local.ldif"));

		InputStream ldifIS = classObject.getResourceAsStream(resourceName);
		if (ldifIS == null) {
			System.out.println(resourceName + " doesn't exist!");
			return;
		}

		createLdapEntry(ldapCon, ldifIS);

		ldapCon.close();
	}

	private static void createLdapEntry(LdapNetworkConnection ldapCon, InputStream ldifIS)
			throws LdapInvalidDnException, LdapException, IOException {
		LdifReader lr = new LdifReader(ldifIS);
		for (LdifEntry e : lr) {
			ldapCon.add(e.getEntry());
		}

		lr.close();
	}

	public static LdapNetworkConnection getLdapCon() throws LdapException {
		String host = new BmConfIni().get(DockerContainer.LDAP.getName());
		logger.info("LDAP connection to {}", host);
		LdapConnectionConfig lcc = new LdapConnectionConfig();
		lcc.setLdapHost(host);
		lcc.setLdapPort(389);
		lcc.setTimeout(10000);
		lcc.setUseSsl(false);
		lcc.setUseTls(false);

		BindRequest bindRequest = new BindRequestImpl();
		bindRequest.setSimple(true);
		bindRequest.setName(LDAP_LOGIN_DN);
		bindRequest.setCredentials(LDAP_LOGIN_PWD);

		LdapNetworkConnection ldapCon = new LdapNetworkConnection(lcc);

		BindResponse response = ldapCon.bind(bindRequest);

		if (ResultCodeEnum.SUCCESS != response.getLdapResult().getResultCode() || !ldapCon.isAuthenticated()) {
			throw new ServerFault("LDAP connection failed: " + response.getLdapResult().getDiagnosticMessage());
		}

		return ldapCon;
	}

	@SuppressWarnings("serial")
	public static class DeleteTreeException extends Exception {
		public DeleteTreeException(String msg, Exception e) {
			super(msg, e);
		}
	}

	public static void deleteTree(LdapConnection ldapCon, String dn) throws DeleteTreeException {
		SearchCursor cursor = null;
		Entry entry = null;
		try {
			SearchRequest searchRequest = new SearchRequestImpl();
			searchRequest.setBase(new Dn(dn));
			searchRequest.setScope(SearchScope.ONELEVEL);
			searchRequest.setFilter("(objectclass=*)");
			searchRequest.addAttributes("dn");
			searchRequest.setDerefAliases(AliasDerefMode.NEVER_DEREF_ALIASES);

			PagedSearchResult pagesSearchResult = new PagedSearchResult(ldapCon, searchRequest);

			cursor = ldapCon.search(searchRequest);

			while (pagesSearchResult.next()) {
				Response response = pagesSearchResult.get();

				if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					continue;
				}

				entry = ((SearchResultEntryDecorator) response).getEntry();
				deleteTree(ldapCon, entry.getDn().getName());
			}

			ldapCon.delete(dn);
		} catch (DeleteTreeException dte) {
			throw dte;
		} catch (Exception e) {
			throw new DeleteTreeException("Fail to delete LDAP entry: " + dn, e);
		} finally {
			try {
				cursor.close();
			} catch (Exception e) {
			}
		}
	}

	public static void initLdapServer() {
		initLdapServer(true);
	}

	public static void initLdapServer(boolean memberOfOverlay) {
		initLdapServer(new LdapDockerTestHelper().getClass().getResourceAsStream("/resources/serverConfig.ldif"),
				memberOfOverlay);
	}

	public static void initLdapServer(InputStream serverConfig, boolean memberOfOverlay) {
		INodeClient nodeClient = NodeActivator.get(new BmConfIni().get(DockerContainer.LDAP.getName()));

		NCUtils.exec(nodeClient, "service slapd stop");
		nodeClient.writeFile("/tmp/serverConfig.ldif", serverConfig);
		nodeClient.writeFile("/tmp/serverConfig-overlay-memberOf.ldif", new LdapDockerTestHelper().getClass()
				.getResourceAsStream("/resources/serverConfig-overlay-memberOf.ldif"));
		nodeClient.writeFile("/etc/default/slapd",
				new LdapDockerTestHelper().getClass().getResourceAsStream("/resources/slapd.default"));

		NCUtils.exec(nodeClient, "rm -rf /etc/ldap/slapd.d");
		NCUtils.exec(nodeClient, "mkdir -p /etc/ldap/slapd.d");
		NCUtils.exec(nodeClient, "/usr/sbin/slapadd -F /etc/ldap/slapd.d -b cn=config -l /tmp/serverConfig.ldif");
		if (memberOfOverlay) {
			NCUtils.exec(nodeClient,
					"/usr/sbin/slapadd -F /etc/ldap/slapd.d -b cn=config -l /tmp/serverConfig-overlay-memberOf.ldif");
		}
		NCUtils.exec(nodeClient, "chown -R openldap:openldap /etc/ldap/slapd.d");

		NCUtils.exec(nodeClient, "rm -rf /var/lib/ldap");
		NCUtils.exec(nodeClient, "mkdir -p /var/lib/ldap");
		NCUtils.exec(nodeClient, "chown -R openldap:openldap /var/lib/ldap");
		NCUtils.exec(nodeClient, "chown -R openldap:openldap /var/lib/ldap");

		NCUtils.exec(nodeClient,
				"openssl req -x509 -newkey rsa:4096 -keyout /etc/ssl/certs/bm_cert.pem -out /etc/ssl/certs/bm_cert.pem -days 365 -subj '/CN=localhost' -nodes");

		NCUtils.exec(nodeClient, "service slapd start");
	}
}
