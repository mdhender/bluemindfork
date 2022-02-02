package net.bluemind.core.backup.continuous.events;

import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.identity.api.Identity;
import net.bluemind.mailbox.identity.hook.IMailboxIdentityHook;

public class MailboxIdentityContinuousHook implements IMailboxIdentityHook, ContinuousContenairization<Identity> {

	@Override
	public String type() {
		return "mailboxIdentity";
	}

	@Override
	public void onCreate(BmContext context, String domainUid, String mailboxUid, String id, Identity identity) {
		save(domainUid, mailboxUid, id, identity, true);
	}

	@Override
	public void onUpdate(BmContext context, String domainUid, String mailboxUid, String id, Identity identity) {
		save(domainUid, mailboxUid, id, identity, false);
	}

	@Override
	public void onDelete(BmContext context, String domainUid, String mailboxUid, String id, Identity previous) {
		delete(domainUid, mailboxUid, id, previous);
	}

}
