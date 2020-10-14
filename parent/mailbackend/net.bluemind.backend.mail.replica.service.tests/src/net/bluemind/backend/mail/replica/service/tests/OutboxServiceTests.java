package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.james.mime4j.dom.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.replication.testhelper.ExpectCommand;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.IOutbox;
import net.bluemind.backend.mail.api.IUserInbox;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.mime.MimeTree;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.mime4j.common.OffloadedBodyFactory;

public class OutboxServiceTests extends AbstractRollingReplicationTests {

	private String apiKey;
	private String partition;
	private String mboxRoot;
	private ClientSideServiceProvider provider;

	private IMailboxFolders mailboxFolderService;
	private String outboxUid;
	private String sentUid;
	private IMailboxItems sent_mailboxItemsService;
	private IOutbox outboxService;
	private IMailboxItems outboxMailboxItemsService;
	private IdRange allocations;

	private static final int ALLOC_COUNT = 42;

	@Before
	public void before() throws Exception {
		super.before();

		imapAsUser(sc -> {
			int added = sc.append("INBOX", testEml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("INBOX");
			Collection<MimeTree> bs = sc.uidFetchBodyStructure(Arrays.asList(added));
			MimeTree tree = bs.iterator().next();
			System.out.println("Mail " + added + " added:\n" + tree);
			return null;
		});

		CyrusPartition part = CyrusPartition.forServerAndDomain(cyrusReplication.server(), domainUid);
		this.partition = part.name;
		this.mboxRoot = "user." + userUid.replace('.', '^');

		this.apiKey = "sid";
		SecurityContext secCtx = new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(),
				domainUid);
		Sessions.get().put(apiKey, secCtx);

		provider = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "sid");

		mailboxFolderService = provider.instance(IMailboxFolders.class, partition, mboxRoot);
		outboxUid = ensureFolder("Outbox", 10, TimeUnit.SECONDS);
		sentUid = ensureFolder("Sent", 10, TimeUnit.SECONDS);
		sent_mailboxItemsService = provider.instance(IMailboxItems.class, sentUid);
		outboxMailboxItemsService = provider.instance(IMailboxItems.class, outboxUid);
		outboxService = provider.instance(IOutbox.class, domainUid, userUid);
		IUserInbox uinbox = provider.instance(IUserInbox.class, domainUid, userUid);

		IOfflineMgmt allocator = provider.instance(IOfflineMgmt.class, domainUid, userUid);
		System.err.println("Allocating " + ALLOC_COUNT + " offline id(s)");
		long allocTime = System.currentTimeMillis();
		this.allocations = allocator.allocateOfflineIds(ALLOC_COUNT);
		allocTime = System.currentTimeMillis() - allocTime;
		System.err.println("Allocation took " + allocTime + "ms.");

		int current = uinbox.unseen();

		imapAsUser(sc -> {
			int added = sc.append("INBOX", testEml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("INBOX");
			Collection<MimeTree> bs = sc.uidFetchBodyStructure(Arrays.asList(added));
			MimeTree tree = bs.iterator().next();
			System.out.println("Mail " + added + " added:\n" + tree);
			return null;
		});

		long delay = System.currentTimeMillis();
		int mailInbox = 0;
		do {
			Thread.sleep(200);
			mailInbox = uinbox.unseen();
			if (System.currentTimeMillis() - delay > 10000) {
				throw new TimeoutException("Hierarchy init took more than 10sec");
			}
		} while (mailInbox < current + 1);
		Thread.sleep(200);

		System.err.println("before is complete, starting test.");

	}

	private String ensureFolder(String name, long to, TimeUnit unit) throws InterruptedException, TimeoutException {
		long deadline = System.currentTimeMillis() + unit.toMillis(to);
		ItemValue<MailboxFolder> box = mailboxFolderService.byName(name);
		while (box == null) {
			if (System.currentTimeMillis() > deadline) {
				throw new TimeoutException(
						"Took more than " + unit.toMillis(to) + " to ensure " + name + " was replicated");
			}
			Thread.sleep(100);
			box = mailboxFolderService.byName(name);
		}
		return box.uid;
	}

	@After
	public void after() throws Exception {
		System.err.println("Test is over, after starts...");
		super.after();
	}

	@Test
	public void testFlushOutboxWorks() throws IOException {
		addMailToFolder();

		assertEquals(0, sent_mailboxItemsService.count(ItemFlagFilter.all()).total);

		long time = System.currentTimeMillis();
		CompletableFuture<Void> applyMailboxCompletetion = new ExpectCommand().onNextApplyMailbox(sentUid);

		outboxService.flush();

		try {
			applyMailboxCompletetion.get(5, TimeUnit.SECONDS);
			System.err.println("Flushed in " + (System.currentTimeMillis() - time) + "ms.");
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
			fail();
		}

		assertEquals(1, sent_mailboxItemsService.count(ItemFlagFilter.all()).total);
	}

	@Test
	public void testFlushOutboxExecutionTime() throws IOException {

		String emlPath = "data/mail_de_7Mo.eml";
		try (InputStream inputStream = AbstractReplicatedMailboxesServiceTests.class.getClassLoader()
				.getResourceAsStream(emlPath)) {
			Objects.requireNonNull(inputStream, "Failed to open resource @ " + emlPath);
			addMailToFolder(inputStream);
		} catch (Exception e) {
			throw new ServerFault(e);
		}

		assertEquals(0, sent_mailboxItemsService.count(ItemFlagFilter.all()).total);

		long time = System.currentTimeMillis();
		CompletableFuture<Void> applyMailboxCompletetion = new ExpectCommand().onNextApplyMailbox(sentUid);

		TaskRef flushTaskRef = outboxService.flush();
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), flushTaskRef);

		try {
			applyMailboxCompletetion.get(5, TimeUnit.SECONDS);
			System.err.println("Flushed in " + (System.currentTimeMillis() - time) + "ms.");
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
			fail();
		}

		assertEquals(1, sent_mailboxItemsService.count(ItemFlagFilter.all()).total);
	}

	@Test
	public void testEmptyFlushDoesNothing() throws Exception {

		String emlPath = "data/mail_de_7Mo.eml";
		long append = System.currentTimeMillis();
		try (InputStream inputStream = AbstractReplicatedMailboxesServiceTests.class.getClassLoader()
				.getResourceAsStream(emlPath)) {
			Objects.requireNonNull(inputStream, "Failed to open resource @ " + emlPath);
			this.addMailToFolder(inputStream);
			System.err.println("Append took " + (System.currentTimeMillis() - append) + "ms.");
		} catch (Exception e) {
			throw new ServerFault(e);
		}

		assertEquals(0, sent_mailboxItemsService.count(ItemFlagFilter.all()).total);

		long time = System.currentTimeMillis();
		CompletableFuture<Void> applyMailboxCompletetion = new ExpectCommand().onNextApplyMailbox(sentUid);

		TaskRef flushTaskRef = outboxService.flush();
		String log = TaskUtils.logStreamWait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM),
				flushTaskRef);

		try {
			applyMailboxCompletetion.get(5, TimeUnit.SECONDS);
			System.err.println("FIRST flush in " + (System.currentTimeMillis() - time) + "ms.");
			System.err.println(log);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
			fail();
		}

		assertEquals(1, sent_mailboxItemsService.count(ItemFlagFilter.all()).total);
		assertEquals(0, outboxMailboxItemsService.count(ItemFlagFilter.all().mustNot(ItemFlag.Deleted)).total);

		for (int i = 0; i < 10; i++) {
			time = System.currentTimeMillis();
			System.err.println("re-flush starts...");
			flushTaskRef = outboxService.flush();
			log = TaskUtils.logStreamWait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), flushTaskRef);
			System.err.println("LOG: " + log);
			System.err.println("re-flush " + i + " finishes after " + (System.currentTimeMillis() - time) + "ms.");
			assertEquals(1, sent_mailboxItemsService.count(ItemFlagFilter.all()).total);
			assertEquals(0, outboxMailboxItemsService.count(ItemFlagFilter.all().mustNot(ItemFlag.Deleted)).total);
		}

	}

	@Test
	public void testBigFlush42x7() throws Exception {

		String emlPath = "data/mail_de_7Mo.eml";
		long append = System.currentTimeMillis();
		double upload = 0;
		for (int i = 0; i < ALLOC_COUNT; i++) {
			try (InputStream inputStream = AbstractReplicatedMailboxesServiceTests.class.getClassLoader()
					.getResourceAsStream(emlPath)) {
				Objects.requireNonNull(inputStream, "Failed to open resource @ " + emlPath);
				upload += addMailToFolder(inputStream);
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		}
		append = System.currentTimeMillis() - append;

		assertEquals(0, sent_mailboxItemsService.count(ItemFlagFilter.all()).total);

		long time = System.currentTimeMillis();
		CompletableFuture<Void> applyMailboxCompletetion = new ExpectCommand().onNextApplyMailbox(sentUid);

		TaskRef flushTaskRef = outboxService.flush();
		String log = TaskUtils.logStreamWait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM),
				flushTaskRef);

		try {
			applyMailboxCompletetion.get(30, TimeUnit.SECONDS);
			System.err.println(log);
			System.err.println("FIRST flush in " + (System.currentTimeMillis() - time) + "ms.");
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
			fail();
		}

		assertEquals(ALLOC_COUNT, sent_mailboxItemsService.count(ItemFlagFilter.all()).total);
		assertEquals(0, outboxMailboxItemsService.count(ItemFlagFilter.all().mustNot(ItemFlag.Deleted)).total);
		System.err.println("Append of " + ALLOC_COUNT + " email(s) (" + ((int) (upload / 1024 / 1024))
				+ " MB) before flush took " + append + "ms.");

	}

	private void addMailToFolder() throws IOException {
		addMailToFolder(testEml());
	}

	/**
	 * @param eml
	 * @return the size of the uploaded eml in bytes
	 * @throws IOException
	 */
	private long addMailToFolder(InputStream eml) throws IOException {

		long time = System.currentTimeMillis();
		Buffer toUpload = null;
		ByteBufOutputStream out = new ByteBufOutputStream(Unpooled.buffer());// NOSONAR
		long uploaded = 0;
		try (Message parsed = Mime4JHelper.parse(eml, new OffloadedBodyFactory())) {
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
		IMailboxItems recordsApi = provider.instance(IMailboxItems.class, outboxUid);
		String partId = recordsApi.uploadPart(forUpload);
		assertNotNull(partId);
		partUpload = System.currentTimeMillis() - partUpload;
		System.err.println("Got partId " + partId + " in " + partUpload + "ms.");
		Part fullEml = Part.create(null, "message/rfc822", partId);
		MessageBody brandNew = new MessageBody();
		brandNew.structure = fullEml;
		MailboxItem item = new MailboxItem();
		item.body = brandNew;
		item.flags = Arrays.asList(new MailboxItemFlag("Pouic"));
		System.err.println("Before create WITH id.....");
		long createTime = System.currentTimeMillis();
		long nextId = allocations.globalCounter++;
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

}
