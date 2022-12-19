package net.bluemind.core.backup.continuous.events;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.hook.IMessageBodyHook;
import net.bluemind.backend.mail.replica.indexing.IndexedMessageBody;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.core.api.Stream;
import net.bluemind.core.backup.continuous.dto.IndexedMessageBodyDTO;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.utils.InputReadStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.mailbox.api.IMailboxes;

public class MessageBodyESSourceHook implements IMessageBodyHook, ContinuousContenairization<IndexedMessageBodyDTO> {

	@Override
	public String type() {
		return "message_bodies_es_source";
	}

	@Override
	public void preCreate(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailboxes mailboxesApi = prov.instance(IMailboxes.class, domainUid);
		var mailbox = mailboxesApi.getComplete(ownerId);

		Map<String, Object> map = new HashMap<>();
		try {
			IndexedMessageBody body = getIndexedMessageBody(mailboxRecord, mailbox.value.dataLocation);
			map = mapcreateMapFromIndexedMessageBody(body);
			IndexedMessageBodyDTO dto = new IndexedMessageBodyDTO(map);
			save(domainUid, ownerId, body.uid, dto, true);
		} catch (Exception e) {
			logger.warn("Cannot resync pending data", e);
		}
	}

	@Override
	public void preUpdate(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailboxes mailboxesApi = prov.instance(IMailboxes.class, domainUid);
		var mailbox = mailboxesApi.getComplete(ownerId);

		Map<String, Object> map = new HashMap<>();
		try {
			IndexedMessageBody body = getIndexedMessageBody(mailboxRecord, mailbox.value.dataLocation);
			map = mapcreateMapFromIndexedMessageBody(body);
			IndexedMessageBodyDTO dto = new IndexedMessageBodyDTO(map);
			save(domainUid, ownerId, body.uid, dto, false);
		} catch (Exception e) {
			logger.warn("Cannot resync pending data", e);
		}

	}

	private IndexedMessageBody getIndexedMessageBody(MailboxRecord mailboxRecord, String dataLocation)
			throws Exception {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		String guid = mailboxRecord.messageBody;
		InputStream bodyinputStream = null;
		MessageBodyObjectStore sds = new MessageBodyObjectStore(prov.getContext(), dataLocation);
		Path bodyPath = sds.open(mailboxRecord.messageBody);
		if (bodyPath != null) {
			bodyinputStream = Files.newInputStream(bodyPath); // NOSONAR
			Files.delete(bodyPath);
		}
		InputReadStream streamAdapter = new InputReadStream(bodyinputStream);
		Stream stream = VertxStream.stream(streamAdapter);
		return IndexedMessageBody.createIndexBody(guid, stream);
	}

	private Map<String, Object> mapcreateMapFromIndexedMessageBody(IndexedMessageBody body) {

		Map<String, Object> content = new HashMap<>();
		content.put("content", body.content);
		content.put("messageId", body.messageId.toString());
		content.put("references", body.references.stream().map(Object::toString).toList());
		content.put("preview", body.preview);
		content.put("subject", body.subject.toString());
		content.put("subject_kw", body.subject.toString());
		content.put("headers", body.headers());
		content.putAll(body.data);
		return content;
	}
}
