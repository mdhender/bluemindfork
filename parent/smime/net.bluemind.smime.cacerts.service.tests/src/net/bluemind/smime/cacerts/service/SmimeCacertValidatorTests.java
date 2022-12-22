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

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.service.internal.SmimeCacertValidator;

public class SmimeCacertValidatorTests {

	private SmimeCacertValidator validator = new SmimeCacertValidator();

	@Test
	public void validate() throws ServerFault {
		SmimeCacert cert = null;

		// cert null
		try {
			validator.create(cert);
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

}
