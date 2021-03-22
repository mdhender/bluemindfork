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
package net.bluemind.lmtp.proxy.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lmtp.testhelper.client.Request;
import net.bluemind.lmtp.testhelper.client.VertxLmtpClient;
import net.bluemind.lmtp.testhelper.model.FakeMailbox;
import net.bluemind.lmtp.testhelper.model.FakeMailbox.State;
import net.bluemind.lmtp.testhelper.model.MailboxesModel;

public class BmLmtpProxyTests extends AbstractChainTest {

	@Before
	public void before() throws Exception {
		super.before();
		MailboxesModel.get().addMailbox(new FakeMailbox("dataerr@bm.lan", State.AfterDataError));
	}

	@Test
	public void testTwoOverQuotaAndOnIOError()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.batch(//
					Request.lhlo("bm.lan"), //
					Request.mailFrom("sender@bm.lan"), //
					Request.rcptTo("overq@bm.lan"), //
					Request.rcptTo("overq2@bm.lan"), //
					Request.rcptTo("fucked@bm.lan")).thenCompose(resps -> {
						assertEquals(5, resps.size());
						checkCode(resps.get(0), 250);
						checkCode(resps.get(1), 250);
						checkCode(resps.get(2), 452);
						checkCode(resps.get(3), 452);
						checkCode(resps.get(4), 451);
						return CompletableFuture.completedFuture(null);
					});
		});
	}

	@Test
	public void testTwoOverQuotaAndOnIOErrorInOneChunk()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.batchOneChunk(//
					Request.lhlo("bm.lan"), //
					Request.mailFrom("sender@bm.lan"), //
					Request.rcptTo("overq@bm.lan"), //
					Request.rcptTo("overq2@bm.lan"), //
					Request.rcptTo("fucked@bm.lan")).thenCompose(resps -> {
						assertEquals(5, resps.size());
						checkCode(resps.get(0), 250);
						checkCode(resps.get(1), 250);
						checkCode(resps.get(2), 452);
						checkCode(resps.get(3), 452);
						checkCode(resps.get(4), 451);
						return CompletableFuture.completedFuture(null);
					});
		});
	}

	@Test
	public void testTwoValidOutOfThree()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("recip@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.rcptTo("dataerr@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.rcptTo("recip2@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.data(3, Buffer.buffer("From: tcataldo@gmail.com\r\n"));
			}).thenCompose(dataResp -> {
				assertEquals(3, dataResp.length);
				return checkCode(dataResp, 250, 451);
			});
		});
	}

	@Test
	public void testMetricsAreWorking()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("recip@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.data(1, resourceBuffer("data/basic_2attachments.eml"));
			}).thenCompose(dataResp -> {
				return checkCode(dataResp, 250);
			}).thenCompose(checkCode -> {
				CompletableFuture<Void> comp = new CompletableFuture<>();
				long len = testRegistry.counters().count();
				if (len == 0) {
					comp.completeExceptionally(new Exception("We should have counters."));
				} else if (testRegistry.distributionSummaries().count() < 2) {
					comp.completeExceptionally(new Exception("We should have 2 distributionSummaries."));
				} else {
					comp.complete(null);
				}
				// Polled gauges updates are done by spectator, to force it, use the line under
				// PolledMeter.update(testRegistry);
				if (testRegistry.gauges() == null) {
					System.out.println("No gauges");
				} else {
					testRegistry.gauges().forEach(gauge -> {
						System.err.println(gauge);
						System.err.println(gauge.id().toString());
						System.err.println(gauge.value());
					});
				}
				return comp;
			});
		});
	}

	protected CompletableFuture<VertxLmtpClient> lmtpClient() {
		CompletableFuture<VertxLmtpClient> ret = new CompletableFuture<VertxLmtpClient>();
		Vertx vertx = VertxPlatform.getVertx();
		vertx.setTimer(1, tid -> {
			VertxLmtpClient client = new VertxLmtpClient(vertx, "127.0.0.1", 2400);
			ret.complete(client);
		});
		return ret;
	}
}
