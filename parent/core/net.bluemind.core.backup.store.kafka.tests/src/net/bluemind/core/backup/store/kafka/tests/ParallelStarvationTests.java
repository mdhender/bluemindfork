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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.store.kafka.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy.ExpectedBehaviour;
import net.bluemind.core.backup.store.kafka.ParallelStarvationHandler;

public class ParallelStarvationTests {

	@Test
	public void testTopicFinished() throws InterruptedException {
		CompletableFuture<Void> cdl = new CompletableFuture<>();
		IRecordStarvationStrategy delegate = infos -> {
			System.err.println("delegate called");
			cdl.complete(null);
			return ExpectedBehaviour.ABORT;
		};
		ParallelStarvationHandler psh = new ParallelStarvationHandler(delegate, 2, Map.of(0, 1L, 1, 1L));
		assertFalse("1 offset per worker, 2 offsets in total, should not be finished", psh.isTopicFinished());
		psh.updateOffsets(Map.of(0, 1L, 1, 0L));
		assertFalse("only one offset, should not be finished", psh.isTopicFinished());
		psh.updateOffsets(Map.of(0, 1L, 1, 1L));
		assertTrue("two offsets, should be finished", psh.isTopicFinished());
		assertFalse(cdl.isDone());
		psh.onStarvation(new JsonObject().put("records", 0L));
		assertTrue(cdl.isDone());
	}
}
