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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.Test;

import net.bluemind.backend.systemconf.internal.MyNetworksValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class MyNetworksValidatorTests {
	private static final String PARAMETER = "mynetworks";

	@Test
	public void testValidateNullModifications() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		try {
			mnv.validate(null, null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testValidateIpv4Address() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "192.168.1.1");
		mnv.validate(null, modifications);
	}

	@Test
	public void testValidateNullIp() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, null);

		try {
			mnv.validate(null, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testValidateEmptyIp() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, null);

		try {
			mnv.validate(null, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testValidateNotDefined() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER + "-fake", "value");
		mnv.validate(null, modifications);
	}

	@Test
	public void testValidateIpv6Address() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "2001:28f8:34:271:218:8bEa:fe2a:56a4");
		mnv.validate(null, modifications);
	}

	@Test
	public void testValidateIpv4Cidr() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "192.168.1.1/24");
		mnv.validate(null, modifications);
	}

	@Test
	public void testValidateIpv6Cidr() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "2001:28f8:34:271::/64");
		mnv.validate(null, modifications);
	}

	@Test
	public void testValidateIpSpaceSeparated() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "192.168.1.1 192.168.1.1/24");
		mnv.validate(null, modifications);
	}

	@Test
	public void testValidateCommaSeparated() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "192.168.1.1,192.168.1.1/24");
		mnv.validate(null, modifications);
	}

	@Test
	public void testValidateIpCommaAndSpaceSeparated() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "192.168.1.1, 192.168.1.1/24");
		mnv.validate(null, modifications);
	}

	@Test
	public void testValidateIpMixedSeparatorSeparated() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "192.168.1.1, 192.168.1.1/24 192.168.2.0/32");
		mnv.validate(null, modifications);
	}

	@Test
	public void testValidateStrangeButValidSeparated() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "192.168.1.1  ,    192.168.1.1/24    192.168.2.0/32");
		mnv.validate(null, modifications);
	}

	@Test
	public void testValidateExcludeIp() throws ServerFault {
		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, "!192.168.1.1");
		mnv.validate(null, modifications);
	}

	@Test
	public void testValidateInvalidIp() throws ServerFault {
		String testValue = "192.168.x";

		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, testValue);

		try {
			mnv.validate(null, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains(testValue));
		}
	}

	@Test
	public void testValidateInvalidIpv4Netmask() throws ServerFault {
		String testValue = "192.168.1.1/45";

		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, testValue);

		try {
			mnv.validate(null, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains(testValue));
		}
	}

	@Test
	public void testValidateInvalidIpv6Netmask() throws ServerFault {
		String testValue = "2001:28f8:34:271::/164";

		MyNetworksValidator mnv = new MyNetworksValidator();

		HashMap<String, String> modifications = new HashMap<String, String>();
		modifications.put(PARAMETER, testValue);

		try {
			mnv.validate(null, modifications);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().contains(testValue));
		}
	}
}
