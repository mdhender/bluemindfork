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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.replication.testhelper.ExpectCommand;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.IOutbox;
import net.bluemind.backend.mail.api.IUserInbox;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.mime.MimeTree;

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
	public void testFlushOutboxWorks() throws IOException, InterruptedException {
		addMailToFolder(outboxUid);

		assertEquals(0, sent_mailboxItemsService.count(ItemFlagFilter.all()).total);

		long time = System.currentTimeMillis();
		CompletableFuture<Void> applyMailboxCompletetion = new ExpectCommand().onNextApplyMailbox(sentUid);

		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), outboxService.flush());

		try {
			applyMailboxCompletetion.get(5, TimeUnit.SECONDS);
			System.err.println("Flushed in " + (System.currentTimeMillis() - time) + "ms.");
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
			fail();
		}
		long curTotal = sent_mailboxItemsService.count(ItemFlagFilter.all()).total;
		System.err.println("Should check now => " + curTotal);

		Thread.sleep(500); // apply mailbox is async now
		assertEquals(1L, sent_mailboxItemsService.count(ItemFlagFilter.all()).total);
	}

	@Test
	public void testFlushOutboxShouldAddCollectedContacts() throws Exception {
		addMailToFolder(outboxUid, "with_inlines2.ftl");

		CompletableFuture<Void> applyMailboxCompletetion = new ExpectCommand().onNextApplyMailbox(sentUid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), outboxService.flush());
		applyMailboxCompletetion.get(500, TimeUnit.SECONDS);

		String abUid = IAddressBookUids.collectedContactsUserAddressbook(userUid);
		IAddressBook service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAddressBook.class, abUid);
		List<String> allUids = service.allUids();

		assertEquals(2, allUids.size());
		ItemValue<VCard> tom = null;
		ItemValue<VCard> david = null;
		for (String vcardUid : allUids) {
			ItemValue<VCard> vcard = service.getComplete(vcardUid);
			String email = vcard.value.defaultMail();
			if (email.equals("david.phan@bluemind.net")) {
				david = vcard;
			} else if (email.equals("thomas.cataldo@blue-mind.net")) {
				tom = vcard;
			}
		}
		assertNotNull(david);
		assertNotNull(tom);
		assertEquals(david.value.identification.name.familyNames, "Phan");
		assertEquals(david.value.identification.name.givenNames, "David");
		assertEquals(tom.value.identification.name.familyNames, "Cataldo");
		assertEquals(tom.value.identification.name.givenNames, "Thomas");
	}

	@Test
	public void testFlushOutboxShouldAddOnlyOnceCollectedContactsWhenDuplicate() throws Exception {
		addMailToFolder(outboxUid);
		addMailToFolder(outboxUid);

		CompletableFuture<Void> applyMailboxCompletetion = new ExpectCommand().onNextApplyMailbox(sentUid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), outboxService.flush());
		applyMailboxCompletetion.get(500, TimeUnit.SECONDS);

		String abUid = IAddressBookUids.collectedContactsUserAddressbook(userUid);
		IAddressBook service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAddressBook.class, abUid);
		List<String> allUids = service.allUids();

		assertEquals(2, allUids.size());
	}

	@Test
	public void testFlushOutboxShouldOnlyAddMissingCollectedContacts() throws Exception {
		addMailToFolder(outboxUid);

		CompletableFuture<Void> applyMailboxCompletetion = new ExpectCommand().onNextApplyMailbox(sentUid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), outboxService.flush());
		applyMailboxCompletetion.get(500, TimeUnit.SECONDS);

		String abUid = IAddressBookUids.collectedContactsUserAddressbook(userUid);
		IAddressBook service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAddressBook.class, abUid);
		List<String> allUids = service.allUids();

		assertEquals(2, allUids.size());

		service.delete(allUids.get(0));

		addMailToFolder(outboxUid);
		assertEquals(2, allUids.size());

	}

	@Test
	public void testFlushOutboxShouldAddOnlyOnceCollectedContactsWhenDuplicateBetweenMultipleFlushes()
			throws Exception {
		addMailToFolder(outboxUid);
		addMailToFolder(outboxUid);

		CompletableFuture<Void> applyMailboxCompletetion = new ExpectCommand().onNextApplyMailbox(sentUid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), outboxService.flush());
		applyMailboxCompletetion.get(500, TimeUnit.SECONDS);

		String abUid = IAddressBookUids.collectedContactsUserAddressbook(userUid);
		IAddressBook service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAddressBook.class, abUid);
		List<String> allUids = service.allUids();

		assertEquals(2, allUids.size());

		addMailToFolder(outboxUid);

		applyMailboxCompletetion = new ExpectCommand().onNextApplyMailbox(sentUid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), outboxService.flush());
		applyMailboxCompletetion.get(500, TimeUnit.SECONDS);

		allUids = service.allUids();
		assertEquals(2, allUids.size());
	}

	@Test
	public void testFlushOutboxExecutionTime() throws IOException {

		String emlPath = "data/mail_de_7Mo.eml";
		try (InputStream inputStream = AbstractReplicatedMailboxesServiceTests.class.getClassLoader()
				.getResourceAsStream(emlPath)) {
			Objects.requireNonNull(inputStream, "Failed to open resource @ " + emlPath);
			addMailToFolder(inputStream, outboxUid);
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
			this.addMailToFolder(inputStream, outboxUid);
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
				upload += addMailToFolder(inputStream, outboxUid);
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

}
