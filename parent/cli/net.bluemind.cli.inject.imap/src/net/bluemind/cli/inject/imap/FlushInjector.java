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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.inject.imap;

import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.vertx.core.buffer.Buffer;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.IOutbox;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.inject.common.IMessageProducer;
import net.bluemind.cli.inject.common.MailExchangeInjector;
import net.bluemind.cli.inject.common.TargetMailbox;
import net.bluemind.cli.inject.common.TargetMailbox.Auth;
import net.bluemind.cli.inject.common.TargetMailboxFactory;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.network.topology.Topology;

public class FlushInjector extends MailExchangeInjector {

	public static class FlushTargetMailbox extends TargetMailbox {
		Semaphore lock;
		private String domainUid;
		private CliContext ctx;

		public FlushTargetMailbox(String domainUid, TargetMailbox.Auth auth, CliContext ctx) {
			super(auth);
			this.domainUid = domainUid;
			this.ctx = ctx;
			this.lock = new Semaphore(1);
		}

		public boolean prepare() {
			try {
				return true;
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		}

		public void exchange(TargetMailbox from, byte[] emlContent, long cycle) {
			try {
				lock.acquire();
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				return;
			}
			try {
				String url = "http://" + Topology.get().any("bm/core").value.address() + ":8090";
				ClientSideServiceProvider prov = ClientSideServiceProvider.getProvider(url, from.auth.sid());
				IAuthentication authApi = prov.instance(IAuthentication.class);
				AuthUser me = authApi.getCurrentUser();
				String subtree = IMailReplicaUids.subtreeUid(domainUid, Type.user, me.uid);
				IMailboxFoldersByContainer foldersApi = prov.instance(IMailboxFoldersByContainer.class, subtree);
				ItemValue<MailboxFolder> outbox = foldersApi.byName("Outbox");

				IMailboxItems recApi = prov.instance(IMailboxItems.class, outbox.uid);
				String part = recApi.uploadPart(VertxStream.stream(Buffer.buffer(emlContent)));
				MailboxItem mi = new MailboxItem();
				mi.flags = Collections.emptyList();
				MessageBody body = new MessageBody();
				body.structure = Part.create(null, "message/rfc822", part);
				mi.body = body;

				recApi.create(mi);
				recApi.removePart(part);
				IOutbox outboxApi = prov.instance(IOutbox.class, domainUid, me.uid);
				TaskRef tsk = outboxApi.flush();
				TaskStatus status = Tasks.followStream(ctx, "flush", tsk, true).orTimeout(30, TimeUnit.SECONDS).join();
				ctx.info("Status is {}", status.state);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				lock.release();
			}
		}
	}

	public FlushInjector(CliContext ctx, String domainUid, IMessageProducer prod) {
		super(ctx.adminApi(), domainUid, factory(domainUid, ctx), prod);

	}

	private static TargetMailboxFactory factory(String domainUid, CliContext ctx) {
		return (Auth auth) -> new FlushTargetMailbox(domainUid, auth, ctx);
	}

}
