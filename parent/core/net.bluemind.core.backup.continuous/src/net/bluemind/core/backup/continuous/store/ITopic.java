package net.bluemind.core.backup.continuous.store;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;
import net.bluemind.core.backup.continuous.store.ITopicStore.TopicDescriptor;

public interface ITopic {

	TopicDescriptor topicDescriptor();

	CompletableFuture<Void> store(byte[] key, byte[] data);

	void delete(long id);

	IResumeToken parseToken(JsonObject js);

	IResumeToken subscribe(Handler<DataElement> de);

	IResumeToken subscribe(IResumeToken index, Handler<DataElement> de);

	IResumeToken subscribe(IResumeToken index, Handler<DataElement> de, Predicate<RecordKey> keyFilter);

}