/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.auth.tests;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.system.api.SystemConf;
import net.bluemind.system.auth.DefaultDomainConfigurationObserver;
import net.bluemind.system.auth.DefaultDomainConfigurationObserver.Action;

public class DefaultDomainObserverTest {

	private DefaultDomainConfigurationObserver obs = new DefaultDomainConfigurationObserver();

	@Test
	public void testNothing() {
		SystemConf previous = SystemConf.create(new HashMap<String, String>());
		SystemConf current = SystemConf.create(new HashMap<String, String>());

		DefaultDomainConfigurationObserver.Action action = obs.process(previous, current);

		assertEquals(Action.NOTHING, action);
	}

	@Test
	public void testUpdate() {
		SystemConf previous = SystemConf.create(new HashMap<String, String>());

		Map<String, String> val = new HashMap<String, String>();
		val.put("default-domain", "bm.lan");
		SystemConf current = SystemConf.create(val);

		DefaultDomainConfigurationObserver.Action action = obs.process(previous, current);

		assertEquals(Action.UPDATE, action);
	}

	@Test
	public void testRemove() {
		Map<String, String> previousVal = new HashMap<String, String>();
		previousVal.put("default-domain", "bm.lan");
		SystemConf previous = SystemConf.create(previousVal);

		Map<String, String> currentVal = new HashMap<String, String>();
		currentVal.put("default-domain", "");
		SystemConf current = SystemConf.create(currentVal);

		DefaultDomainConfigurationObserver.Action action = obs.process(previous, current);

		assertEquals(Action.REMOVE, action);
	}

	@Test
	public void testRemoveNullCurrentVal() {
		Map<String, String> previousVal = new HashMap<String, String>();
		previousVal.put("default-domain", "bm.lan");
		SystemConf previous = SystemConf.create(previousVal);

		Map<String, String> currentVal = new HashMap<String, String>();
		SystemConf current = SystemConf.create(currentVal);

		DefaultDomainConfigurationObserver.Action action = obs.process(previous, current);

		assertEquals(Action.REMOVE, action);
	}
}
