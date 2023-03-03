/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeRevocation;
import net.bluemind.smime.cacerts.api.RevocationResult;

public class SmimeRevocationServiceTests extends AbstractServiceTests {

	@Test
	public void test_isRevoked() {
		final List<String> snList = Arrays.asList("1");
		// test anonymous
		try {
			getServiceCrl(SecurityContext.ANONYMOUS, domainUid).isRevoked(snList);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			List<RevocationResult> revoked = getServiceCrl(defaultSecurityContext, domainUid).isRevoked(snList);
			assertNull(revoked);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}
	}

	@Override
	protected ISmimeRevocation getServiceCrl(SecurityContext context, String domainUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ISmimeRevocation.class, domainUid);
	}

	@Override
	protected ISmimeCACert getService(SecurityContext context, String containerUid) throws ServerFault {
		return null;
	}
}
