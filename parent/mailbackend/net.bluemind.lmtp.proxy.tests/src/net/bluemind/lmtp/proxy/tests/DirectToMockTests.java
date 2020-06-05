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

import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lmtp.testhelper.client.VertxLmtpClient;
import net.bluemind.lmtp.testhelper.model.FakeMailbox;
import net.bluemind.lmtp.testhelper.model.FakeMailbox.State;
import net.bluemind.lmtp.testhelper.model.MailboxesModel;

public class DirectToMockTests extends BmLmtpProxyTests {

	protected CompletableFuture<VertxLmtpClient> lmtpClient() {
		CompletableFuture<VertxLmtpClient> ret = new CompletableFuture<VertxLmtpClient>();
		Vertx vertx = VertxPlatform.getVertx();
		vertx.setTimer(1, tid -> {
			VertxLmtpClient client = new VertxLmtpClient(vertx, "127.0.0.1", 2424);
			ret.complete(client);
		});
		return ret;
	}

	@Test
	public void newLineInDataResponse()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		MailboxesModel.get().addMailbox(new FakeMailbox("newlineindata@bm.lan", State.NewLineDataResponse));

		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("newlineindata@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.data(1, Buffer.buffer("From: tcataldo@gmail.com\r\n"));
			}).thenCompose(dataResp -> {
				assertEquals(1, dataResp.length);
				return checkCode(dataResp, 451);
			});
		});
	}

	@Test
	public void tooShortDataResponse()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		MailboxesModel.get().addMailbox(new FakeMailbox("tooshortdata@bm.lan", State.TooShortDataResponse));

		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("tooshortdata@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.data(1, Buffer.buffer("From: tcataldo@gmail.com\r\n"));
			}).thenCompose(dataResp -> {
				assertEquals(1, dataResp.length);
				return checkCode(dataResp, 451);
			});
		});
	}
}
