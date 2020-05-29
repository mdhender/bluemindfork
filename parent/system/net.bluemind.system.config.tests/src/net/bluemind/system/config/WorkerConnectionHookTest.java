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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Matchers;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.nginx.NginxService;

public class WorkerConnectionHookTest {
	@Test
	public void validate_invalid() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.nginx_worker_connections.name(), "not an int");
		try {
			new WorkerConnectionHook().validate(null, modifications);
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals("nginx_worker_connections must be a valid integer", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void validate_valid() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.nginx_worker_connections.name(), "12");
		new WorkerConnectionHook().validate(null, modifications);
	}

	@Test
	public void onUpdated_noPreviousNoNew() {
		NginxService hook = spy(new NginxService());
		doNothing().when(hook).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook().onUpdated(null, new SystemConf(), new SystemConf());
		verify(hook, times(0)).updateWorkerConnection(Matchers.<String>any());
	}

	private SystemConf getSystemConf(String value) {
		Map<String, String> map = new HashMap<>();
		map.put(SysConfKeys.nginx_worker_connections.name(), value);

		return SystemConf.create(map);
	}

	@Test
	public void onUpdated_emptyOrNullPreviousAndNew() {
		NginxService hook = spy(new NginxService());
		doNothing().when(hook).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf(""), getSystemConf(""));
		verify(hook, times(0)).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf(""), getSystemConf(null));
		verify(hook, times(0)).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf(null), getSystemConf(""));
		verify(hook, times(0)).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf(null), getSystemConf(null));
		verify(hook, times(0)).updateWorkerConnection(Matchers.<String>any());
	}

	@Test
	public void onUpdated_samePreviousNoNew() {
		NginxService hook = spy(new NginxService());
		doNothing().when(hook).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf(null), getSystemConf(null));
		verify(hook, times(0)).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf(""), getSystemConf(""));
		verify(hook, times(0)).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf("12"), getSystemConf("12"));
		verify(hook, times(0)).updateWorkerConnection(Matchers.<String>any());
	}

	@Test
	public void onUpdated_update() {
		NginxService hook = spy(new NginxService());
		doNothing().when(hook).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf(null), getSystemConf("12"));
		verify(hook, times(1)).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf(""), getSystemConf("12"));
		verify(hook, times(2)).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf("12"), getSystemConf("15"));
		verify(hook, times(3)).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf("12"), getSystemConf(""));
		verify(hook, times(4)).updateWorkerConnection(Matchers.<String>any());

		new WorkerConnectionHook(hook).onUpdated(null, getSystemConf("12"), getSystemConf(null));
		verify(hook, times(5)).updateWorkerConnection(Matchers.<String>any());
	}
}
