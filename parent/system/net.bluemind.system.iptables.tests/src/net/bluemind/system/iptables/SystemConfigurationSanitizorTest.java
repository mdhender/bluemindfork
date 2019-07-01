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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.iptables.tools.SystemConfigurationSanitizor;

public class SystemConfigurationSanitizorTest {
	@Test
	public void inexistantKey() throws ServerFault {
		new SystemConfigurationSanitizor().sanitize(null, new HashMap<String, String>());
	}

	@Test
	public void emptyOrNull() throws ServerFault {
		Map<String, String> values = new HashMap<>();

		values.put(SysConfKeys.fwAdditionalIPs.name(), null);
		new SystemConfigurationSanitizor().sanitize(null, values);

		values.put(SysConfKeys.fwAdditionalIPs.name(), "");
		new SystemConfigurationSanitizor().sanitize(null, values);
	}

	@Test
	public void trim() throws ServerFault {
		Map<String, String> values = new HashMap<>();

		values.put(SysConfKeys.fwAdditionalIPs.name(), " test ");
		new SystemConfigurationSanitizor().sanitize(null, values);

		assertEquals("test", values.get(SysConfKeys.fwAdditionalIPs.name()));
	}

	@Test
	public void multipleSpaces() throws ServerFault {
		Map<String, String> values = new HashMap<>();

		values.put(SysConfKeys.fwAdditionalIPs.name(), "test   test2");
		new SystemConfigurationSanitizor().sanitize(null, values);

		assertEquals("test test2", values.get(SysConfKeys.fwAdditionalIPs.name()));
	}

	@Test
	public void unique() throws ServerFault {
		Map<String, String> values = new HashMap<>();

		values.put(SysConfKeys.fwAdditionalIPs.name(), "test test2 test");
		new SystemConfigurationSanitizor().sanitize(null, values);

		assertEquals("test test2", values.get(SysConfKeys.fwAdditionalIPs.name()));
	}
}
