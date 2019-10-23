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
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.Test;

import net.bluemind.backend.systemconf.internal.CyrusMaxChildValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class CyrusMaxChildValidatorTest {
	private static final String PARAMETER = "imap_max_child";

	@Test
	public void testValidateNullModifications() throws ServerFault {
		CyrusMaxChildValidator mslv = new CyrusMaxChildValidator();

		try {
			mslv.validate(null, null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testValidateNotDefined() throws ServerFault {
		CyrusMaxChildValidator mslv = new CyrusMaxChildValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER + "-fake", "value");
		mslv.validate(null, modifications);
	}

	@Test
	public void testValidateNullMessageSize() throws ServerFault {
		CyrusMaxChildValidator mslv = new CyrusMaxChildValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, null);

		try {
			mslv.validate(null, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testValidateEmptyMessageSize() throws ServerFault {
		CyrusMaxChildValidator mslv = new CyrusMaxChildValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "");

		try {
			mslv.validate(null, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testValidateInvalidMessageSize() throws ServerFault {
		CyrusMaxChildValidator mslv = new CyrusMaxChildValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "invalid");

		try {
			mslv.validate(null, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testValidate() throws ServerFault {
		CyrusMaxChildValidator mslv = new CyrusMaxChildValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "1000");

		mslv.validate(null, modifications);

		assertEquals("1000", modifications.get(PARAMETER));
	}

	@Test
	public void testValidateNotTrimed() throws ServerFault {
		CyrusMaxChildValidator mslv = new CyrusMaxChildValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, " 1000 ");

		mslv.validate(null, modifications);

		assertEquals(" 1000 ", modifications.get(PARAMETER));
	}

	@Test
	public void testValidateMinValue() throws ServerFault {
		CyrusMaxChildValidator mslv = new CyrusMaxChildValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "0");

		try {
			mslv.validate(null, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}
}
