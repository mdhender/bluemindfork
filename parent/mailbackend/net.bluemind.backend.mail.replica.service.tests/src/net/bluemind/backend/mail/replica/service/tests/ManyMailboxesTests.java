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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.replication.client.UnparsedResponse;
import net.bluemind.backend.cyrus.replication.protocol.parsing.ParenObjectParser;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesMgmt;
import net.bluemind.backend.mail.replica.api.ResolvedMailbox;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.IMAPException;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ManyMailboxesTests extends AbstractRollingReplicationTests {

	private List<String> mailboxes;

	/**
	 * each user produces 6 folders
	 */
	public static final int TOTAL = 6;
	public static final int SHARED_EVERY_N = 2;

	@Before
	@Override
	public void before() throws Exception {
		super.before();
		int CNT = TOTAL;
		this.mailboxes = new ArrayList<>(10 * CNT);
		for (int i = 1; i <= CNT; i++) {
			String uid = null;
			if (i % SHARED_EVERY_N == 0) {
				uid = "shared.junit" + Strings.padStart(Integer.toString(i), 5, '0');
				ServerSideServiceProvider apis = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
				IMailshare sharesApi = apis.instance(IMailshare.class, domainUid);
				Mailshare ms = new Mailshare();
				ms.routing = Routing.internal;
				ms.name = uid;
				sharesApi.create(ms.name, ms);
				String inName = ms.name.replace('.', '^');
				mailboxes.add(domainUid + "!" + inName);
				mailboxes.add(domainUid + "!" + inName + ".Sent");
			} else {
				uid = "junit" + Strings.padStart(Integer.toString(i), 5, '0');

				PopulateHelper.addUser(uid, domainUid, Routing.internal);

				mailboxes.add(domainUid + "!user." + uid);
				mailboxes.add(domainUid + "!user." + uid + ".Sent");
				mailboxes.add(domainUid + "!user." + uid + ".Trash");
				mailboxes.add(domainUid + "!user." + uid + ".Drafts");
				mailboxes.add(domainUid + "!user." + uid + ".Outbox");
				mailboxes.add(domainUid + "!user." + uid + ".Junk");
			}
			Thread.sleep(20);
			System.err.println("After " + uid);
		}
		System.err.println("Registered " + mailboxes.size() + " mailboxes.");
		System.err.println("Connecting in 2sec...");
		Thread.sleep(2000);
		VertxPlatform.eventBus().request("sc.connect", "hello").toCompletionStage().toCompletableFuture().get(10,
				TimeUnit.SECONDS);
	}

	private static CompletableFuture<UnparsedResponse> mailboxes(String... mboxes) {
		CompletableFuture<UnparsedResponse> ret = new CompletableFuture<>();
		if (mboxes.length == 0) {
			ret.completeExceptionally(new ArrayIndexOutOfBoundsException("the mailboxes array must not be empty"));
			return ret;
		}
		JsonArray js = new JsonArray();
		Arrays.stream(mboxes).forEach(js::add);
		VertxPlatform.eventBus().request("sc.mailboxes", new JsonObject().put("mboxes", js),
				new DeliveryOptions().setSendTimeout(10000), (AsyncResult<Message<JsonArray>> result) -> {
					if (result.succeeded()) {
						JsonArray dataLines = result.result().body();
						List<String> asList = new ArrayList<>(dataLines.size());
						dataLines.forEach(obj -> asList.add(obj.toString()));
						ret.complete(new UnparsedResponse("OK", asList));
					} else {
						ret.completeExceptionally(result.cause());
					}
				});
		return ret;
	}

	private static CompletableFuture<UnparsedResponse> fullMailbox(String mbox) {
		CompletableFuture<UnparsedResponse> ret = new CompletableFuture<>();
		VertxPlatform.eventBus().request("sc.fullMailbox", mbox, new DeliveryOptions().setSendTimeout(10000),
				(AsyncResult<Message<JsonArray>> result) -> {
					if (result.succeeded()) {
						JsonArray dataLines = result.result().body();
						List<String> asList = new ArrayList<>(dataLines.size());
						dataLines.forEach(obj -> asList.add(obj.toString()));
						ret.complete(new UnparsedResponse("OK", asList));
					} else {
						ret.completeExceptionally(result.cause());
					}
				});
		return ret;
	}

	@Override
	public void after() throws Exception {
		VertxPlatform.eventBus().request("sc.disconnect", "bye").toCompletionStage().toCompletableFuture().get(10,
				TimeUnit.SECONDS);
		super.after();
	}

	@Test
	public void testGetDottedMailshare()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		String[] mboxes = mailboxes.stream().filter(v -> v.contains("shared")).toArray(String[]::new);
		System.err.println("Starting on slice with " + mboxes.length + " item(s)");
		long time = System.currentTimeMillis();
		UnparsedResponse response = mailboxes(mboxes).get(30, TimeUnit.SECONDS);
		assertNotNull(response);
		time = System.currentTimeMillis() - time;
		System.err.println("Ran in " + time + "ms. " + response.statusResponse);
		Set<String> expectedInResponse = new HashSet<>(Arrays.asList(mboxes));
		ParenObjectParser parser = ParenObjectParser.create();
		for (String l : response.dataLines) {
			System.err.println(l);
			int idx = l.indexOf("%(");
			assertTrue(idx > 0);

			JsonObject mailboxObj = parser.parse(l.substring(idx)).asObject();
			assertTrue(mailboxObj.containsKey("MBOXNAME"));
			String fetched = mailboxObj.getString("MBOXNAME");
			assertTrue(fetched + " is not an expected mailbox", expectedInResponse.contains(fetched));
		}
	}

	@Test
	public void testGetFullDottedMailshare()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		String mbox = mailboxes.stream().filter(v -> v.contains("shared")).findFirst().get();
		long time = System.currentTimeMillis();
		UnparsedResponse response = fullMailbox(mbox).get(30, TimeUnit.SECONDS);
		assertNotNull(response);
		time = System.currentTimeMillis() - time;
		System.err.println("Ran in " + time + "ms. " + response.statusResponse);
		for (String l : response.dataLines) {
			System.err.println(l);
		}
		assertEquals(1, response.dataLines.size());
	}

	@Test
	public void testResolveMany() {
		IReplicatedMailboxesMgmt api = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IReplicatedMailboxesMgmt.class);
		List<ResolvedMailbox> resolved = api.resolve(mailboxes);
		assertNotNull(resolved);
		System.err.println("Resolved " + mailboxes.size() + " returns " + resolved.size());
		assertEquals(mailboxes.size(), resolved.size());
	}

	@Test
	public void testGetManyMailboxes()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		int loops = 10;
		for (int i = 0; i < loops; i++) {
			System.err.println("**** " + (i + 1) + " / " + loops + " ****");
			for (List<String> slice : Lists.partition(mailboxes, 100)) {
				String[] mboxes = slice.toArray(new String[0]);
				System.err.println("Starting on slice with " + mboxes.length + " item(s)");
				long time = System.currentTimeMillis();
				UnparsedResponse response = mailboxes(mboxes).get(30, TimeUnit.SECONDS);
				assertNotNull(response);
				time = System.currentTimeMillis() - time;
				System.err.println((i + 1) + "/ " + loops + ": Response for " + slice.size() + " in " + time + "ms.");
			}
		}
	}

}
