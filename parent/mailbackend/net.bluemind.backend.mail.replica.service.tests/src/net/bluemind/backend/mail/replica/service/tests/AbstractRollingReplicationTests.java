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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.junit.Before;

import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.mime4j.common.OffloadedBodyFactory;

public abstract class AbstractRollingReplicationTests extends MailApiTestsBase {

	protected String cyrusIp;
	protected String domainUid;

	protected String apiKey;

	protected String partition;
	protected String mboxRoot;
	protected ReplicationEventsRecorder rec;
	protected IdRange allocations;

	protected String uniqueUidPart() {
		return System.currentTimeMillis() + "";
	}

	@Before
	@Override
	public void before() throws Exception {
		super.before();
		domainUid = domUid;
		cyrusIp = "127.0.0.1";
		AuthUser testUser = userProvider.instance(IAuthentication.class).getCurrentUser();
		partition = CyrusPartition.forServerAndDomain(testUser.value.dataLocation, domUid).name;
		mboxRoot = "user." + testUser.value.login.replace('.', '^');
		this.rec = new ReplicationEventsRecorder(VertxPlatform.getVertx());
		IOfflineMgmt offlineApi = userProvider.instance(IOfflineMgmt.class, domUid, userUid);
		this.allocations = offlineApi.allocateOfflineIds(50);
		this.apiKey = "sid";
	}

	@FunctionalInterface
	public static interface ImapActions<T> {

		T run(StoreClient sc) throws Exception;
	}

	protected <T> T imapAsCyrusAdmin(ImapActions<T> actions) {
		throw new ServerFault("imap-endpoint has not 'admin' connections.");
	}

	protected InputStream testEml() {
		return EmlTemplates.withRandomMessageId("with_inlines.ftl");
	}

	protected InputStream testEml(String file) {
		return EmlTemplates.withRandomMessageId(file);
	}

	protected ItemValue<MailboxItem> addDraft(ItemValue<MailboxFolder> inbox) throws IOException, InterruptedException {
		return addDraft(inbox, userUid);
	}

	protected ItemValue<MailboxItem> addDraft(ItemValue<MailboxFolder> inbox, String owner)
			throws IOException, InterruptedException {
		IOfflineMgmt idAllocator = provider().instance(IOfflineMgmt.class, domainUid, owner);
		IdRange oneId = idAllocator.allocateOfflineIds(1);
		return addDraft(inbox, oneId.globalCounter, owner);
	}

	protected ItemValue<MailboxItem> addDraft(ItemValue<MailboxFolder> inbox, long id)
			throws IOException, InterruptedException {
		return addDraft(inbox, id, userUid);
	}

	protected ItemValue<MailboxItem> addDraft(ItemValue<MailboxFolder> inbox, long id, String owner)
			throws IOException, InterruptedException {
		assertNotNull(inbox);
		IMailboxItems recordsApi = provider().instance(IMailboxItems.class, inbox.uid);
		try (InputStream in = testEml()) {
			Stream forUpload = VertxStream.stream(Buffer.buffer(ByteStreams.toByteArray(in)));
			String partId = recordsApi.uploadPart(forUpload);
			assertNotNull(partId);
			Part fullEml = Part.create(null, "message/rfc822", partId);
			MessageBody brandNew = new MessageBody();
			brandNew.subject = "toto";
			brandNew.structure = fullEml;
			brandNew.headers = Arrays
					.asList(Header.create(MailApiHeaders.X_BM_DRAFT_REFRESH_DATE, new Date().toInstant().toString()));
			MailboxItem item = new MailboxItem();
			item.body = brandNew;
			item.flags = Arrays.asList(new MailboxItemFlag("Pouic"));
			long expectedId = id;
			recordsApi.createById(expectedId, item);
			ItemValue<MailboxItem> reloaded = recordsApi.getCompleteById(expectedId);
			assertNotNull(reloaded);
			assertNotNull(reloaded.value.body.headers);
			Optional<Header> idHeader = reloaded.value.body.headers.stream()
					.filter(h -> h.name.equals(MailApiHeaders.X_BM_INTERNAL_ID)).findAny();
			assertTrue(idHeader.isPresent());
			assertEquals(owner + "#" + InstallationId.getIdentifier() + ":" + expectedId, idHeader.get().firstValue());
			recordsApi.removePart(partId);
			return reloaded;
		}
	}

	protected IServiceProvider provider() {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", apiKey);
	}

	protected CountDownLatch expectMessage(String vertxAddress) {
		return expectMessages(vertxAddress, 1, msg -> true);
	}

	protected CountDownLatch expectMessages(String vertxAddress, int count) {
		return expectMessages(vertxAddress, count, msg -> true);
	}

	protected CountDownLatch expectMessages(String vertxAddress, int count, Predicate<JsonObject> msgFilter) {
		CountDownLatch msgLock = new CountDownLatch(count);
		System.err.println("Consuming on " + vertxAddress);
		AtomicReference<Handler<Message<JsonObject>>> ref = new AtomicReference<>();
		MessageConsumer<JsonObject> cons = VertxPlatform.eventBus().consumer(vertxAddress);
		Handler<Message<JsonObject>> h = (Message<JsonObject> msg) -> {
			JsonObject payload = msg.body();
			boolean matches = msgFilter.test(payload);
			System.out.println("GOT 1 on " + vertxAddress + " (match: " + matches + ") (still expects "
					+ (msgLock.getCount() - (matches ? 1 : 0)) + "): " + payload.encodePrettily());
			if (matches) {
				msgLock.countDown();
				if (msgLock.getCount() == 0) {
					cons.unregister();
				}
			}
		};
		ref.set(h);
		cons.handler(h);
		return msgLock;
	}

	/**
	 * @param eml
	 * @return the size of the uploaded eml in bytes
	 * @throws IOException
	 */
	protected long addMailToFolder(InputStream eml, String folderUid) throws IOException {
		long time = System.currentTimeMillis();
		Buffer toUpload = null;
		ByteBufOutputStream out = new ByteBufOutputStream(Unpooled.buffer());// NOSONAR
		long uploaded = 0;

		try (org.apache.james.mime4j.dom.Message parsed = Mime4JHelper.parse(eml, new OffloadedBodyFactory())) {

			parsed.createMessageId(UUID.randomUUID().toString());
			Mime4JHelper.serialize(parsed, out);
			time = System.currentTimeMillis() - time;

			System.err.println("Fresh " + out.buffer().readableBytes() + " byte(s) mail generated in " + time + "ms.");
			toUpload = Buffer.buffer(out.buffer());
			uploaded = toUpload.length();
		} catch (Exception e) {
			throw new IOException(e);
		}
		Stream forUpload = VertxStream.stream(toUpload);
		long partUpload = System.currentTimeMillis();
		IMailboxItems recordsApi = provider().instance(IMailboxItems.class, folderUid);
		String partId = recordsApi.uploadPart(forUpload);
		assertNotNull(partId);
		partUpload = System.currentTimeMillis() - partUpload;
		System.err.println("Got partId " + partId + " in " + partUpload + "ms.");
		Part fullEml = Part.create(null, "message/rfc822", partId);
		MessageBody brandNew = new MessageBody();
		brandNew.structure = fullEml;
		brandNew.headers = Arrays
				.asList(Header.create(MailApiHeaders.X_BM_DRAFT_REFRESH_DATE, new Date().toInstant().toString()));
		MailboxItem item = new MailboxItem();
		item.body = brandNew;
		item.flags = Arrays.asList(new MailboxItemFlag("Pouic"));
		System.err.println("Before create WITH id.....");
		long createTime = System.currentTimeMillis();
		long nextId = allocations.globalCounter++;
		System.err.println("create mail with id " + nextId);
		recordsApi.createById(nextId, item);
		createTime = System.currentTimeMillis() - createTime;
		System.err.println("create WITH id took " + createTime + "ms.");
		ItemValue<MailboxItem> reloaded = recordsApi.getCompleteById(nextId);
		assertNotNull(reloaded);
		assertNotNull(reloaded.value.body.headers);
		Optional<Header> idHeader = reloaded.value.body.headers.stream()
				.filter(h -> h.name.equals(MailApiHeaders.X_BM_INTERNAL_ID)).findAny();
		assertTrue(idHeader.isPresent());
		recordsApi.removePart(partId);
		return uploaded;
	}

	protected void addMailToFolder(String folderUid) throws IOException {
		addMailToFolder(testEml(), folderUid);
	}

	protected void addMailToFolder(String folderUid, String file) throws IOException {
		addMailToFolder(testEml(file), folderUid);
	}

	/** Create a message in a synchronous way. */
	protected long createEml(String emlPath, String userUid, String mboxRoot, String folderName) throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(emlPath)) {
			IServiceProvider provider = ServerSideServiceProvider.getProvider(new SecurityContext(userUid, userUid,
					Collections.<String>emptyList(), Collections.<String>emptyList(), domainUid));
			Stream stream = VertxStream.stream(Buffer.buffer(ByteStreams.toByteArray(in)));
			IMailboxFolders mailboxFolderService = provider.instance(IMailboxFolders.class, partition, mboxRoot);
			ItemValue<MailboxFolder> folder = mailboxFolderService.byName(folderName);
			IMailboxItems mailboxItemService = provider.instance(IMailboxItems.class, folder.uid);
			String partId = mailboxItemService.uploadPart(stream);
			Part fullEml = Part.create(null, "message/rfc822", partId);
			MessageBody messageBody = new MessageBody();
			messageBody.subject = "Subject_" + System.currentTimeMillis();
			messageBody.structure = fullEml;
			MailboxItem item = new MailboxItem();
			item.body = messageBody;
			IOfflineMgmt offlineMgmt = provider.instance(IOfflineMgmt.class, domainUid, userUid);
			IdRange oneId = offlineMgmt.allocateOfflineIds(1);
			long expectedId = oneId.globalCounter;
			mailboxItemService.createById(expectedId, item);
			return expectedId;
		}
	}

	protected SortDescriptor createSortDescriptor(ItemFlagFilter flagFilter) {
		SortDescriptor sortDesc = new SortDescriptor();
		sortDesc.filter = flagFilter;
		return sortDesc;
	}

}
