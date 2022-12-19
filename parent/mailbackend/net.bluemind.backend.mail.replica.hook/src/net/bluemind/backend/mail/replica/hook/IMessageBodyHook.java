package net.bluemind.backend.mail.replica.hook;

import net.bluemind.backend.mail.replica.api.MailboxRecord;

public interface IMessageBodyHook {

	public void preCreate(String domainUid, String ownerId, MailboxRecord mailboxRecord);

	public void preUpdate(String domainUid, String ownerId, MailboxRecord mailboxRecord);
}
