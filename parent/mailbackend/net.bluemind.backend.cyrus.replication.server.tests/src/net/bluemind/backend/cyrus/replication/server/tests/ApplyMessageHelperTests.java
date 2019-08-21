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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.Test;

import com.google.common.io.CharStreams;

import net.bluemind.backend.cyrus.replication.server.utils.ApplyMessageHelper;
import net.bluemind.backend.cyrus.replication.server.utils.ApplyMessageHelper.MessagesBatch;

public class ApplyMessageHelperTests {

	@Test
	public void testParseRequest() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		InputStream in = ParenObjectParserTests.class.getClassLoader()
				.getResourceAsStream("data/parent_objects/apply_message.txt");
		String fat = CharStreams.toString(new InputStreamReader(in, StandardCharsets.US_ASCII));
		System.out.println("Object len is " + fat.length());
		assertTrue(fat.startsWith("APPLY MESSAGE ("));
		String justTokens = fat.substring("APPLY MESSAGE (".length());
		assertEquals('%', justTokens.charAt(0));

		Stream<MessagesBatch> theStream = ApplyMessageHelper.process(justTokens);
		CompletableFuture<Void> root = CompletableFuture.completedFuture(null);
		final AtomicReference<CompletableFuture<Void>> rootRef = new AtomicReference<CompletableFuture<Void>>(root);
		final AtomicInteger current = new AtomicInteger(0);
		theStream.forEach(msg -> rootRef.set(rootRef.get().thenCompose(v -> {
			System.out.println(current.incrementAndGet() + ":  MSG " + msg);
			return CompletableFuture.completedFuture(null);
		})));
		CompletableFuture<String> endOfStream = rootRef.get().thenApply(v -> {
			return "yeah";
		});
		String result = endOfStream.get(10, TimeUnit.SECONDS);
		assertNotNull(result);
	}

}
