/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.cyrus.replication.server.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.google.common.io.CharStreams;

import net.bluemind.backend.cyrus.replication.protocol.parsing.JsonElement;
import net.bluemind.backend.cyrus.replication.protocol.parsing.ZeroCopyParenObjectParser;

public class ApplyMailboxTests {

	int PERF_LOOPS = 4;

	@Test
	public void testZeroCopyVeryBigMailboxObject() throws IOException {
		InputStream in = ParenObjectParserTests.class.getClassLoader()
				.getResourceAsStream("data/parent_objects/big_apply_mailbox.txt");
		String fat = CharStreams.toString(new InputStreamReader(in, StandardCharsets.US_ASCII));
		System.out.println("Object len is " + fat.length());
		ZeroCopyParenObjectParser pop = new ZeroCopyParenObjectParser();
		JsonElement parsed = pop.parse(fat);
		assertNotNull(parsed);
		assertTrue(parsed.isObject());
		for (int i = 0; i < PERF_LOOPS; i++) {
			parsed = pop.parse(fat);
			assertNotNull(parsed);
			System.out.println("run " + i);
		}
	}

}
