package net.bluemind.delivery.lmtp.common;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.mime4j.dom.Message;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.delivery.lmtp.common.DeliveryContent.DeferredActionMessage;

public record DeliveryContent(String from, ResolvedBox box, ItemValue<MailboxReplica> folderItem, Message message,
		MailboxRecord mailboxRecord, boolean stop, long size, List<DeferredActionMessage> deferredActionMessages) {

	public record DeferredActionMessage(long ruleId, String ruleProvider, String deferredAction) {
		public JsonObject toJson() {
			JsonObject json = new JsonObject();
			json.put("ruleId", ruleId);
			json.put("ruleProvider", ruleProvider);
			json.put("deferredAction", deferredAction);
			return json;
		}
	}

	public DeliveryContent(String from, ResolvedBox box, ItemValue<MailboxReplica> folderItem, Message message,
			MailboxRecord mailboxRecord) {
		this(from, box, folderItem, message, mailboxRecord, false, 0l, new ArrayList<>());
	}

	public boolean isEmpty() {
		return message == null;
	}

	public DeliveryContent withBox(ResolvedBox box) {
		return new DeliveryContent(from, box, folderItem, message, mailboxRecord, stop, size, deferredActionMessages);
	}

	public DeliveryContent withSize(long size) {
		return new DeliveryContent(from, box, folderItem, message, mailboxRecord, stop, size, deferredActionMessages);
	}

	public DeliveryContent withStop(boolean stop) {
		return new DeliveryContent(from, box, folderItem, message, mailboxRecord, stop, size, deferredActionMessages);
	}

	public DeliveryContent withFolder(ItemValue<MailboxReplica> folderItem) {
		return new DeliveryContent(from, box, folderItem, message, mailboxRecord, stop, size, deferredActionMessages);
	}

	public DeliveryContent withMessage(Message message) {
		return new DeliveryContent(from, box, folderItem, message, mailboxRecord, stop, size, deferredActionMessages);
	}

	public DeliveryContent withoutMessage() {
		return new DeliveryContent(from, box, folderItem, null, mailboxRecord, stop, size, deferredActionMessages);
	}

	public DeliveryContent withDeferredAction(DeferredActionMessage deferredActionMessage) {
		deferredActionMessages.add(deferredActionMessage);
		return new DeliveryContent(from, box, folderItem, message, mailboxRecord, stop, size, deferredActionMessages);
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
