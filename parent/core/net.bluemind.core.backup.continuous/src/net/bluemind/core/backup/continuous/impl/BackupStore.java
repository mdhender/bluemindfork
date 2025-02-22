package net.bluemind.core.backup.continuous.impl;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.RecordKey.Operation;
import net.bluemind.core.backup.continuous.TopicSerializer;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.store.ITopicStore.TopicDescriptor;
import net.bluemind.core.backup.continuous.store.TopicPublisher;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.ReservedIds;

public class BackupStore<T> implements IBackupStore<T> {

	private static final Logger logger = LoggerFactory.getLogger(BackupStore.class);

	private final TopicPublisher publisher;
	private final TopicDescriptor descriptor;
	private final TopicSerializer<RecordKey, ItemValue<T>> serializer;

	public BackupStore(TopicPublisher publisher, TopicDescriptor descriptor,
			TopicSerializer<RecordKey, ItemValue<T>> serializer) {
		this.publisher = publisher;
		this.descriptor = descriptor;
		this.serializer = serializer;
	}

	@Override
	public CompletableFuture<Void> storeRaw(String partitionKey, byte[] key, byte[] raw) {
		return publisher.store(partitionKey, key, raw);
	}

	@Override
	public CompletableFuture<Void> store(ItemValue<T> data) {
		return store(data, null);
	}

	@Override
	public CompletableFuture<Void> store(ItemValue<T> data, ReservedIds reservedIds) {
		RecordKey key = RecordKey.forItemValue(descriptor, data, false);
		byte[] serializedKey = serializer.key(key);
		byte[] serializedItem = serializer.value(data, reservedIds);
		String partitionKey = descriptor.partitionKey(data.uid);
		return storeRaw(partitionKey, serializedKey, serializedItem).whenComplete((v, ex) -> {
			if (ex != null) {
				logger.warn("Failed to store {} to {}: {}", key.id, publisher, ex.getMessage());
			} else if (logger.isDebugEnabled()) {
				logger.debug("Stored id {} to {}", key.id, publisher);
			}
		});
	}

	@Override
	public CompletableFuture<Void> delete(ItemValue<T> data) {
		RecordKey key = RecordKey.forItemValue(descriptor, data, true);
		byte[] serializedKey = serializer.key(key);
		byte[] serializedItem = ("{\"uid\":\"" + data.uid + "\"}").getBytes();
		String partitionKey = descriptor.partitionKey(data.uid);

		RecordKey invalidateCreates = RecordKey.forItemValue(descriptor, data, true);
		invalidateCreates.operation = Operation.CREATE.name();
		byte[] compactCreateKey = serializer.key(invalidateCreates);

		RecordKey invalidateUpdates = RecordKey.forItemValue(descriptor, data, true);
		invalidateUpdates.operation = Operation.UPDATE.name();
		byte[] compactUpdateKey = serializer.key(invalidateUpdates);

		CompletableFuture<Void> delProm = storeRaw(partitionKey, serializedKey, serializedItem);
		CompletableFuture<Void> updProm = storeRaw(partitionKey, compactUpdateKey, null);
		CompletableFuture<Void> creProm = storeRaw(partitionKey, compactCreateKey, null);

		CompletableFuture<Void> allRecs = CompletableFuture.allOf(delProm, updProm, creProm);
		return allRecs.whenComplete((v, ex) -> {
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
