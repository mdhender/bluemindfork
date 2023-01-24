package net.bluemind.core.backup.continuous.mgmt.service.containers.mail;

import org.slf4j.event.Level;

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.events.ContinuousContenairization;
import net.bluemind.core.backup.continuous.events.MessageBodyHook;
import net.bluemind.core.backup.continuous.mgmt.service.containers.mail.RecordsSync.BodyStat;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.Domain;

class MessageBodySync implements ContinuousContenairization<MessageBody> {

	private static final String MESSAGE_BODIES = "message_bodies";

	private IBackupStoreFactory target;
	private ItemValue<Domain> domain;
	private BaseContainerDescriptor cont;
	private IServerTaskMonitor contMon;

	public MessageBodySync(IBackupStoreFactory target, ItemValue<Domain> domain, IServerTaskMonitor contMon,
			BaseContainerDescriptor cont) {
		this.target = target;
		this.domain = domain;
		this.cont = cont;
		this.contMon = contMon;
	}

	@Override
	public String type() {
		return MESSAGE_BODIES;
	}

	@Override
	public IBackupStoreFactory targetStore() {
		return target;
	}

	public void storeMessageBodies(BodyStat bodyStat, ItemValue<MailboxRecord> mailboxRecord) {

		String mb = mailboxRecord.value.messageBody;
		if (mb != null) {
			MessageBody messageBody = MessageBodyHook.fetchMessageBody(domain.uid, cont.owner, mailboxRecord.value);
			if (messageBody != null) {
				save(domain.uid, cont.owner, messageBody.guid, messageBody, true);
				long total = bodyStat.body().incrementAndGet();
				if (total % 100 == 0) {
					contMon.log("sync {} body(ies) for {}", total, cont.owner);
				}
			} else {
				contMon.log("Failed to fetch body {} for {}", Level.WARN, mb, cont.owner);
			}
		}
	}
}
