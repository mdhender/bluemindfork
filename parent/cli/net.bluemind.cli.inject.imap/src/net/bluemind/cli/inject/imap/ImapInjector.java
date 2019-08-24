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

import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javafaker.Faker;
import com.github.javafaker.GameOfThrones;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.network.topology.Topology;

public class ImapInjector {

	private static final Logger logger = LoggerFactory.getLogger(ImapInjector.class);
	private final TargetMailbox[] userEmails;
	private final String domain;

	private static class TargetMailbox {
		StoreClient sc;
		String email;
		Semaphore lock;

		public TargetMailbox(String email, String sid) {
			this.email = email;
			this.sc = new StoreClient(Topology.get().any("mail/imap").value.address(), 1143, email, sid);
			this.lock = new Semaphore(1);
		}
	}

	public ImapInjector(IServiceProvider provider, String domainUid) {
		IDirectory dirApi = provider.instance(IDirectory.class, domainUid);
		ListResult<ItemValue<DirEntry>> users = dirApi.search(DirEntryQuery.filterKind(Kind.USER));
		IMailboxes mboxApi = provider.instance(IMailboxes.class, domainUid);
		this.userEmails = users.values.stream().map(iv -> {
			String em = iv.value.email;
			if (em == null) {
				return null;
			}
			MailboxQuota quota = mboxApi.getMailboxQuota(iv.value.entryUid);
			if (quota.quota != null && quota.quota > 0) {
				logger.info("Skip user {} with quota", iv.value.entryUid);
				return null;
			}
			LoginResponse lr = provider.instance(IAuthentication.class).su(em);
			return new TargetMailbox(em, lr.authKey);
		}).filter(Objects::nonNull).toArray(TargetMailbox[]::new);
		this.domain = domainUid;
		for (TargetMailbox tm : userEmails) {
			boolean login = tm.sc.login();
			logger.info("Logged-in {} => {}", tm.email, login);
		}
		logger.info("Created with {} target mailbox(es)", userEmails.length);
	}

	public void runCycle(int msg) {
		ExecutorService pool = Executors.newFixedThreadPool(4);
		ListeningExecutorService lPool = MoreExecutors.listeningDecorator(pool);
		Executor compPool = MoreExecutors.directExecutor();
		CompletableFuture<?>[] proms = new CompletableFuture<?>[msg];
		for (int i = 0; i < msg; i++) {
			CompletableFuture<Void> cur = new CompletableFuture<>();
			ListenableFuture<?> lFuture = lPool.submit(this::oneMsg);
			proms[i] = cur;
			lFuture.addListener(() -> cur.complete(null), compPool);
		}
		CompletableFuture<Void> globalProm = CompletableFuture.allOf(proms);
		logger.info("{} Waiting for completion of {} task(s)...", domain, proms.length);
		globalProm.join();

	}

	private void oneMsg() {
		try {
			sendRandom();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void sendRandom() throws Exception {
		Random rd = ThreadLocalRandom.current();
		TargetMailbox from = userEmails[rd.nextInt(userEmails.length)];
		TargetMailbox to = userEmails[rd.nextInt(userEmails.length)];
		byte[] emlContent = createEml(rd, from, to);
		to.lock.acquire();
		try {
			int added = to.sc.append("INBOX", new ByteArrayInputStream(emlContent), new FlagsList());
			logger.debug("Added {} to {}", added, to.email);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			System.exit(1);
		} finally {
			to.lock.release();
		}
	}

	private static final GameOfThrones gotFaker = Faker.instance().gameOfThrones();

	private byte[] createEml(Random rd, TargetMailbox from, TargetMailbox to) {
		StringBuilder sb = new StringBuilder();
		sb.append("From: ").append(from.email).append("\r\n");
		sb.append("To: ").append(to.email).append("\r\n");
		sb.append("Context-Type: text/html; charset=utf-8\r\n");
		sb.append("Subject: Rand Message ").append(UUID.randomUUID()).append("\r\n\r\n");
		sb.append("<html><body><p>Yeah this is   body   </p>\r\n");
		for (int i = 0; i < 1024; i++) {
			sb.append("<p>").append(gotFaker.quote()).append("</p>\r\n");
			sb.append("<div>Written by <em>").append(gotFaker.character()).append("</em> of ").append(gotFaker.house())
					.append("</div>");
			sb.append("<div>Delivered to <em>").append(gotFaker.city()).append("</em></div>");
		}
		sb.append("\r\n</body></html>\r\n");
		byte[] emlContent = sb.toString().getBytes();
		return emlContent;
	}

}
