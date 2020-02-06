/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.mail.replica.service.tests;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.replication.client.SyncClient;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class LocalSyncClientVerticle extends AbstractVerticle {

	public static class ScReg implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new LocalSyncClientVerticle();
		}

	}

	private SyncClient client;

	@Override
	public void start() {
		this.client = new SyncClient(vertx, "127.0.0.1", 2501);

		vertx.eventBus().consumer("sc.connect", msg -> {
			System.err.println(Thread.currentThread().getName() + " connect");
			client.connect().whenComplete((v, ex) -> {
				if (ex != null) {
					ex.printStackTrace();
				}
				msg.reply(ex == null);
			});
		});
		vertx.eventBus().consumer("sc.disconnect", msg -> {
			System.err.println(Thread.currentThread().getName() + " disconnect");
			client.disconnect().whenComplete((v, ex) -> {
				if (ex != null) {
					ex.printStackTrace();
				}
				msg.reply(ex == null);
			});
		});
		vertx.eventBus().consumer("sc.mailboxes", (Message<JsonObject> msg) -> {
			System.err.println(Thread.currentThread().getName() + " getMailboxes " + msg.body());
			JsonArray js = msg.body().getJsonArray("mboxes");
			if (js.size() == 0) {
				msg.reply(false);
				System.err.println("Call without mailbox");
				return;
			}
			String[] mb = new String[js.size()];
			for (int i = 0; i < js.size(); i++) {
				mb[i] = js.getString(i);
			}
			client.getMailboxes(mb).whenComplete((ur, ex) -> {
				if (ex != null) {
					ex.printStackTrace();
				}
				JsonArray lines = new JsonArray();
				for (String l : ur.dataLines) {
					lines.add(l);
				}
				msg.reply(lines);
			});
		});

		vertx.eventBus().consumer("sc.fullMailbox", (Message<String> msg) -> {
			System.err.println(Thread.currentThread().getName() + " fullMailbox " + msg.body());
			client.getFullMailbox(msg.body()).whenComplete((ur, ex) -> {
				if (ex != null) {
					ex.printStackTrace();
				}
				JsonArray lines = new JsonArray();
				for (String l : ur.dataLines) {
					lines.add(l);
				}
				msg.reply(lines);
			});
		});

		System.err.println("LocalSyncClientVerticle deployed !");
	}
}