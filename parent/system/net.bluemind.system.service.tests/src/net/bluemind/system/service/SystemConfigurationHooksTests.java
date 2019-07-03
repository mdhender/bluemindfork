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
package net.bluemind.system.service;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.service.internal.ObserverHook;
import net.bluemind.system.service.internal.SanitizorHook;
import net.bluemind.system.service.internal.SystemConfigurationHooks;
import net.bluemind.system.service.internal.ValidatorHook;

public class SystemConfigurationHooksTests {

	@Test
	public void testUpdates() throws Exception {
		ObserverHook.called = false;
		SystemConfigurationHooks.getInstance().fireUpdated(new BmTestContext(SecurityContext.SYSTEM), new SystemConf(),
				new SystemConf());

		assertTrue(ObserverHook.called);
	}

	@Test
	public void testSanitize() throws Exception {
		SanitizorHook.called = false;
		SystemConfigurationHooks.getInstance().sanitize(new SystemConf(), new HashMap<String, String>());

		assertTrue(SanitizorHook.called);
	}

	@Test
	public void testValidate() throws Exception {
		ValidatorHook.called = false;
		SystemConfigurationHooks.getInstance().validate(new SystemConf(), new HashMap<String, String>());
		assertTrue(ValidatorHook.called);
	}
}
