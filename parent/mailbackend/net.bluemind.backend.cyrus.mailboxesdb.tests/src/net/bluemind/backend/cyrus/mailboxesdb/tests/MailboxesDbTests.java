package net.bluemind.backend.cyrus.mailboxesdb.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.junit.Test;

import net.bluemind.backend.cyrus.mailboxesdb.MailboxesDb;
import net.bluemind.backend.cyrus.mailboxesdb.MailboxesDbEntry;
import net.bluemind.backend.cyrus.mailboxesdb.MailboxesDbEntry.Acl;
import net.bluemind.config.Token;
import net.bluemind.imap.AnnotationList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;

public class MailboxesDbTests {
	@Test
	public void getFlatMailboxesDb_validEntries() throws IOException {
		long now = System.currentTimeMillis() / 1000;

		MailboxesDb md = MailboxesDb.loadFromLineArray(new String[0]);
		md.mailboxesDbEntry.add(new MailboxesDbEntry("mailbox1name", "mailbox1uid", "mailbox1partition",
				Arrays.asList(new Acl("mailbox1aclname", "mailbox1aclperms")), now));
		md.mailboxesDbEntry.add(new MailboxesDbEntry("mailbox2name", "mailbox2uid", "mailbox2partition",
				Arrays.asList(new Acl("mailbox2aclname", "mailbox2aclperms"),
						new Acl("mailbox21aclname", "mailbox21aclperms")),
				now + 1));

		MailboxesDbEntry mde = new MailboxesDbEntry("mailbox3name", "mailbox3partition",
				Arrays.asList(new Acl("mailbox3aclname", "mailbox3aclperms")));
		md.mailboxesDbEntry.add(mde);

		String mailboxesDb = md.getFlatMailboxesDb();

		String expectedLine = "mailbox1name\t%(A %(mailbox1aclname mailbox1aclperms) I mailbox1uid P mailbox1partition M "
				+ now + ")";
		expectedLine += "\nmailbox2name\t%(A %(mailbox2aclname mailbox2aclperms mailbox21aclname mailbox21aclperms) I mailbox2uid P mailbox2partition M "
				+ (now + 1) + ")";
		expectedLine += "\nmailbox3name\t%(A %(mailbox3aclname mailbox3aclperms) P mailbox3partition M " + mde.timestamp
				+ ")\n";
		assertEquals(expectedLine, mailboxesDb);
	}

	@Test
	public void updateMailboxesDbPartition() throws IOException {
		String imapServerAddress = new BmConfIni().get("imap-role");
		INodeClient nc = NodeActivator.get(imapServerAddress);
		Map<String, BoxInfos> annotationsList = initCyrus(imapServerAddress, nc);

		MailboxesDb mailboxesDb = MailboxesDb.loadFromMailboxesDb(imapServerAddress);
		assertEquals(8, mailboxesDb.mailboxesDbEntry.size());

		mailboxesDb.mailboxesDbEntry.forEach(entry -> entry.partition = "dst_tld");
		MailboxesDb.writeToMailboxesDb(mailboxesDb);

		// Do FS partition move - not managed by mailboxesDb
		NCUtils.exec(nc, "rm -rf /var/spool/cyrus/data/dst_tld");
		NCUtils.exec(nc, "mv /var/spool/cyrus/data/orig_tld /var/spool/cyrus/data/dst_tld");
		NCUtils.exec(nc, "rm -rf /var/spool/cyrus/meta/dst_tld");
		NCUtils.exec(nc, "mv /var/spool/cyrus/meta/orig_tld /var/spool/cyrus/meta/dst_tld");

		checkCyrusService(imapServerAddress, "dst_tld", annotationsList);
	}

	private Map<String, BoxInfos> initCyrus(String imapServerAddress, INodeClient nc) throws IOException {
		String mailboxes = FileLocator
				.resolve(this.getClass().getResource("/resources/mailboxes-mix24-30LineFormat.db")).getFile();

		NCUtils.exec(nc, "service bm-cyrus-imapd stop");
		nc.writeFile("/var/lib/cyrus/mailboxes.db", new FileInputStream(mailboxes));
		NCUtils.exec(nc, "chown cyrus:mail /var/lib/cyrus/mailboxes.db");

		String cyrusPartitionContent = "partition-orig_tld:/var/spool/cyrus/data/orig_tld\n"
				+ "metapartition-orig_tld:/var/spool/cyrus/meta/orig_tld\n"
				+ "partition-dst_tld:/var/spool/cyrus/data/dst_tld\n"
				+ "metapartition-dst_tld:/var/spool/cyrus/meta/dst_tld\n";
		nc.writeFile("/etc/cyrus-partitions", new ByteArrayInputStream(cyrusPartitionContent.getBytes()));

		NCUtils.exec(nc,
				"rm -rf /var/spool/cyrus/data/orig_tld /var/spool/cyrus/data/dst_tld /var/spool/cyrus/meta/orig_tld /var/spool/cyrus/meta/dst_tld");

		NCUtils.exec(nc, "reconstruct -r -f *@test.fr");
		// reconstruct upgrade mailboxes.db, put it again
		nc.writeFile("/var/lib/cyrus/mailboxes.db", new FileInputStream(mailboxes));

		NCUtils.exec(nc, "service bm-cyrus-imapd start");

		return checkCyrusService(imapServerAddress, "orig_tld");
	}

	private Map<String, BoxInfos> checkCyrusService(String imapServerAddress, String expectedCyrusPartition) {
		return checkCyrusService(imapServerAddress, expectedCyrusPartition, null);
	}

	private class BoxInfos {
		public AnnotationList annotationList;
		public Map<String, net.bluemind.imap.Acl> acls;

		public BoxInfos(AnnotationList annotationList, Map<String, net.bluemind.imap.Acl> acls) {
			this.annotationList = annotationList;
			this.acls = acls;
		}
	}

	private Map<String, BoxInfos> checkCyrusService(String imapServerAddress, String expectedCyrusPartition,
			Map<String, BoxInfos> expectedAnnotations) {
		Map<String, BoxInfos> annotationsByName = new HashMap<>();

		try (StoreClient sc = new StoreClient(imapServerAddress, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			ListResult list = sc.listAll();
			assertEquals(8, list.size());

			list.forEach(entry -> {
				try {
					Map<String, net.bluemind.imap.Acl> acls = sc.listAcl(entry.getName());
					assertTrue(acls.size() > 1);

					AnnotationList annotations = sc.getAnnotation(entry.getName());
					assertNotNull(annotations);
					assertNotEquals(0, annotations.size());
					assertEquals(expectedCyrusPartition,
							annotations.get("/vendor/cmu/cyrus-imapd/partition").valueShared);
					assertNotNull(annotations.get("/vendor/cmu/cyrus-imapd/uniqueid").valueShared);

					if (expectedAnnotations != null) {
						assertNotNull(expectedAnnotations.get(entry.getName()));

						BoxInfos expectedBoxInfos = expectedAnnotations.get(entry.getName());
						assertEquals(
								expectedBoxInfos.annotationList.get("/vendor/cmu/cyrus-imapd/uniqueid").valueShared,
								annotations.get("/vendor/cmu/cyrus-imapd/uniqueid").valueShared);

						assertEquals(expectedBoxInfos.acls.size(), acls.size());
						expectedBoxInfos.acls.forEach((n, a) -> {
							assertNotNull(acls.get(n));
							assertEquals(a.toString(), acls.get(n).toString());
						});
					}

					annotationsByName.put(entry.getName(), new BoxInfos(annotations, acls));
				} catch (IMAPException e) {
					fail("Test thrown exception: " + e.getMessage());
				}
			});
		}

		return annotationsByName;
	}
}
