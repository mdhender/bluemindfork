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
package net.bluemind.system.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.ISecurityMgmt;
import net.bluemind.system.service.internal.ValidatorHook;

public class SecurityTests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		ValidatorHook.throwException = false;
	}

	@Test
	public void updateFirewallRulesAsAnonymous() {
		try {
			service(SecurityContext.ANONYMOUS).updateFirewallRules();
			fail("Only global domain users can update firewall");
		} catch (ServerFault e) {
			System.out.println(e.getMessage());
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	private ISecurityMgmt service(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(ISecurityMgmt.class);
	}
}
