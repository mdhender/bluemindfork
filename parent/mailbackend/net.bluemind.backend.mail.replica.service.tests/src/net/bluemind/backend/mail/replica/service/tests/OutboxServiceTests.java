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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vertx.java.core.buffer.Buffer;

import com.google.common.io.ByteStreams;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.replication.testhelper.ExpectCommand;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.IOutbox;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.backend.mail.replica.service.tests.ReplicationEventsRecorder.Hierarchy;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.mime.MimeTree;

public class OutboxServiceTests extends AbstractRollingReplicationTests {

	private String apiKey;
	private String partition;
	private String mboxRoot;
	private ClientSideServiceProvider provider;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("es.mailspool.count", "1");
	}

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

		long delay = System.currentTimeMillis();
		Hierarchy hierarchy = null;
		do {
			Thread.sleep(200);
			hierarchy = rec.hierarchy(domainUid, userUid);
			System.out.println("Hierarchy version is " + hierarchy.exactVersion);
			if (System.currentTimeMillis() - delay > 10000) {
				throw new TimeoutException("Hierarchy init took more than 10sec");
			}
		} while (hierarchy.exactVersion < 7);
		System.out.println("Hierarchy is now at version " + hierarchy.exactVersion);
		System.err.println("before is complete, starting test.");

		provider = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "sid");
	}

	@After
	public void after() throws Exception {
		System.err.println("Test is over, after starts...");
		super.after();
	}

	@Test
	public void testFlushOutbox() throws IOException {
		// create a mail in outbox folder
		IMailboxFolders mailboxFolderService = provider.instance(IMailboxFolders.class, partition, mboxRoot);
		String outboxUid = mailboxFolderService.byName("Outbox").uid;
		this.addMailToFolder(outboxUid);

		String sentUid = mailboxFolderService.byName("Sent").uid;
		IMailboxItems mailboxItemsService = provider.instance(IMailboxItems.class, sentUid);
		assertEquals(0, mailboxItemsService.count(ItemFlagFilter.all()).total);

		provider.instance(IOutbox.class, domainUid, userUid).flush();

		CompletableFuture<Void> applyMailboxCompletetion = new ExpectCommand().onNextApplyMailbox(sentUid);

		try {
			applyMailboxCompletetion.get(5, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
			fail();
		}

		assertEquals(1, mailboxItemsService.count(ItemFlagFilter.all()).total);
	}

	private long addMailToFolder(String mailboxUid) throws IOException {
		assertNotNull(mailboxUid);

		try (InputStream in = testEml()) {
			Stream forUpload = VertxStream.stream(new Buffer(ByteStreams.toByteArray(in)));
			IMailboxItems recordsApi = provider.instance(IMailboxItems.class, mailboxUid);
			String partId = recordsApi.uploadPart(forUpload);
			assertNotNull(partId);
			System.out.println("Got partId " + partId);
			Part fullEml = Part.create(null, "message/rfc822", partId);
			MessageBody brandNew = new MessageBody();
			brandNew.subject = "toto";
			brandNew.structure = fullEml;
			MailboxItem item = new MailboxItem();
			item.body = brandNew;
			item.otherFlags = Arrays.asList("Pouic");
			System.err.println("Before create by id.....");
			long itemId = recordsApi.create(item).id;
			ItemValue<MailboxItem> reloaded = recordsApi.getCompleteById(itemId);
			assertNotNull(reloaded);
			assertNotNull(reloaded.value.body.headers);
			Optional<Header> idHeader = reloaded.value.body.headers.stream()
					.filter(h -> h.name.equals(MailApiHeaders.X_BM_INTERNAL_ID)).findAny();
			assertTrue(idHeader.isPresent());
			return itemId;
		}
	}
}
