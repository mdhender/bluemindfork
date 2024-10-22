package net.bluemind.core.backup.continuous.events;

import java.util.concurrent.atomic.LongAdder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.RateLimiter;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.hook.IMessageBodyHook;
import net.bluemind.backend.mail.replica.indexing.IElasticSourceHolder;
import net.bluemind.backend.mail.replica.indexing.IndexedMessageBody;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.core.api.Stream;
import net.bluemind.core.backup.continuous.dto.IndexedMessageBodyDTO;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.index.mail.IndexableMessageBodyCache;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public class MessageBodyESSourceHook implements IMessageBodyHook, ContinuousContenairization<IndexedMessageBodyDTO> {

	private static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public String type() {
		return "message_bodies_es_source";
	}

	private final LongAdder cacheStrategy = new LongAdder();
	private final LongAdder esStrategy = new LongAdder();
	private final LongAdder slowStrategy = new LongAdder();
	private final RateLimiter warnLimit = RateLimiter.create(1.0 / 2);

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

		try {
			byte[] body = getIndexedMessageBody(mailboxRecord, domainUid, ownerId);
			IndexedMessageBodyDTO dto = new IndexedMessageBodyDTO(body);
			save(domainUid, ownerId, mailboxRecord.messageBody, dto, create);
		} catch (Exception e) {
			logger.warn("Cannot resync pending data", e);
		}
	}

	private byte[] getIndexedMessageBody(MailboxRecord mailboxRecord, String domainUid, String ownerId)
			throws Exception {

		// cache strategy
		IElasticSourceHolder fromCache = IndexableMessageBodyCache.sourceHolder.getIfPresent(mailboxRecord.messageBody);
		if (fromCache != null) {
			cacheStrategy.add(1);
			return fromCache.asElasticSource();
		}

		ElasticsearchClient esClient = ESearchActivator.getClient();
		GetResponse<ObjectNode> response = esClient
				.get(g -> g.index("mailspool_pending_read_alias").id(mailboxRecord.messageBody), ObjectNode.class);
		if (response.found()) {
			esStrategy.add(1);
			return mapper.writeValueAsBytes(response.source());
		}

		// slow strategy
		slowStrategy.add(1);
		if (warnLimit.tryAcquire()) {
			logger.warn("Slow ES push to kafka triggered on {}, strategies used are fast: {}, es: {}, slow: {}",
					mailboxRecord.messageBody, cacheStrategy.sum(), esStrategy.sum(), slowStrategy.sum());
		}

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDirectory dirApi = prov.instance(IDirectory.class, domainUid);
		DirEntry mailbox = dirApi.findByEntryUid(ownerId);
		MessageBodyObjectStore sds = new MessageBodyObjectStore(prov.getContext(), mailbox.dataLocation);
		ByteBuf mmapedBody = sds.openMmap(mailboxRecord.messageBody);
		Stream stream = VertxStream.stream(Buffer.buffer(mmapedBody));
		return IndexedMessageBody.createIndexBody(mailboxRecord.messageBody, stream).asElasticSource();
	}

}
