/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.importation.commons.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Optional;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.Parameters.Directory;
import net.bluemind.system.importation.commons.Parameters.Server.Host;
import net.bluemind.system.importation.commons.Parameters.SplitDomain;
import net.bluemind.system.importation.commons.exceptions.DirectoryConnectionFailed;
import net.bluemind.system.importation.commons.pool.LdapPoolWrapper;
import net.bluemind.system.importation.commons.pool.TestServer;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper.DeleteTreeException;

public class DirectoryTesterTests {
	@BeforeClass
	public static void beforeClass() throws LdapInvalidDnException, LdapException, IOException, DeleteTreeException {
		LdapDockerTestHelper.initLdapServer();
		LdapDockerTestHelper.initLdapTree();
	}

	public class DirectoryTesterTest extends DirectoryTester {
		public DirectoryTesterTest(Parameters parameters) {
			super(parameters);
		}

		@Override
		protected LdapConnection getDirectoryConnection(Parameters parameters) {
			try {
				return new LdapPoolWrapper(parameters).getPool().getConnection();
			} catch (LdapException e) {
				throw new DirectoryConnectionFailed();
			}
		}
	}

	@Test
	public void success() {
		new DirectoryTesterTest(Parameters.build(true,
				new TestServer(Host.build(new BmConfIni().get(DockerContainer.LDAP.getName()), 389, 0, 0),
						"uid=admin,dc=local", "admin", LdapProtocol.PLAIN, true),
				Directory.build("dc=local", "userfilter", "groupfilter", "extidattr"),
				new SplitDomain(true, "relaymailboxgroup"), Optional.of("lastupdate"))).testDirectoryParameters();
	}

	@Test
	public void invalidRootDn() {
		try {
			new DirectoryTesterTest(Parameters.build(true,
					new TestServer(Host.build(new BmConfIni().get(DockerContainer.LDAP.getName()), 389, 0, 0),
							"uid=admin,dc=local", "admin", LdapProtocol.PLAIN, true),
					Directory.build("dc=invalid", "userfilter", "groupfilter", "extidattr"),
					new SplitDomain(true, "relaymailboxgroup"), Optional.of("lastupdate"))).testDirectoryParameters();
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
			assertTrue(sf.getMessage().contains("Base DN not found"));
		}
	}
}
