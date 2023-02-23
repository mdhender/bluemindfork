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
import static org.junit.Assert.fail;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.smime.cacerts.api.SmimeCertClient;
import net.bluemind.smime.cacerts.service.internal.SmimeCertClientSanitizer;

public class SmimeCertClientSanitizerTests {

	private SmimeCertClientSanitizer sanitizer = new SmimeCertClientSanitizer();

	@Test
	public void sanitize() throws ServerFault {
		SmimeCertClient cert = null;

		// cert null
		try {
			sanitizer.create(cert);
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		cert = new SmimeCertClient();
		String sn = "serial-number";
		cert.serialNumber = sn;
		cert.issuer = "issuer";
		try {
			sanitizer.create(cert);
			assertEquals(sn.toUpperCase(), cert.serialNumber);
		} catch (ServerFault e) {
			fail(e.getMessage());
		}

	}

}
