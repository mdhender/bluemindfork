package net.bluemind.core.backup.continuous.restore.domains.replication;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.IndexedMessageBodyDTO;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreDomainType;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class RestoreMessageBodyESSource implements RestoreDomainType {

	private RestoreLogger log;

	public RestoreMessageBodyESSource(RestoreLogger log) {
		this.log = log;

	}

	private static final ValueReader<VersionnedItem<IndexedMessageBodyDTO>> reader = JsonUtils
			.reader(new TypeReference<VersionnedItem<IndexedMessageBodyDTO>>() {
			});

	@Override
	public String type() {
		return "message_bodies_es_source";
	}

	@Override
	public void restore(RecordKey key, String payload) {
		create(key, payload);
	}

	private void create(RecordKey key, String payload) {
		VersionnedItem<IndexedMessageBodyDTO> item = reader().read(payload);
		RecordIndexActivator.getIndexer().ifPresent(service -> {
			if (item.value.data != null) {
				service.storeBodyAsByte(item.uid, item.value.data);
				log.create(type(), key);
			}
		});
	}

	private ValueReader<VersionnedItem<IndexedMessageBodyDTO>> reader() {
		return reader;
	}
}
