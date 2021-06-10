package net.bluemind.core.backup.continuous.store;

import java.util.function.BiConsumer;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;

public interface TopicSubscriber {

	String topicName();

	IResumeToken parseToken(JsonObject js);

	IResumeToken subscribe(BiConsumer<byte[], byte[]> handler);

	IResumeToken subscribe(IResumeToken index, BiConsumer<byte[], byte[]> handler);

}