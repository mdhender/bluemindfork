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

public class OtherUrlHookTest {
	@Test
	public void sanitize_noKey() {
		Map<String, String> modifications = new HashMap<>();
		new OtherUrlHook().sanitize(null, modifications);
		assertFalse(modifications.containsKey(SysConfKeys.other_urls.name()));
	}

	@Test
	public void sanitize_nullOrEmptyValue() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.other_urls.name(), null);

		new OtherUrlHook().sanitize(null, modifications);
		assertTrue(modifications.containsKey(SysConfKeys.other_urls.name()));
		assertNull(modifications.get(SysConfKeys.other_urls.name()));

		modifications = new HashMap<>();
		modifications.put(SysConfKeys.other_urls.name(), "");

		new OtherUrlHook().sanitize(null, modifications);
		assertTrue(modifications.containsKey(SysConfKeys.other_urls.name()));
		assertNull(modifications.get(SysConfKeys.other_urls.name()));
	}

	@Test
	public void sanitize_trimValue() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.other_urls.name(), " value1  value2 ");

		new OtherUrlHook().sanitize(null, modifications);
		assertEquals("value1 value2", modifications.get(SysConfKeys.other_urls.name()));
	}

	@Test
	public void validate_noKey() {
		Map<String, String> modifications = new HashMap<>();
		new OtherUrlHook().validate(null, modifications);
	}

	@Test
	public void validate_nullOrEmptyValue() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.other_urls.name(), null);
		new OtherUrlHook().validate(null, modifications);

		modifications = new HashMap<>();
		modifications.put(SysConfKeys.other_urls.name(), "");
		try {
			new OtherUrlHook().validate(null, modifications);
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals("Invalid URL ''", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void validate_invalid() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.other_urls.name(), "valid1.other.url.tld not-a-FQDN");
		try {
			new OtherUrlHook().validate(null, modifications);
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals("Invalid URL 'not-a-FQDN'", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void validate_valid() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.other_urls.name(), "valid1.other.url.tld valid2.other.url.tld");
		new OtherUrlHook().validate(null, modifications);
	}
}
