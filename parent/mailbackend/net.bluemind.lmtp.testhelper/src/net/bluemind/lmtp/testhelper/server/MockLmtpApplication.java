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
package net.bluemind.lmtp.testhelper.server;

import java.util.concurrent.CompletableFuture;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lmtp.testhelper.model.FakeMailbox;
import net.bluemind.lmtp.testhelper.model.FakeMailbox.State;
import net.bluemind.lmtp.testhelper.model.MailboxesModel;

public class MockLmtpApplication implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(MockLmtpApplication.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		MailboxesModel.get()//
				.addValidSender("sender@bm.lan")//
				.addMailbox(new FakeMailbox("full@bm.lan", State.OverQuotaOnNextMail)) //
				.addMailbox(new FakeMailbox("overq@bm.lan", State.OverQuota)) //
				.addMailbox(new FakeMailbox("fucked@bm.lan", State.Fucked)) //
				.addMailbox(new FakeMailbox("recip@bm.lan", State.Ok)) //
				.addMailbox(new FakeMailbox("recip2@bm.lan", State.Ok));

		MockServer.start();
		ProxyServer.start();
		logger.info("Started.");
		CompletableFuture<Void> block = new CompletableFuture<>();
		block.thenAccept(v -> {
			context.setResult(EXIT_OK, this);
		}).join();
		return IApplicationContext.EXIT_ASYNC_RESULT;
	}

	@Override
	public void stop() {
		logger.info("Stopped.");
	}

}
