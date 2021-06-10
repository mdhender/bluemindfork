package net.bluemind.core.backup.store.kafka;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.MoreObjects;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;

public class KafkaToken implements IResumeToken {
	Map<Integer, Long> partitionToOffset;

	public KafkaToken(Map<Integer, Long> partitionToOffset) {
		this.partitionToOffset = partitionToOffset;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(KafkaToken.class).add("part-offsets", partitionToOffset).toString();
	}

	@Override
	public JsonObject toJson() {
		JsonObject js = new JsonObject();
		for (Entry<Integer, Long> entry : partitionToOffset.entrySet()) {
			js.put(Integer.toString(entry.getKey()), entry.getValue());
		}
		return js;
	}
}
