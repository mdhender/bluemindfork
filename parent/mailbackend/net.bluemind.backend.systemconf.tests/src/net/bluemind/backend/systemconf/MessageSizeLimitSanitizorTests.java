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

package net.bluemind.backend.systemconf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.Test;

import net.bluemind.backend.systemconf.internal.MessageSizeLimitSanitizor;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SystemConf;

public class MessageSizeLimitSanitizorTests {
	private static final String PARAMETER = "message_size_limit";
	private static final String DEFAULT_VALUE = "10000000";

	@Test
	public void testSanitizeNullPrevious() throws ServerFault {
		MessageSizeLimitSanitizor msls = new MessageSizeLimitSanitizor();

		try {
			msls.sanitize(null, new HashMap<String, String>());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testSanitizeNullModifications() {
		MessageSizeLimitSanitizor msls = new MessageSizeLimitSanitizor();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<String, String>();

		try {
			msls.sanitize(systemConf, null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testSanitizeDefineDefaultNoPrevious() throws ServerFault {
		MessageSizeLimitSanitizor msls = new MessageSizeLimitSanitizor();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<String, String>();

		HashMap<String, String> modifications = new HashMap<String, String>();
		msls.sanitize(new SystemConf(), modifications);

		assertTrue(modifications.containsKey(PARAMETER));
		assertEquals(DEFAULT_VALUE, modifications.get(PARAMETER));
	}

	@Test
	public void testSanitizeNoMessageSizeLimitModifications() throws ServerFault {
		MessageSizeLimitSanitizor msls = new MessageSizeLimitSanitizor();

		HashMap<String, String> modifications = new HashMap<String, String>();
		msls.sanitize(new SystemConf(), modifications);

		assertFalse(modifications.containsKey(DEFAULT_VALUE));
	}

	@Test
	public void testSanitize() throws ServerFault {
		MessageSizeLimitSanitizor msls = new MessageSizeLimitSanitizor();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, " 987654321 ");
		msls.sanitize(new SystemConf(), modifications);

		assertTrue(modifications.containsKey(PARAMETER));
		assertEquals("987654321", modifications.get(PARAMETER));
	}
}
