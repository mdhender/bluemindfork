package net.bluemind.mailshare.service.internal;

import net.bluemind.directory.service.DirValueStoreService.MailboxAdapter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailshare.api.Mailshare;

public class MailshareMailboxAdapter implements MailboxAdapter<Mailshare> {

	@Override
	public Mailbox asMailbox(String domainUid, String uid, Mailshare value) {
		return value.toMailbox();
	}

}
