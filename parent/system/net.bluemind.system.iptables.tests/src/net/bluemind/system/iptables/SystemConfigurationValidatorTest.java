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
package net.bluemind.system.iptables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.iptables.tools.SystemConfigurationValidator;

public class SystemConfigurationValidatorTest {
	@Test
	public void inexistantKey() throws ServerFault {
		new SystemConfigurationValidator().validate(null, new HashMap<String, String>());
	}

	@Test
	public void nullOrEmptyKey() throws ServerFault {
		Map<String, String> values = new HashMap<>();

		values.put(SysConfKeys.fwAdditionalIPs.name(), null);
		new SystemConfigurationValidator().validate(null, values);

		values.put(SysConfKeys.fwAdditionalIPs.name(), "");
		new SystemConfigurationValidator().validate(null, values);

		values.put(SysConfKeys.fwAdditionalIPs.name(), "   ");
		new SystemConfigurationValidator().validate(null, values);
	}

	@Test
	public void validIp() throws ServerFault {
		Map<String, String> values = new HashMap<>();

		values.put(SysConfKeys.fwAdditionalIPs.name(), "10.0.0.1");
		new SystemConfigurationValidator().validate(null, values);

		values.put(SysConfKeys.fwAdditionalIPs.name(), "10.0.0.1/32");
		new SystemConfigurationValidator().validate(null, values);

		values.put(SysConfKeys.fwAdditionalIPs.name(), "10.0.0.1/255.255.255.0");
		new SystemConfigurationValidator().validate(null, values);

		values.put(SysConfKeys.fwAdditionalIPs.name(), "10.0.0.1 192.168.1.1/32");
		new SystemConfigurationValidator().validate(null, values);
	}

	@Test
	public void invalidSyntax() throws ServerFault {
		Map<String, String> values = new HashMap<>();

		values.put(SysConfKeys.fwAdditionalIPs.name(), "10.0.0.1 test");
		mustThrownAnException(values, "test");

		values.put(SysConfKeys.fwAdditionalIPs.name(), "10.0.0.1/0");
		mustThrownAnException(values, "10.0.0.1/0");

		values.put(SysConfKeys.fwAdditionalIPs.name(), "10.0.0.1/99");
		mustThrownAnException(values, "10.0.0.1/99");

		values.put(SysConfKeys.fwAdditionalIPs.name(), "10.0.0.1/255.255.255.256");
		mustThrownAnException(values, "10.0.0.1/255.255.255.256");

		values.put(SysConfKeys.fwAdditionalIPs.name(), "10.0.0.1/");
		mustThrownAnException(values, "10.0.0.1/");

		values.put(SysConfKeys.fwAdditionalIPs.name(), "10.0.0.256");
		mustThrownAnException(values, "10.0.0.256");

		values.put(SysConfKeys.fwAdditionalIPs.name(), "10.0.0.1/32/32");
		mustThrownAnException(values, "10.0.0.1/32/32");
	}

	private void mustThrownAnException(Map<String, String> values, String expectedInvalidIp) {
		try {
			new SystemConfigurationValidator().validate(null, values);
			fail("Test must thrown an exception !");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());

			if (expectedInvalidIp != null && !expectedInvalidIp.isEmpty()) {
				assertEquals("Invalid IP: " + expectedInvalidIp, sf.getMessage());
			}
		}
	}
}
