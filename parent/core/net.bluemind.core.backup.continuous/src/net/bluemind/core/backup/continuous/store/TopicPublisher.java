package net.bluemind.core.backup.continuous.store;

import java.util.concurrent.CompletableFuture;

public interface TopicPublisher {

	CompletableFuture<Void> store(String partitionKey, byte[] key, byte[] data);

}