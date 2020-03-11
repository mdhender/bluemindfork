package net.bluemind.directory.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IDirEntryPath;

public class IDirEntryPathTest {

	@Test
	public void path() {
		assertEquals("my-domain/users/user-uid", IDirEntryPath.path("my-domain", "user-uid", Kind.USER));
		assertEquals("another-domain/addressbooks/another-uid",
				IDirEntryPath.path("another-domain", "another-uid", Kind.ADDRESSBOOK));
		assertEquals("my-domain/calendars/cuid", IDirEntryPath.path("my-domain", "cuid", Kind.CALENDAR));
		assertEquals("my-domain/externalusers/ext-uid", IDirEntryPath.path("my-domain", "ext-uid", Kind.EXTERNALUSER));
		assertEquals("my-domain/groups/guid", IDirEntryPath.path("my-domain", "guid", Kind.GROUP));
		assertEquals("my-domain/mailshares/ms-uid", IDirEntryPath.path("my-domain", "ms-uid", Kind.MAILSHARE));
		assertEquals("my-domain/ous/org-unit-uid", IDirEntryPath.path("my-domain", "org-unit-uid", Kind.ORG_UNIT));
		assertEquals("my-domain/resources/ruid", IDirEntryPath.path("my-domain", "ruid", Kind.RESOURCE));
	}
}
