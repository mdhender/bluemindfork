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

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.backend.cyrus.replication.client.SyncClient;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class LocalSyncClientVerticle extends Verticle {

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

		vertx.eventBus().registerLocalHandler("sc.connect", msg -> {
			System.err.println(Thread.currentThread().getName() + " connect");
			client.connect().whenComplete((v, ex) -> {
				if (ex != null) {
					ex.printStackTrace();
				}
				msg.reply(ex == null);
			});
		});
		vertx.eventBus().registerLocalHandler("sc.disconnect", msg -> {
			System.err.println(Thread.currentThread().getName() + " disconnect");
			client.disconnect().whenComplete((v, ex) -> {
				if (ex != null) {
					ex.printStackTrace();
				}
				msg.reply(ex == null);
			});
		});
		vertx.eventBus().registerLocalHandler("sc.mailboxes", (Message<JsonObject> msg) -> {
			System.err.println(Thread.currentThread().getName() + " getMailboxes " + msg.body());
			JsonArray js = msg.body().getArray("mboxes");
			if (js.size() == 0) {
				msg.reply(false);
				System.err.println("Call without mailbox");
				return;
			}
			String[] mb = new String[js.size()];
			for (int i = 0; i < js.size(); i++) {
				mb[i] = js.get(i);
			}
			client.getMailboxes(mb).whenComplete((ur, ex) -> {
				if (ex != null) {
					ex.printStackTrace();
				}
				JsonArray lines = new JsonArray();
				for (String l : ur.dataLines) {
					lines.addString(l);
				}
				msg.reply(lines);
			});
		});
		System.err.println("LocalSyncClientVerticle deployed !");
	}
}