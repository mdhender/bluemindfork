package net.bluemind.mailshare.service.internal;

import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirValueStoreService.DirEntryAdapter;
import net.bluemind.mailshare.api.Mailshare;

public class MailshareDirEntryAdapter implements DirEntryAdapter<Mailshare> {

	@Override
	public DirEntry asDirEntry(String domainUid, String uid, Mailshare mailshare) {
		return DirEntry.create(mailshare.orgUnitUid, domainUid + "/mailshares/" + uid, DirEntry.Kind.MAILSHARE, uid,
				mailshare.name, mailshare.defaultEmailAddress(domainUid), mailshare.hidden, mailshare.system,
				mailshare.archived, mailshare.dataLocation);
	}

}
