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

public class AllowBmEmbedTest {
	@Test
	public void validate_invalid() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.allow_bm_embed.name(), "not a boolean");
		try {
			new AllowBmEmbedHook().validate(null, modifications);
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals("allow_bm_embed must be true or false", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void validate_valid() {
		new AllowBmEmbedHook().validate(null, new HashMap<>());

		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.allow_bm_embed.name(), "true");
		new AllowBmEmbedHook().validate(null, modifications);

		modifications.put(SysConfKeys.allow_bm_embed.name(), "false");
		new AllowBmEmbedHook().validate(null, modifications);

		modifications.put(SysConfKeys.allow_bm_embed.name(), null);
		new AllowBmEmbedHook().validate(null, modifications);

		modifications.put(SysConfKeys.allow_bm_embed.name(), "");
		new AllowBmEmbedHook().validate(null, modifications);
	}

	@Test
	public void onUpdated_noPreviousNoNew() {
		NginxService hook = spy(new NginxService());
		doNothing().when(hook).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook().onUpdated(null, new SystemConf(), new SystemConf());
		verify(hook, times(0)).updateAllowBmEmbed(Matchers.anyBoolean());
	}

	private SystemConf getSystemConf(String value) {
		Map<String, String> map = new HashMap<>();
		map.put(SysConfKeys.allow_bm_embed.name(), value);

		return SystemConf.create(map);
	}

	@Test
	public void onUpdated_emptyOrNullPreviousAndNew() {
		NginxService hook = spy(new NginxService());
		doNothing().when(hook).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf(""), getSystemConf(""));
		verify(hook, times(0)).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf(""), getSystemConf(null));
		verify(hook, times(0)).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf(null), getSystemConf(""));
		verify(hook, times(0)).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf(null), getSystemConf(null));
		verify(hook, times(0)).updateAllowBmEmbed(Matchers.anyBoolean());
	}

	@Test
	public void onUpdated_samePreviousNoNew() {
		NginxService hook = spy(new NginxService());
		doNothing().when(hook).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf(null), getSystemConf(null));
		verify(hook, times(0)).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf("true"), getSystemConf("true"));
		verify(hook, times(0)).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf("false"), getSystemConf("false"));
		verify(hook, times(0)).updateAllowBmEmbed(Matchers.anyBoolean());
	}

	@Test
	public void onUpdated_update() {
		NginxService hook = spy(new NginxService());
		doNothing().when(hook).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf(null), getSystemConf("true"));
		verify(hook, times(1)).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf("false"), getSystemConf("true"));
		verify(hook, times(2)).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf("true"), getSystemConf("false"));
		verify(hook, times(3)).updateAllowBmEmbed(Matchers.anyBoolean());

		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf("true"), getSystemConf(null));
		verify(hook, times(4)).updateAllowBmEmbed(Matchers.anyBoolean());

		// false == null
		new AllowBmEmbedHook(hook).onUpdated(null, getSystemConf("false"), getSystemConf(null));
		verify(hook, times(4)).updateAllowBmEmbed(Matchers.anyBoolean());
	}
}
