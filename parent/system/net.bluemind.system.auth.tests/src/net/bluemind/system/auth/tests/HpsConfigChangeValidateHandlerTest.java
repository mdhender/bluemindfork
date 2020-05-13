/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.system.auth.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.auth.HpsConfigChangeValidateHandler;

public class HpsConfigChangeValidateHandlerTest {
	@Test
	public void validate() {
		Map<String, String> modifications = new HashMap<>();
		new HpsConfigChangeValidateHandler().validate(null, modifications);

		modifications.put(SysConfKeys.hps_max_sessions_per_user.name(), null);
		try {
			new HpsConfigChangeValidateHandler().validate(null, modifications);
			fail("Must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertEquals("Max HPS sessions per user must be an integer", sf.getMessage());
		}

		modifications.put(SysConfKeys.hps_max_sessions_per_user.name(), "invalid");
		try {
			new HpsConfigChangeValidateHandler().validate(null, modifications);
			fail("Must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertEquals("Max HPS sessions per user must be an integer", sf.getMessage());
		}

		modifications.put(SysConfKeys.hps_max_sessions_per_user.name(), "0");
		try {
			new HpsConfigChangeValidateHandler().validate(null, modifications);
			fail("Must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertEquals("Max HPS sessions per user must be greater than 0 - default to 5", sf.getMessage());
		}

		modifications.put(SysConfKeys.hps_max_sessions_per_user.name(), "10");
	}
}
