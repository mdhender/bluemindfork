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
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

import io.netty.util.concurrent.DefaultThreadFactory;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.cli.inject.common.TargetMailbox.Auth;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxQuota;

public class MailExchangeInjector {

	public static record DirEntryFilter(Set<String> filteredEmails, Set<Kind> entryKinds) {

		public Predicate<DirEntry> predicate() {
			return entry -> filteredEmails.isEmpty() || filteredEmails.contains(entry.email);
		}

		public Kind[] kinds() {
			return entryKinds.toArray(l -> new Kind[l]);
		}
	}

	protected static final Logger logger = LoggerFactory.getLogger(MailExchangeInjector.class);
	private final ArrayList<TargetMailbox> userEmails;
	private final String domain;
	private IMessageProducer producer;

	public MailExchangeInjector(IServiceProvider provider, String domainUid, TargetMailboxFactory tmf,
			IMessageProducer producer) {
		this(provider, domainUid, tmf, producer, new DirEntryFilter(Collections.emptySet(), Set.of(Kind.USER)));
	}

	public MailExchangeInjector(IServiceProvider provider, String domainUid, TargetMailboxFactory tmf,
			IMessageProducer producer, DirEntryFilter dirEntryFilter) {
		IDirectory dirApi = provider.instance(IDirectory.class, domainUid);
		ListResult<ItemValue<DirEntry>> users = dirApi.search(DirEntryQuery.filterKind(dirEntryFilter.kinds()));
		IMailboxes mboxApi = provider.instance(IMailboxes.class, domainUid);
		this.producer = producer;

		List<ItemValue<DirEntry>> chunk = new ArrayList<>(users.values);
		// because our cyrus max process is 200
		if (chunk.size() > 175) {
			Collections.shuffle(chunk, ThreadLocalRandom.current());
			chunk = chunk.subList(0, 175);
		}
		this.userEmails = chunk.stream().map(iv -> iv.value) //
				.filter(entry -> entry.email != null && !entry.archived) //
				.filter(entry -> {
					MailboxQuota quota = mboxApi.getMailboxQuota(entry.entryUid);
					return (quota.quota == null || quota.quota <= 0);
				}) //
				.filter(dirEntryFilter.predicate()) //
				.map(entry -> {
					ItemValue<Mailbox> box = mboxApi.getComplete(entry.entryUid);
					Optional<String> writerUid = (box.value.type == Mailbox.Type.mailshare)
							? mboxApi.getMailboxAccessControlList(entry.entryUid).stream() //
									.filter(ace -> ace.verb.equals(Verb.Write) || ace.verb.equals(Verb.All)) //
									.map(ace -> ace.subject).findFirst()
							: Optional.of(entry.entryUid);

					return writerUid.map(uid -> {
						ItemValue<Mailbox> mbox = mboxApi.getComplete(uid);
						LoginResponse lr = provider.instance(IAuthentication.class)
								.su(mbox.value.name + "@" + domainUid);
						return tmf.create(new TargetMailbox.Auth(lr.latd, lr.authKey, box.value));
					}).orElse(null);
				}).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));

		this.domain = domainUid;

		userEmails.stream().parallel().forEach(tm -> {
			boolean login = tm.prepare();
			logger.info("Logged-in {} => {}", tm.auth.email(), login);
		});

		logger.info("Created with {} target mailbox(es)", userEmails.size());
	}

	public void runCycle(RateLimiter rpm, int msg) {
		runCycle(rpm, msg, 4);
	}

	public void runCycle(RateLimiter rpm, int msg, int workers) {
		ExecutorService pool = Executors.newFixedThreadPool(workers, new DefaultThreadFactory("cli-inject"));
		CompletableFuture<?>[] proms = new CompletableFuture<?>[msg];
		AtomicLong cycle = new AtomicLong();
		AtomicInteger promSlot = new AtomicInteger();

		while (promSlot.get() < msg) {
			rpm.acquire();
			final long cycleLoc = cycle.incrementAndGet();
			proms[promSlot.getAndIncrement()] = CompletableFuture.supplyAsync(() -> {
				sendRandom(cycleLoc);
				return null;
			}, pool);
		}
		CompletableFuture<Void> globalProm = CompletableFuture.allOf(proms);
		logger.info("{} Waiting for completion of {} task(s)...", domain, proms.length);
		globalProm.join();
		end();
	}

	protected void end() {
		// override to implement post injection
	}

	private void sendRandom(long cycle) {
		Random rd = ThreadLocalRandom.current();
		TargetMailbox from = userEmails.get(rd.nextInt(userEmails.size()));
		TargetMailbox to = userEmails.get(rd.nextInt(userEmails.size()));
		byte[] emlContent = producer.createEml(from, to);
		to.exchange(from, emlContent, cycle);
	}

}
