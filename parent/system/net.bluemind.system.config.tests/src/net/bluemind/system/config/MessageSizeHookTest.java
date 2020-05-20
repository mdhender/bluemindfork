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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.api.SysConfKeys;

public class MessageSizeHookTest {
	@Test
	public void validate_invalid() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.message_size_limit.name(), "not an int");
		try {
			new MessageSizeHook().validate(null, modifications);
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals("message_size_limit must be a valid integer", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		modifications = new HashMap<>();
		modifications.put(GlobalSettingsKeys.filehosting_max_filesize.name(), "not an int");
		try {
			new MessageSizeHook().validate(null, modifications);
			fail("Test must thrown an exception!");
		} catch (ServerFault sf) {
			assertEquals("filehosting_max_filesize must be a valid integer", sf.getMessage());
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void validate_valid() {
		Map<String, String> modifications = new HashMap<>();
		modifications.put(SysConfKeys.message_size_limit.name(), "12");
		modifications.put(GlobalSettingsKeys.filehosting_max_filesize.name(), "10");
		new MessageSizeHook().validate(null, modifications);
	}
}
