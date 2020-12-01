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
package net.bluemind.cli.inject.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxQuota;

public class MailExchangeInjector {

	protected static final Logger logger = LoggerFactory.getLogger(MailExchangeInjector.class);
	private final ArrayList<TargetMailbox> userEmails;
	private final String domain;
	private IMessageProducer producer;

	public MailExchangeInjector(IServiceProvider provider, String domainUid, TargetMailboxFactory tmf,
			IMessageProducer producer) {
		IDirectory dirApi = provider.instance(IDirectory.class, domainUid);
		ListResult<ItemValue<DirEntry>> users = dirApi.search(DirEntryQuery.filterKind(Kind.USER));
		IMailboxes mboxApi = provider.instance(IMailboxes.class, domainUid);
		this.producer = producer;

		List<ItemValue<DirEntry>> chunk = new ArrayList<>(users.values);
		// because our cyrus max process is 200
		if (chunk.size() > 175) {
			Collections.shuffle(chunk, ThreadLocalRandom.current());
			chunk = chunk.subList(0, 175);
		}
		this.userEmails = chunk.stream().map(iv -> {
			String em = iv.value.email;
			if (em == null) {
				return null;
			}
			if (iv.value.archived) {
				return null;
			}
			MailboxQuota quota = mboxApi.getMailboxQuota(iv.value.entryUid);
			if (quota.quota != null && quota.quota > 0) {
				logger.info("Skip user {} with quota", iv.value.entryUid);
				return null;
			}
			ItemValue<Mailbox> mbox = mboxApi.getComplete(iv.value.entryUid);
			LoginResponse lr = provider.instance(IAuthentication.class).su(mbox.value.name + "@" + domainUid);
			return tmf.create(lr.latd, lr.authKey);
		}).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));

		this.domain = domainUid;
		for (TargetMailbox tm : userEmails) {
			boolean login = tm.prepare();
			logger.info("Logged-in {} => {}", tm.email, login);
		}
		logger.info("Created with {} target mailbox(es)", userEmails.size());
	}

	public void runCycle(int msg) {
		runCycle(msg, 4);
	}

	public void runCycle(int msg, int workers) {
		ExecutorService pool = Executors.newFixedThreadPool(workers);
		CompletableFuture<?>[] proms = new CompletableFuture<?>[msg];

		for (int i = 0; i < msg; i++) {
			proms[i] = CompletableFuture.supplyAsync(this::oneMsg, pool);
		}
		CompletableFuture<Void> globalProm = CompletableFuture.allOf(proms);
		logger.info("{} Waiting for completion of {} task(s)...", domain, proms.length);
		globalProm.join();
		end();
	}

	protected void end() {
		// override to implement post injection
	}

	private Object oneMsg() {
		try {
			sendRandom();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	private void sendRandom() {
		Random rd = ThreadLocalRandom.current();
		TargetMailbox from = userEmails.get(rd.nextInt(userEmails.size()));
		TargetMailbox to = userEmails.get(rd.nextInt(userEmails.size()));
		byte[] emlContent = producer.createEml(from, to);
		to.exchange(from, emlContent);
	}

}
