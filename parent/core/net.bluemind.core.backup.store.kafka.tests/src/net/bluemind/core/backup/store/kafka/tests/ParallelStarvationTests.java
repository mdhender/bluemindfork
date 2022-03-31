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

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy.ExpectedBehaviour;
import net.bluemind.core.backup.store.kafka.ParallelStarvationHandler;

public class ParallelStarvationTests {

	@Test
	public void testStarvedOneThenTheOther() throws InterruptedException {
		CompletableFuture<Void> cdl = new CompletableFuture<>();
		IRecordStarvationStrategy delegate = infos -> {
			System.err.println("delegate called");
			cdl.complete(null);
			return ExpectedBehaviour.ABORT;
		};
		ParallelStarvationHandler psh = new ParallelStarvationHandler(delegate, 2);
		IRecordStarvationStrategy worker0 = psh;
		IRecordStarvationStrategy worker1 = psh;

		// only worker2 starves...
		worker1.onStarvation(new JsonObject().put("records", 42L));
		System.err.println("sleep 500");
		Thread.sleep(500);
		worker1.onStarvation(new JsonObject().put("records", 42L));
		System.err.println("sleep 1000");
		Thread.sleep(1000);
		assertFalse("only worker2 is starved, worker1 is unknown", cdl.isDone());

		worker0.onStarvation(new JsonObject().put("records", 0L));
		Thread.sleep(1000);
		worker0.onStarvation(new JsonObject().put("records", 0L));
		assertTrue("worker1 starved too, but we did not complete", cdl.isDone());
	}

	@Test
	public void testStarveBothThenReceive() throws InterruptedException {
		CompletableFuture<Void> cdl = new CompletableFuture<>();
		IRecordStarvationStrategy delegate = infos -> {
			System.err.println("delegate called");
			cdl.complete(null);
			return ExpectedBehaviour.ABORT;
		};
		ParallelStarvationHandler psh = new ParallelStarvationHandler(delegate, 2);
		IRecordStarvationStrategy worker0 = psh;
		IRecordStarvationStrategy worker1 = psh;

		// only worker2 starves...
		worker0.onStarvation(new JsonObject().put("records", 42L));
		worker1.onStarvation(new JsonObject().put("records", 42L));
		Thread.sleep(500);
		worker1.onRecordsReceived(new JsonObject());
		Thread.sleep(1000);
		assertFalse("worker1 received data last", cdl.isDone());
	}

}
