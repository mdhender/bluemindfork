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
package net.bluemind.system.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;

public class SwPasswordHookTest {
	@Test
	public void sanitize_noKey() {
		Map<String, String> modifications = new HashMap<>();
		new SwPasswordHook().sanitize(null, modifications);
		assertFalse(modifications.containsKey(SysConfKeys.sw_password.name()));
	}

	@Test
	public void sanitize_nullValue() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.sw_password.name(), null);

		new SwPasswordHook().sanitize(null, modifications);
		assertTrue(modifications.containsKey(SysConfKeys.sw_password.name()));
		assertNull(modifications.get(SysConfKeys.sw_password.name()));
	}

	@Test
	public void sanitize_trimValue() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.sw_password.name(), " value ");

		new SwPasswordHook().sanitize(null, modifications);
		assertEquals("value", modifications.get(SysConfKeys.sw_password.name()));
	}

	@Test
	public void validate_noKey() {
		Map<String, String> modifications = new HashMap<>();
		new SwPasswordHook().validate(null, modifications);
	}

	@Test
	public void validate_nullOrEmptyValue() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.sw_password.name(), null);
		try {
			new SwPasswordHook().validate(null, modifications);
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals("SetupWizard password must not be null or empty!", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		modifications = new HashMap<>();
		modifications.put(SysConfKeys.sw_password.name(), "");
		try {
			new SwPasswordHook().validate(null, modifications);
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals("SetupWizard password must not be null or empty!", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void validate_invalid() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.sw_password.name(), "with a '");
		try {
			new SwPasswordHook().validate(null, modifications);
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals("\"'\" is a forbidden character in this password", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void validate_valid() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.sw_password.name(), "valid.test.lan");
		new SwPasswordHook().validate(null, modifications);
	}
}
