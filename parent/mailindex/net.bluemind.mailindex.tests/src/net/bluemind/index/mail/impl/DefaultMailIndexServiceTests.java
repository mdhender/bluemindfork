/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.index.mail.impl;

import org.junit.Before;

import net.bluemind.lib.elasticsearch.ElasticsearchClientConfig;

public class DefaultMailIndexServiceTests extends MailIndexServiceTests {

	@Override
	@Before
	public void before() throws Exception {
		String ACTIVATE_ALIAS_RING_MODE = "elasticsearch.indexation.alias_mode.ring";
		String ALIAS_RING_MODE_ALIAS_COUNT_MULTIPLIER = "elasticsearch.indexation.alias_mode.mode_ring.alias_count_multiplier";
		System.setProperty(ACTIVATE_ALIAS_RING_MODE, "false");
		System.setProperty(ALIAS_RING_MODE_ALIAS_COUNT_MULTIPLIER, "0");
		ElasticsearchClientConfig.reload();
		super.before();
	}

}
