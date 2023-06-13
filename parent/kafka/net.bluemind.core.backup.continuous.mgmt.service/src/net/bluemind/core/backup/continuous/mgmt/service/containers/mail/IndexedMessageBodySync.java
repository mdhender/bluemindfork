package net.bluemind.core.backup.continuous.mgmt.service.containers.mail;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.GetResponse;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.dto.IndexedMessageBodyDTO;
import net.bluemind.core.backup.continuous.events.ContinuousContenairization;
import net.bluemind.core.backup.continuous.mgmt.service.containers.mail.RecordsSync.BodyStat;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public class IndexedMessageBodySync implements ContinuousContenairization<IndexedMessageBodyDTO> {

	private static final String INDEXED_MESSAGE_BODIES = "message_bodies_es_source";
	private static final String INDEX_PENDING_READ_ALIAS = "mailspool_pending_read_alias";
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private IBackupStoreFactory target;
	private ItemValue<Domain> domain;
	private BaseContainerDescriptor cont;
	private IServerTaskMonitor contMon;

	public IndexedMessageBodySync(IBackupStoreFactory target, IServerTaskMonitor contMon, ItemValue<Domain> domain,
			BaseContainerDescriptor cont) {
		this.target = target;
		this.domain = domain;
		this.cont = cont;
		this.contMon = contMon;
	}

	@Override
	public String type() {
		return INDEXED_MESSAGE_BODIES;
	}

	@Override
	public IBackupStoreFactory targetStore() {
		return target;
	}

	public void storeIndexedMessageBodies(BodyStat bodyStat, MailboxRecord mailboxRecord, MessageBody body) {
		String messageBodyId = mailboxRecord.messageBody;
		final ElasticsearchClient esClient = ESearchActivator.getClient();
		try {
			GetResponse<ObjectNode> response = esClient.get(g -> g.index(INDEX_PENDING_READ_ALIAS).id(messageBodyId),
					ObjectNode.class);
			if (response.found()) {
				byte[] bytes = objectMapper.writeValueAsBytes(response.source());
				IndexedMessageBodyDTO indexedMessageBody = new IndexedMessageBodyDTO(bytes);
				save(domain.uid, cont.owner, messageBodyId, indexedMessageBody, true);
				long total = bodyStat.esSource().incrementAndGet();
				if (total % 100 == 0) {
					contMon.log("sync {} item(s) es source for {}", total, cont.owner);
				}
			} else if (body != null) {
				// fallback to building from MessageBody
			}
		} catch (ElasticsearchException | IOException e1) {
			// fallback to building from MessageBody
		}
	}
}
