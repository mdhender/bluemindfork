package net.bluemind.core.backup.continuous.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import net.bluemind.core.backup.continuous.IBackupStore;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.TopicSerializer;
import net.bluemind.core.backup.continuous.store.ITopicStore.TopicDescriptor;
import net.bluemind.core.backup.continuous.store.TopicPublisher;
import net.bluemind.core.container.model.ItemValue;

public class BackupStore<T> implements IBackupStore<T> {

	private static final Logger logger = LoggerFactory.getLogger(BackupStore.class);

	private final TopicPublisher publisher;
	private final TopicDescriptor descriptor;
	private final TopicSerializer<RecordKey, ItemValue<T>> serializer;
	private final Map<byte[], byte[]> waitingRecords;

	public BackupStore(TopicPublisher publisher, TopicDescriptor descriptor,
			TopicSerializer<RecordKey, ItemValue<T>> serializer, Map<byte[], byte[]> waitingRecords) {
		this.publisher = publisher;
		this.descriptor = descriptor;
		this.serializer = serializer;
		this.waitingRecords = waitingRecords;
	}

	@Override
	public CompletableFuture<Void> storeRaw(byte[] key, byte[] raw) {
		String partitionKey = descriptor.partitionKey();
		waitingRecords.put(key, raw);
		return publisher.store(partitionKey, key, raw).whenComplete((v, t) -> {
			if (t == null) {
				waitingRecords.remove(key);
			}
		});
	}

	@Override
	public void store(ItemValue<T> data) {
		RecordKey key = RecordKey.forItemValue(descriptor, data);
		byte[] serializedKey = serializer.key(key);
		byte[] serializedItem = serializer.value(data);
		storeRaw(serializedKey, serializedItem).whenComplete((v, ex) -> {
			if (ex != null) {
				logger.warn("Failed to store {} to {}: {}", key.id, publisher, ex.getMessage());
			} else if (logger.isDebugEnabled()) {
				logger.debug("Stored id {} to {}", key.id, publisher);
			}
		});
	}

	@Override
	public void delete(ItemValue<T> data) {
		RecordKey key = RecordKey.forItemValue(descriptor, data);
		byte[] serializedKey = serializer.key(key);
		byte[] serializedItem = "".getBytes();
		storeRaw(serializedKey, serializedItem).whenComplete((v, ex) -> {
			if (ex != null) {
				logger.warn("Failed to store delete operation {} to {}: {}", key.id, publisher, ex.getMessage());
			} else if (logger.isDebugEnabled()) {
				logger.debug("Stored id {} to {}", key.id, publisher);
			}
		});
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(IBackupStore.class).add("topic", publisher).toString();
	}
}
