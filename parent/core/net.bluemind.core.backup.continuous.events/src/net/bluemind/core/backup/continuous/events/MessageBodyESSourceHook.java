package net.bluemind.core.backup.continuous.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.hook.IMessageBodyHook;
import net.bluemind.backend.mail.replica.indexing.IndexedMessageBody;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.core.api.Stream;
import net.bluemind.core.backup.continuous.dto.IndexedMessageBodyDTO;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.mailbox.api.IMailboxes;

public class MessageBodyESSourceHook implements IMessageBodyHook, ContinuousContenairization<IndexedMessageBodyDTO> {

	@Override
	public String type() {
		return "message_bodies_es_source";
	}

	private static final Logger timingLogger = LoggerFactory.getLogger(MessageBodyESSourceHook.class);

	@Override
	public void preCreate(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		saveToStore(domainUid, ownerId, mailboxRecord, true);
	}

	@Override
	public void preUpdate(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		saveToStore(domainUid, ownerId, mailboxRecord, false);
	}

	private void saveToStore(String domainUid, String ownerId, MailboxRecord mailboxRecord, boolean create) {
		if (targetStore().isPaused()) {
			return;
		}
		long time = System.currentTimeMillis();
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailboxes mailboxesApi = prov.instance(IMailboxes.class, domainUid);
		var mailbox = mailboxesApi.getComplete(ownerId);

		try {
			IndexedMessageBody body = getIndexedMessageBody(mailboxRecord, mailbox.value.dataLocation);
			IndexedMessageBodyDTO dto = new IndexedMessageBodyDTO(body.asElasticSource());
			save(domainUid, ownerId, body.uid, dto, create);
			time = System.currentTimeMillis() - time;
			timingLogger.info("[{}@{}] body {} took {}ms.", mailbox.value.name, domainUid, mailboxRecord.messageBody,
					time);
		} catch (Exception e) {
			logger.warn("Cannot resync pending data", e);
		}
	}

	private IndexedMessageBody getIndexedMessageBody(MailboxRecord mailboxRecord, String dataLocation)
			throws Exception {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		String guid = mailboxRecord.messageBody;
		MessageBodyObjectStore sds = new MessageBodyObjectStore(prov.getContext(), dataLocation);
		ByteBuf mmapedBody = sds.openMmap(mailboxRecord.messageBody);
		Stream stream = VertxStream.stream(Buffer.buffer(mmapedBody));
		return IndexedMessageBody.createIndexBody(guid, stream);
	}

}
