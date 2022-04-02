package net.bluemind.core.backup.continuous.store;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;

public interface TopicSubscriber {

	String topicName();

	IResumeToken parseToken(JsonObject js);

	IResumeToken subscribe(RecordHandler handler);

	IResumeToken subscribe(IResumeToken index, RecordHandler handler);

	IResumeToken subscribe(IResumeToken index, RecordHandler handler, IRecordStarvationStrategy strat);

}