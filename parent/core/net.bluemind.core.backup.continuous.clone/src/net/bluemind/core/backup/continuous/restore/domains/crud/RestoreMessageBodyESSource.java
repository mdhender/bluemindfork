package net.bluemind.core.backup.continuous.restore.domains.crud;

import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreDomainType;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class RestoreMessageBodyESSource implements RestoreDomainType {

	private static final Logger logger = LoggerFactory.getLogger(RestoreMessageBodyESSource.class);

	private RestoreLogger log;

	public RestoreMessageBodyESSource(RestoreLogger log) {
		this.log = log;

	}

	private static final ValueReader<VersionnedItem<Map<String, Object>>> reader = JsonUtils
			.reader(new TypeReference<VersionnedItem<Map<String, Object>>>() {
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
		VersionnedItem<Map<String, Object>> item = reader().read(payload);
		RecordIndexActivator.getIndexer().ifPresent(service -> {
			if (item.value.containsKey("data")) {
				byte[] dataAsBytes = Base64.getDecoder().decode((String) item.value.get("data"));
				service.storeBodyAsByte(item.uid, dataAsBytes);
			}
		});
	}

	private ValueReader<VersionnedItem<Map<String, Object>>> reader() {
		return reader;
	}
}
