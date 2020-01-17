/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.replication.server.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.lib.vertx.VertxPlatform;

public class TokenTests {

	@Test
	public void testToken() {
		FileSystem fs = VertxPlatform.getVertx().fileSystem();
		Token token = Token.of(Buffer.buffer("toto {12+}"), false, fs);
		assertNotNull(token);
		assertNotNull(token.followup());
		assertEquals(12, token.followup().size());

		Token noPlus = Token.of(Buffer.buffer("titi {14}"), false, fs);
		assertNotNull(noPlus);
		assertNotNull(noPlus.followup());
		assertEquals(14, noPlus.followup().size());

	}

	@Test
	public void testTokenMerge() {
		FileSystem fs = VertxPlatform.getVertx().fileSystem();
		Token text1 = Token.of(Buffer.buffer("apply "), false, fs);
		Token text2 = Token.of(Buffer.buffer("mailbox"), false, fs);
		LinkedList<Token> tokenList = new LinkedList<>(Arrays.asList(text1, text2));
		ReplicationFrame frame = new ReplicationFrame(0, tokenList, CompletableFuture.completedFuture(null));
		assertEquals(1, frame.size());
		System.out.println("frame: " + frame);
		Token merged = frame.next();
		assertEquals(merged.value(), "apply mailbox");

		Token t1 = Token.of(Buffer.buffer("apply "), false, fs);
		Token t2 = Token.of(Buffer.buffer("message %{coucou 3}"), false, fs);
		Token b1 = Token.of(Buffer.buffer("xxx"), true, fs);
		Token t3 = Token.of(Buffer.buffer(" after"), false, fs);
		Token t4 = Token.of(Buffer.buffer(" bin"), false, fs);
		LinkedList<Token> tokenList2 = new LinkedList<>(Arrays.asList(t1, t2, b1, t3, t4));
		ReplicationFrame complexFrame = new ReplicationFrame(1, tokenList2, CompletableFuture.completedFuture(null));
		assertEquals(1, complexFrame.size());
		System.out.println("frame: " + complexFrame);
		Token firstOne = complexFrame.next();
		assertNull(firstOne.followup());
	}

}
