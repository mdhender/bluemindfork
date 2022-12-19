package net.bluemind.core.backup.continuous.events;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.hook.IMessageBodyHook;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.mailbox.api.IMailboxes;

public class MessageBodyHook implements IMessageBodyHook, ContinuousContenairization<MessageBody> {

	@Override
	public String type() {
		return "message_bodies";
	}

	@Override
	public void preCreate(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		MessageBody messageBody = fetchMessageBody(domainUid, ownerId, mailboxRecord);
		save(domainUid, ownerId, messageBody.guid, messageBody, true);
	}

	@Override
	public void preUpdate(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		MessageBody messageBody = fetchMessageBody(domainUid, ownerId, mailboxRecord);
		save(domainUid, ownerId, messageBody.guid, messageBody, false);
	}

	public static MessageBody fetchMessageBody(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailboxes mailboxesApi = prov.instance(IMailboxes.class, domainUid);
		var mailbox = mailboxesApi.getComplete(ownerId);
		String partition = CyrusPartition.forServerAndDomain(mailbox.value.dataLocation, domainUid).name;

		IDbMessageBodies apiMessageBodies = prov.instance(IDbMessageBodies.class, partition);
		return apiMessageBodies.getComplete(mailboxRecord.messageBody);
	}

}
