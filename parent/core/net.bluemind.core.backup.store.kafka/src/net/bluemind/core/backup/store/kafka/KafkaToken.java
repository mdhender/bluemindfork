package net.bluemind.core.backup.store.kafka;

import com.google.common.base.MoreObjects;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;

public class KafkaToken implements IResumeToken {
	public String groupId;
	public int workers;

	public KafkaToken(String groupId, int workers) {
		this.groupId = groupId;
		this.workers = workers;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(KafkaToken.class).add("group", groupId).add("workers", workers).toString();
	}

	@Override
	public JsonObject toJson() {
		JsonObject js = new JsonObject();
		js.put("group", groupId).put("workers", workers);
		return js;
	}
}
