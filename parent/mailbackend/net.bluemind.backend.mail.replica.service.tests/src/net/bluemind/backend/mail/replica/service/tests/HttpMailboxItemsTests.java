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
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.sessions.Sessions;

public class HttpMailboxItemsTests extends MailboxItemsTests {

	@Before
	@Override
	public void before() throws Exception {
		super.before();

	}

	@Override
	public IServiceProvider provider() {
		SecurityContext userSec = new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(),
				domainUid);
		Sessions.get().put(userSec.getSessionId(), userSec);
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", userSec.getSessionId());
	}

	@Test
	public void testFetchComplete() {
		ContainerChangeset<Long> allContent = mailApi.changesetById(0L);
		List<ItemValue<MailboxItem>> fullList = mailApi.multipleGetById(allContent.created);
		System.err.println("got " + fullList.size());
		assertTrue(!fullList.isEmpty());
		ExecutorService pool = Executors.newFixedThreadPool(4);
		List<CompletableFuture<String>> proms = new ArrayList<>(fullList.size());
		AtomicInteger fetches = new AtomicInteger();
		@SuppressWarnings("deprecation")
		HashFunction sha1Hash = Hashing.sha1();
		while (fetches.get() < 10_000) {
			for (ItemValue<MailboxItem> imi : fullList) {
				CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
					Stream fetched = mailApi.fetchComplete(imi.value.imapUid);
					byte[] result = GenericStream.streamToBytes(fetched);
					String sha1 = sha1Hash.hashBytes(result).toString();
					assertEquals(imi.value.body.guid, sha1);
					fetches.incrementAndGet();
					return sha1;
				}, pool);
				proms.add(future);
			}
			CompletableFuture<Void> all = CompletableFuture.allOf(proms.toArray(CompletableFuture[]::new));
			all.orTimeout(30, TimeUnit.SECONDS).join();
			System.err.println("fetches " + fetches.get());
		}
		pool.shutdown();
	}

	@Override
	protected InputStream testEml() {
		return EmlTemplates.withRandomMessageId("with_inlines.ftl");
	}

}
