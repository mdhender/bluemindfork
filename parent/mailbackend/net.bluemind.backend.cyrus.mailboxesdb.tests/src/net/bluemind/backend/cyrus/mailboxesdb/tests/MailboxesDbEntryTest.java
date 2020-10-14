package net.bluemind.backend.cyrus.mailboxesdb.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import net.bluemind.backend.cyrus.mailboxesdb.MailboxesDb;
import net.bluemind.backend.cyrus.mailboxesdb.MailboxesDbEntry;
import net.bluemind.backend.cyrus.mailboxesdb.MailboxesDbEntry.Acl;

public class MailboxesDbEntryTest {
	@Test
	public void Acl_toString() {
		assertEquals("acl1 perms1", new Acl("acl1", "perms1").toString());

		assertNull(new Acl(null, null).toString());
		assertNull(new Acl("acl", null).toString());
	}

	@Test
	public void MailboxesDbEntry_create() {
		long now = System.currentTimeMillis() / 1000;

		Acl acl = new MailboxesDbEntry.Acl("aclname1", "aclperms1");
		MailboxesDbEntry m = new MailboxesDbEntry("mailboxname", "mailboxuid", "mailboxpartition", Arrays.asList(acl),
				now);

		assertEquals("mailboxname", m.name);
		assertTrue(m.uid.isPresent());
		assertEquals("mailboxuid", m.uid.get());
		assertEquals("mailboxpartition", m.partition);
		assertEquals(now, m.timestamp);

		assertEquals(1, m.acls.size());
		assertEquals("aclname1", m.acls.get(0).name);
		assertEquals("aclperms1", m.acls.get(0).perms);

		assertEquals(
				String.format("mailboxname\t%%(A %%(aclname1 aclperms1) I mailboxuid P mailboxpartition M %s)", now),
				m.toString());
	}

	@Test
	public void MailboxesDbEntry_toString_noOrInvalidAcl() {
		long now = System.currentTimeMillis() / 1000;

		MailboxesDbEntry m = new MailboxesDbEntry("mailboxname", "mailboxuid", "mailboxpartition",
				Collections.emptyList(), now);
		assertEquals(String.format("mailboxname\t%%(I mailboxuid P mailboxpartition M %s)", now), m.toString());

		Acl acl = new MailboxesDbEntry.Acl(null, null);
		m = new MailboxesDbEntry("mailboxname", "mailboxuid", "mailboxpartition", Arrays.asList(acl), now);
		assertEquals(String.format("mailboxname\t%%(I mailboxuid P mailboxpartition M %s)", now), m.toString());
	}

	@Test
	public void MailboxesDbEntry_toString_noPartition() {
		long now = System.currentTimeMillis() / 1000;

		Acl acl = new MailboxesDbEntry.Acl("aclname1", "aclperms1");

		MailboxesDbEntry m = new MailboxesDbEntry("mailboxname", "mailboxuid", null, Arrays.asList(acl), now);
		assertEquals(String.format("mailboxname\t%%(A %%(aclname1 aclperms1) I mailboxuid M %s)", now), m.toString());

		m = new MailboxesDbEntry("mailboxname", "mailboxuid", "", Arrays.asList(acl), now);
		assertEquals(String.format("mailboxname\t%%(A %%(aclname1 aclperms1) I mailboxuid M %s)", now), m.toString());
	}

	@Test
	public void MailboxesDbEntry_toString_noUid() {
		long now = System.currentTimeMillis() / 1000;

		Acl acl = new MailboxesDbEntry.Acl("aclname1", "aclperms1");

		MailboxesDbEntry m = new MailboxesDbEntry("mailboxname", null, "mailboxpartition", Arrays.asList(acl), now);
		assertEquals(String.format("mailboxname\t%%(A %%(aclname1 aclperms1) P mailboxpartition M %s)", now),
				m.toString());

		m = new MailboxesDbEntry("mailboxname", "", "mailboxpartition", Arrays.asList(acl), now);
		assertEquals(String.format("mailboxname\t%%(A %%(aclname1 aclperms1) P mailboxpartition M %s)", now),
				m.toString());

		m = new MailboxesDbEntry("mailboxname", "mailboxpartition", Arrays.asList(acl));
		assertTrue(
				m.toString().matches("^mailboxname\t%\\(A %\\(aclname1 aclperms1\\) P mailboxpartition M [0-9]+\\)$"));
	}

	@Test
	public void MailboxesDbEntry_toString_noInformations() {
		MailboxesDbEntry m = new MailboxesDbEntry("mailboxname", null, Collections.emptyList());
		assertTrue(m.toString().matches("^mailboxname\t%\\(M [0-9]+\\)$"));
	}

	@Test
	public void loadFromLineArray_cyrus3_invalidLine() {
		assertNull(MailboxesDbEntry.getFromString(
				"apr-vmnet.loc!user.admin %(A %(admin0 lrswipkxtecda) I 5d01614958d24926 P apr-vmnet_loc M 1533736213)"));
		assertNull(MailboxesDbEntry.getFromString("popo"));
		assertNull(MailboxesDbEntry.getFromString(""));
		assertNull(MailboxesDbEntry.getFromString("    "));
		assertNull(MailboxesDbEntry.getFromString("%(A invalid)"));
		assertNull(MailboxesDbEntry.getFromString("\t%(A invalid)"));
	}

	@Test
	public void loadFromLineArray_cyrus3_validLine_allFields() {
		String line = "apr-vmnet.loc!user.admin\t%(A %(admin0 lrswipkxtecda FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc lrswipkxtecda group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc lrsp) I 5d01614958d24926 P apr-vmnet_loc M 1533736213)";

		MailboxesDbEntry mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertTrue(mdEntry.uid.isPresent());
		assertEquals("5d01614958d24926", mdEntry.uid.get());
		assertEquals(1533736213, mdEntry.timestamp);

		assertEquals(3, mdEntry.acls.size());
		mdEntry.acls.forEach(e -> {
			if (e.name.equals("admin0")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc")) {
				assertEquals("lrsp", e.perms);
			} else {
				fail(String.format("Unknown acl for %s, perms %s", e.name, e.perms));
			}
		});

		line = "apr-vmnet.loc!user.admin.BlueMind.BM mail list\t%(A %(admin0 lrswipkxtecda FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc lrswipkxtecda group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc lrsp) I 5d01614958d24926 P apr-vmnet_loc M 1533736213)";

		mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin.BlueMind.BM mail list", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertTrue(mdEntry.uid.isPresent());
		assertEquals("5d01614958d24926", mdEntry.uid.get());
		assertEquals(1533736213, mdEntry.timestamp);

		assertEquals(3, mdEntry.acls.size());
		mdEntry.acls.forEach(e -> {
			if (e.name.equals("admin0")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc")) {
				assertEquals("lrsp", e.perms);
			} else {
				fail(String.format("Unknown acl for %s, perms %s", e.name, e.perms));
			}
		});

		line = "apr-vmnet.loc!user.admin.BlueMind.T&AOk-to titi\t%(A %(admin0 lrswipkxtecda FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc lrswipkxtecda group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc lrsp) I 5d01614958d24926 P apr-vmnet_loc M 1533736213)";

		mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin.BlueMind.T&AOk-to titi", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertTrue(mdEntry.uid.isPresent());
		assertEquals("5d01614958d24926", mdEntry.uid.get());
		assertEquals(1533736213, mdEntry.timestamp);

		assertEquals(3, mdEntry.acls.size());
		mdEntry.acls.forEach(e -> {
			if (e.name.equals("admin0")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc")) {
				assertEquals("lrsp", e.perms);
			} else {
				fail(String.format("Unknown acl for %s, perms %s", e.name, e.perms));
			}
		});
	}

	@Test
	public void loadFromLineArray_cyrus3_invalidAclsList() {
		String line = "apr-vmnet.loc!user.admin\t%(A %(admin0 FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc lrswipkxtecda group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc lrsp) I 5d01614958d24926 P apr-vmnet_loc M 1533736213)";

		MailboxesDbEntry mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertTrue(mdEntry.uid.isPresent());
		assertEquals("5d01614958d24926", mdEntry.uid.get());
		assertEquals(1533736213, mdEntry.timestamp);

		assertEquals(0, mdEntry.acls.size());
	}

	@Test
	public void loadFromLineArray_cyrus3_noAcls() {
		String line = "apr-vmnet.loc!user.admin\t%(I 5d01614958d24926 P apr-vmnet_loc M 1533736213)";

		MailboxesDbEntry mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertTrue(mdEntry.uid.isPresent());
		assertEquals("5d01614958d24926", mdEntry.uid.get());
		assertEquals(1533736213, mdEntry.timestamp);

		assertEquals(0, mdEntry.acls.size());
	}

	@Test
	public void loadFromLineArray_cyrus3_noTimestamp() {
		String line = "apr-vmnet.loc!user.admin\t%(A %(admin0 lrswipkxtecda FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc lrswipkxtecda group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc lrsp) I 5d01614958d24926 P apr-vmnet_loc)";

		MailboxesDbEntry mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertTrue(mdEntry.uid.isPresent());
		assertEquals("5d01614958d24926", mdEntry.uid.get());
		assertNotNull(mdEntry.timestamp);
		// 1533736213 == 8/8/2018 à 15:50:13
		assertTrue(mdEntry.timestamp > 1533736213);

		assertEquals(3, mdEntry.acls.size());
	}

	@Test
	public void loadFromLineArray_cyrus3_noPartition() {
		String line = "apr-vmnet.loc!user.admin\t%(A %(admin0 lrswipkxtecda FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc lrswipkxtecda group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc lrsp) I 5d01614958d24926 M 1533736213)";

		MailboxesDbEntry mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin", mdEntry.name);
		assertNull(mdEntry.partition);
		assertTrue(mdEntry.uid.isPresent());
		assertEquals("5d01614958d24926", mdEntry.uid.get());
		assertEquals(1533736213, mdEntry.timestamp);

		assertEquals(3, mdEntry.acls.size());
		mdEntry.acls.forEach(e -> {
			if (e.name.equals("admin0")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc")) {
				assertEquals("lrsp", e.perms);
			} else {
				fail(String.format("Unknown acl for %s, perms %s", e.name, e.perms));
			}
		});
	}

	@Test
	public void loadFromLineArray_cyrus3_noUid() {
		String line = "apr-vmnet.loc!user.admin\t%(A %(admin0 lrswipkxtecda FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc lrswipkxtecda group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc lrsp) P apr-vmnet_loc M 1533736213)";

		MailboxesDbEntry mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertFalse(mdEntry.uid.isPresent());
		assertEquals(1533736213, mdEntry.timestamp);

		assertEquals(3, mdEntry.acls.size());
		mdEntry.acls.forEach(e -> {
			if (e.name.equals("admin0")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc")) {
				assertEquals("lrsp", e.perms);
			} else {
				fail(String.format("Unknown acl for %s, perms %s", e.name, e.perms));
			}
		});
	}

	@Test
	public void loadFromLineArray_cyrus24_validLine_allFields() {
		String line = "apr-vmnet.loc!user.admin\t0 apr-vmnet_loc admin0\tlrswipkxtecda\tFF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc\tlrswipkxtecda\tgroup:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc\tlrsp\t";

		MailboxesDbEntry mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertFalse(mdEntry.uid.isPresent());
		// 1533736213 == 8/8/2018 à 15:50:13
		assertTrue(mdEntry.timestamp > 1533736213);

		assertEquals(3, mdEntry.acls.size());
		mdEntry.acls.forEach(e -> {
			if (e.name.equals("admin0")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc")) {
				assertEquals("lrsp", e.perms);
			} else {
				fail(String.format("Unknown acl for %s, perms %s", e.name, e.perms));
			}
		});

		line = "apr-vmnet.loc!user.admin.BlueMind.BM mail list\t0 apr-vmnet_loc admin0\tlrswipkxtecda\tFF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc\tlrswipkxtecda\tgroup:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc\tlrsp\t";

		mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin.BlueMind.BM mail list", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertFalse(mdEntry.uid.isPresent());
		// 1533736213 == 8/8/2018 à 15:50:13
		assertTrue(mdEntry.timestamp > 1533736213);

		assertEquals(3, mdEntry.acls.size());
		mdEntry.acls.forEach(e -> {
			if (e.name.equals("admin0")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc")) {
				assertEquals("lrsp", e.perms);
			} else {
				fail(String.format("Unknown acl for %s, perms %s", e.name, e.perms));
			}
		});

		line = "apr-vmnet.loc!user.admin.BlueMind.T&AOk-to titi\t0 apr-vmnet_loc admin0\tlrswipkxtecda\tFF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc\tlrswipkxtecda\tgroup:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc\tlrsp\t";

		mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin.BlueMind.T&AOk-to titi", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertFalse(mdEntry.uid.isPresent());
		// 1533736213 == 8/8/2018 à 15:50:13
		assertTrue(mdEntry.timestamp > 1533736213);

		assertEquals(3, mdEntry.acls.size());
		mdEntry.acls.forEach(e -> {
			if (e.name.equals("admin0")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc")) {
				assertEquals("lrsp", e.perms);
			} else {
				fail(String.format("Unknown acl for %s, perms %s", e.name, e.perms));
			}
		});
	}

	@Test
	public void loadFromLineArray_cyrus24_cyrus3Converted_validLine_allFields() throws IOException {
		String line = "apr-vmnet.loc!user.admin\t0 apr-vmnet_loc admin0" + MailboxesDbEntry.ACL_24LINE_CVT3_SEP
				+ "lrswipkxtecda" + MailboxesDbEntry.ACL_24LINE_CVT3_SEP
				+ "FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc" + MailboxesDbEntry.ACL_24LINE_CVT3_SEP
				+ "lrswipkxtecda" + MailboxesDbEntry.ACL_24LINE_CVT3_SEP
				+ "group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc" + MailboxesDbEntry.ACL_24LINE_CVT3_SEP
				+ "lrsp" + MailboxesDbEntry.ACL_24LINE_CVT3_SEP;

		MailboxesDbEntry mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertFalse(mdEntry.uid.isPresent());
		// 1533736213 == 8/8/2018 à 15:50:13
		assertTrue(mdEntry.timestamp > 1533736213);

		assertEquals(3, mdEntry.acls.size());
		mdEntry.acls.forEach(e -> {
			if (e.name.equals("admin0")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("FF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc")) {
				assertEquals("lrswipkxtecda", e.perms);
			} else if (e.name.equals("group:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc")) {
				assertEquals("lrsp", e.perms);
			} else {
				fail(String.format("Unknown acl for %s, perms %s", e.name, e.perms));
			}
		});
	}

	@Test
	public void loadFromLineArray_cyrus24_noAcls() {
		String line = "apr-vmnet.loc!user.admin\t0 apr-vmnet_loc";

		MailboxesDbEntry mdEntry = MailboxesDbEntry.getFromString(line);
		assertEquals("apr-vmnet.loc!user.admin", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertFalse(mdEntry.uid.isPresent());
		// 1533736213 == 8/8/2018 à 15:50:13
		assertTrue(mdEntry.timestamp > 1533736213);

		assertEquals(0, mdEntry.acls.size());
	}

	@Test
	public void loadFromLineArray_cyrus24_invalidLine() {
		assertNull(MailboxesDbEntry.getFromString(
				"apr-vmnet.loc!user.admin 0 apr-vmnet_loc admin0\tlrswipkxtecda\tFF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc\tlrswipkxtecda\tgroup:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc\tlrsp\t"));
		assertNull(MailboxesDbEntry.getFromString(
				"apr-vmnet.loc!user.admin\t0 admin0\tlrswipkxtecda\tFF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc\tlrswipkxtecda\tgroup:3BE51DA3-40B0-45D3-A073-29DB23BEF786@apr-vmnet.loc\tlrsp\t"));
		assertNull(MailboxesDbEntry.getFromString(""));
		assertNull(MailboxesDbEntry.getFromString("    "));
		assertNull(MailboxesDbEntry.getFromString("\t invalid"));
		assertNull(MailboxesDbEntry.getFromString("  \\t0 part acls1\\tperms1\\t"));
	}

	@Test
	public void loadFromLineArray_cyrus24_invalidAclsList() {
		MailboxesDbEntry mdEntry = MailboxesDbEntry.getFromString(
				"apr-vmnet.loc!user.admin\t0 apr-vmnet_loc admin0\tlrswipkxtecda\tFF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc\t");
		assertEquals("apr-vmnet.loc!user.admin", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertFalse(mdEntry.uid.isPresent());
		// 1533736213 == 8/8/2018 à 15:50:13
		assertTrue(mdEntry.timestamp > 1533736213);
		assertEquals(0, mdEntry.acls.size());

		mdEntry = MailboxesDbEntry.getFromString(
				"apr-vmnet.loc!user.admin\t0 apr-vmnet_loc admin0\tlrswipkxtecda\tFF14A785-4867-430D-B714-F8D2B5374546@apr-vmnet.loc");
		assertEquals("apr-vmnet.loc!user.admin", mdEntry.name);
		assertEquals("apr-vmnet_loc", mdEntry.partition);
		assertFalse(mdEntry.uid.isPresent());
		// 1533736213 == 8/8/2018 à 15:50:13
		assertTrue(mdEntry.timestamp > 1533736213);
		assertEquals(0, mdEntry.acls.size());
	}

	@Test
	public void testMailboxesDbContainingEntriesOnDefaultPartitionShouldSkipTheseEntries() throws IOException {
		String[] lines = new String[] {
				"etudiant.univ.fr!_admin\t0 my_space_fr anyone<FF><89>p<FF><89>admin0<FF><89>lrswipkxtecda<FF><89>\n",
				"user.0815\t0 default anyone<FF><89>p<FF><89>admin0<FF><89>lrswipkxtecda<FF><89>",
				"etudiant2.univ.fr!_admin\t0 my_space_fr anyone<FF><89>p<FF><89>admin0<FF><89>lrswipkxtecda<FF><89>\n" };
		MailboxesDb db = MailboxesDb.loadFromLineArray(lines);

		assertEquals(2, db.mailboxesDbEntry.size());
		assertFalse(db.mailboxesDbEntry.stream().filter(entry -> entry.name.equals("user.0815")).findAny().isPresent());
	}
}
