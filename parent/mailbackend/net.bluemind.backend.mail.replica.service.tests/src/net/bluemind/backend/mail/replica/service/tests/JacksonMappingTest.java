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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.utils.JsonUtils;

public class JacksonMappingTest {

	@Test
	public void testJackson() {
		String js = "{\"body\":null,\"imapUid\":1,\"flags\":[],\"internalFlags\":[],\"messageBody\":\"6c72c9519f7d6944ef7520e1e62e83c67128fea3\",\"modSeq\":3,\"internalDate\":1585042711000,\"lastUpdated\":1585042711000}";
		MailboxRecord parsed = JsonUtils.read(js, MailboxRecord.class);
		assertNotNull(parsed);
	}

}
