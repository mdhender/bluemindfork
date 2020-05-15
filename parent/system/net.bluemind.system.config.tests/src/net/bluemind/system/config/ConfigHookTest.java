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
package net.bluemind.system.config;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.Matchers;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.SystemConf;

public class ConfigHookTest {

	private ConfigHook getConfigHook() throws ServerFault {
		ConfigHook hook = spy(new ConfigHook());
		doReturn(Collections.emptyList()).when(hook).getTaggedServers(Matchers.<BmContext>any(),
				Matchers.<Set<String>>any());
		doReturn(Collections.emptyList()).when(hook).searchUpdaters();
		return hook;
	}

	@Test
	public void testDoExecuteIfMessageSizeLimitIsSet() throws ServerFault {
		ConfigHook hook = getConfigHook();
		SystemConf confOld = new SystemConf();
		Map<String, String> map = new HashMap<>();
		map.put("message_size_limit", "" + 500 * 1024 * 1024);
		confOld.values = map;
		SystemConf confNew = new SystemConf();
		Map<String, String> mapNew = new HashMap<>();
		mapNew.put("message_size_limit", "" + 100 * 1024 * 1024);
		confNew.values = mapNew;

		hook.onUpdated(null, confOld, confNew);

		verify(hook, times(1)).getTaggedServers(Matchers.<BmContext>any(), Matchers.<Set<String>>any());
	}

	@Test
	public void testDoNothingIfMessageSizeLimitIsNotSet() throws ServerFault {
		ConfigHook hook = getConfigHook();
		SystemConf conf = new SystemConf();

		hook.onUpdated(null, conf, conf);

		verify(hook, times(0)).getTaggedServers(Matchers.<BmContext>any(), Matchers.<Set<String>>any());
	}

	@Test
	public void testDoNothingIfMessageSizeLimitHasNotChanged() throws ServerFault {
		ConfigHook hook = getConfigHook();
		SystemConf conf = new SystemConf();
		Map<String, String> map = new HashMap<>();
		map.put("message_size_limit", "" + 500 * 1024 * 1024);
		conf.values = map;

		hook.onUpdated(null, conf, conf);

		verify(hook, times(0)).getTaggedServers(Matchers.<BmContext>any(), Matchers.<Set<String>>any());
	}

}
