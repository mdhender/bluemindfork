package net.bluemind.core.auditlogs.client.kafka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.AuditLogQuery;
import net.bluemind.core.auditlogs.IItemChangeLogClient;
import net.bluemind.core.backup.store.kafka.KafkaTopicStore;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.ItemChangeLogEntry;
import net.bluemind.core.container.model.ItemChangelog;

public class KafkaAuditLogItemChangeLog implements IItemChangeLogClient {

	private AuditLogDeserializer deserializer;
	private String bootstrap;

	public KafkaAuditLogItemChangeLog(String bootstrap) {
		deserializer = new AuditLogDeserializer();
		this.bootstrap = bootstrap;
	}

	@Override
	public ItemChangelog getItemChangeLog(String domainUid, String containerUid, String itemUid, Long since) {
		List<AuditLogEntry> auditLogEntries = new ArrayList<>();
		KafkaAuditLogConsumer kafkaAuditLogConsumer = new KafkaAuditLogConsumer(bootstrap);

		int containerPartition = Math.abs(containerUid.hashCode() % KafkaTopicStore.PARTITION_COUNT);

		kafkaAuditLogConsumer.consume(domainUid, containerUid, (de) -> {
			if (de.part() == containerPartition && de.key().containerUid().equals(containerUid)) {
				auditLogEntries.add(deserializer.value(de.payload()));
			}
		});

		return buildResponse(auditLogEntries);
	}

	@Override
	public List<AuditLogEntry> queryAuditLog(AuditLogQuery query) {
		return List.of();
	}

	private ItemChangelog buildResponse(List<AuditLogEntry> entries) {
		ItemChangelog changelog = new ItemChangelog();

		changelog.entries = entries.stream().map(h -> {
			AuditLogEntry auditLogEntry = h;
			ItemChangeLogEntry entry = new ItemChangeLogEntry();

			entry.date = auditLogEntry.timestamp;

			if (auditLogEntry.item != null) {
				entry.version = auditLogEntry.item.version();
				entry.internalId = auditLogEntry.item.id();
				entry.itemUid = auditLogEntry.item.uid();
				entry.itemExtId = null;
			}
			if (auditLogEntry.securityContext != null) {
				entry.author = auditLogEntry.securityContext.displayName();
				entry.origin = auditLogEntry.securityContext.origin();
			}

			String type = auditLogEntry.action;
			Arrays.asList(Type.values()).stream().filter(t -> t.name().equals(type)).findFirst()
					.ifPresent(t -> entry.type = t);
			return entry;
		}).toList();
		return changelog;
	}

}
