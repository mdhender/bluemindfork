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
package net.bluemind.system.ldap.export.conf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.apache.directory.api.ldap.codec.api.ConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.export.LdapHelper;

public class DebSlapdConfigTests {
	private String ldapRoleServerIp = new BmConfIni().get("bluemind/ldap");
	private ItemValue<Server> ldapRoleServer;

	@Before
	public void before() throws Exception {
		getLdapRoleServer();
		updateUserPassword("admin0@global.virt", Token.admin0());
	}

	@Test
	public void testSlapdConfig_initHost() throws ServerFault, IOException {
		SlapdConfig.build(ldapRoleServer).init();

		try (LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer)) {
			assertNotNull(ldapCon);
			assertTrue(ldapCon.isConnected());
			assertTrue(ldapCon.isAuthenticated());
		}
	}

	@Test
	public void testSlapdConfig_checkSaslAuth() throws ServerFault, IOException, LdapException {
		updateUserPassword("test@domain.tld", "testpassword");

		SlapdConfig.build(ldapRoleServer).init();

		try (LdapConnection ldapCon = LdapHelper.connectDirectory(ldapRoleServer)) {
			ldapCon.add(new DefaultEntry("dc=local", "objectClass: organization", "objectClass: dcObject",
					"o: BlueMind", "description: BlueMind LDAP directory"));

			ldapCon.add(new DefaultEntry("uid=test,dc=local", "objectclass: inetOrgPerson", "sn: test", "cn: test",
					"userPassword: {SASL}test@domain.tld"));
		}

		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(ldapRoleServer.value.address());
		config.setLdapPort(389);
		config.setUseTls(true);
		config.setUseSsl(false);
		config.setTrustManagers(new NoVerificationTrustManager());

		config.setTimeout(10000);

		ConfigurableBinaryAttributeDetector detector = new DefaultConfigurableBinaryAttributeDetector();
		config.setBinaryAttributeDetector(detector);

		try (LdapConnection ldapCon = new LdapConProxy(config)) {
			assertNotNull(ldapCon);

			BindRequest bindRequest = new BindRequestImpl();
			bindRequest.setSimple(true);
			bindRequest.setName("uid=test,dc=local");
			bindRequest.setCredentials("testpassword");

			BindResponse response = ldapCon.bind(bindRequest);
			assertEquals(ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode());
			assertTrue(ldapCon.isConnected());
			assertTrue(ldapCon.isAuthenticated());
		}
	}

	private void updateUserPassword(String login, String passwd) {
		INodeClient nodeClient = NodeActivator.get(ldapRoleServerIp);
		NCUtils.exec(nodeClient, "/usr/local/sbin/updateUserPassword.sh " + login + " " + passwd);
	}

	private void getLdapRoleServer() {
		String uid = UUID.randomUUID().toString();

		Server lrs = new Server();
		lrs.ip = ldapRoleServerIp;
		lrs.tags = Collections.emptyList();

		ldapRoleServer = ItemValue.create(Item.create(uid, null), lrs);
	}
}
