/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.core.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.utils.ErrorLogBuilder;
import net.bluemind.core.tests.BmTestContext;

public class ErrorLogBuilderTest {

	@Test
	public void testLogPermissionDenied() {
		BmTestContext anon = new BmTestContext(SecurityContext.ANONYMOUS, null);
		RBACManager rbac = RBACManager.forContext(anon);
		try {
			rbac.check("hasAdminConsole");
			fail();
		} catch (Exception e) {
			assertEquals(
					"  class net.bluemind.core.api.fault.ServerFault: anonymous@null Doesnt have role hasAdminConsole\n",
					ErrorLogBuilder.build(e));
		}

	}

}
