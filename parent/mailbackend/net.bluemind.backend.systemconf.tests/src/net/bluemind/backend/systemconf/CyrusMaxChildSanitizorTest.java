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

import net.bluemind.backend.systemconf.internal.CyrusMaxChildSanitizor;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SystemConf;

public class CyrusMaxChildSanitizorTest {
	private static final String PARAMETER = "imap_max_child";
	private static final String DEFAULT_VALUE = "200";

	@Test
	public void testSanitizeNullPrevious() throws ServerFault {
		CyrusMaxChildSanitizor msls = new CyrusMaxChildSanitizor();

		try {
			msls.sanitize(null, new HashMap<String, String>());
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testSanitizeNullModifications() {
		CyrusMaxChildSanitizor msls = new CyrusMaxChildSanitizor();

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
		CyrusMaxChildSanitizor msls = new CyrusMaxChildSanitizor();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<String, String>();

		HashMap<String, String> modifications = new HashMap<String, String>();
		msls.sanitize(new SystemConf(), modifications);

		assertTrue(modifications.containsKey(PARAMETER));
		assertEquals(DEFAULT_VALUE, modifications.get(PARAMETER));
	}

	@Test
	public void testSanitizeDefineDefaultNullOrEmptyPrevious() throws ServerFault {
		CyrusMaxChildSanitizor msls = new CyrusMaxChildSanitizor();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<String, String>();
		systemConf.values.put(PARAMETER, null);

		HashMap<String, String> modifications = new HashMap<String, String>();
		msls.sanitize(new SystemConf(), modifications);

		assertTrue(modifications.containsKey(PARAMETER));
		assertEquals(DEFAULT_VALUE, modifications.get(PARAMETER));

		systemConf = new SystemConf();
		systemConf.values = new HashMap<String, String>();
		systemConf.values.put(PARAMETER, "");

		modifications = new HashMap<String, String>();
		msls.sanitize(new SystemConf(), modifications);

		assertTrue(modifications.containsKey(PARAMETER));
		assertEquals(DEFAULT_VALUE, modifications.get(PARAMETER));
	}

	@Test
	public void testSanitizeDefineDefaultNullOrEmptyPreviousEmptyModification() throws ServerFault {
		CyrusMaxChildSanitizor msls = new CyrusMaxChildSanitizor();

		SystemConf systemConf = new SystemConf();
		systemConf.values = new HashMap<String, String>();
		systemConf.values.put(PARAMETER, null);

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, " ");
		msls.sanitize(new SystemConf(), modifications);

		assertTrue(modifications.containsKey(PARAMETER));
		assertEquals(DEFAULT_VALUE, modifications.get(PARAMETER));

		systemConf = new SystemConf();
		systemConf.values = new HashMap<String, String>();
		systemConf.values.put(PARAMETER, "");

		modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, " ");
		msls.sanitize(new SystemConf(), modifications);

		assertTrue(modifications.containsKey(PARAMETER));
		assertEquals(DEFAULT_VALUE, modifications.get(PARAMETER));
	}

	@Test
	public void testSanitizeNoMessageSizeLimitModifications() throws ServerFault {
		CyrusMaxChildSanitizor msls = new CyrusMaxChildSanitizor();

		HashMap<String, String> modifications = new HashMap<String, String>();
		msls.sanitize(new SystemConf(), modifications);

		assertFalse(modifications.containsKey(DEFAULT_VALUE));
	}

	@Test
	public void testSanitize() throws ServerFault {
		CyrusMaxChildSanitizor msls = new CyrusMaxChildSanitizor();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, " 1000 ");
		msls.sanitize(new SystemConf(), modifications);

		assertTrue(modifications.containsKey(PARAMETER));
		assertEquals("1000", modifications.get(PARAMETER));
	}
}
