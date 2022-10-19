package net.bluemind.delivery.lmtp.common;

import org.apache.james.mime4j.dom.Message;

import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;

public record DeliveryContent(String from, ResolvedBox box, ItemValue<MailboxReplica> folderItem, Message message,
		MailboxRecord mailboxRecord, boolean stop, long size) {

	public DeliveryContent(String from, ResolvedBox box, ItemValue<MailboxReplica> folderItem, Message message,
			MailboxRecord mailboxRecord) {
		this(from, box, folderItem, message, mailboxRecord, false, 0l);
	}

	public boolean isEmpty() {
		return message == null;
	}

	public DeliveryContent withSize(long size) {
		return new DeliveryContent(from, box, folderItem, message, mailboxRecord, stop, size);
	}

	public DeliveryContent withStop(boolean stop) {
		return new DeliveryContent(from, box, folderItem, message, mailboxRecord, stop, size);
	}

	public DeliveryContent withFolder(ItemValue<MailboxReplica> folderItem) {
		return new DeliveryContent(from, box, folderItem, message, mailboxRecord, stop, size);
	}

	public DeliveryContent withMessage(Message message) {
		return new DeliveryContent(from, box, folderItem, message, mailboxRecord, stop, size);
	}

	public DeliveryContent withoutMessage() {
		return new DeliveryContent(from, box, folderItem, null, mailboxRecord, stop, size);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("DeliveryContent[");
		builder.append("from=").append(from).append(", ");
		builder.append("box=").append(box().entry.email).append(", ");
		builder.append("folderUid=").append(folderItem().uid).append(", ");
		if (message() != null) {
			builder.append("messageId=").append(message.getMessageId()).append(", ");
			builder.append("messageSubject=").append(message.getSubject());
		} else {
			builder.append("message=false");
		}
		return builder.append("]").toString();
	}
}
