package net.bluemind.core.backup.continuous.mgmt.service.containers.mail;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

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

	public void storeIndexedMessageBodies(IServerTaskMonitor entryMon, BodyStat bodyStat, MailboxRecord mailboxRecord) {
		String messageBodyId = mailboxRecord.messageBody;
		final Client client = ESearchActivator.getClient();
		QueryBuilder matchSpecificFieldQuery = QueryBuilders.multiMatchQuery(messageBodyId, "_id");
		SearchResponse r = client.prepareSearch(INDEX_PENDING_READ_ALIAS).setQuery(matchSpecificFieldQuery)
				.setFetchSource(true).execute().actionGet();
		if (r.getHits().getTotalHits().value == 1L) {
			SearchHit searchHit = r.getHits().getAt(0);
			IndexedMessageBodyDTO indexedMessageBody = new IndexedMessageBodyDTO(searchHit.getSourceRef().array());
			save(domain.uid, cont.owner, messageBodyId, indexedMessageBody, true);
			long total = bodyStat.esSource().incrementAndGet();
			if (total % 100 == 0) {
				contMon.log("sync {} item(s) es source for {}", total, cont.owner);
			}
		}
	}
}
