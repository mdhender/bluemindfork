package net.bluemind.core.backup.continuous.impl;

import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.TopicDeserializer;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;
import net.bluemind.core.backup.continuous.store.TopicNames;
import net.bluemind.core.backup.continuous.store.TopicSubscriber;

public class LiveStream implements ILiveStream {

	private static final Logger logger = LoggerFactory.getLogger(LiveStream.class);

	private final String installationid;
	private final String domainUid;
	private final TopicSubscriber subscriber;
	private final TopicDeserializer<RecordKey, VersionnedItem<?>> deserializer;

	public LiveStream(String installationid, String domainUid, TopicSubscriber subscriber,
			TopicDeserializer<RecordKey, VersionnedItem<?>> deserializer) {
		this.installationid = installationid;
		this.domainUid = domainUid;
		this.subscriber = subscriber;
		this.deserializer = deserializer;
	}

	@Override
	public String fullName() {
		return TopicNames.build(installationid, domainUid);
	}

	@Override
	public IResumeToken subscribe(IResumeToken startOffset, Handler<DataElement> handler) {
		return subscriber.subscribe(startOffset, deserialize(handler));
	}

	@Override
	public IResumeToken subscribe(Handler<DataElement> handler) {
		return subscriber.subscribe(deserialize(handler));
	}

	@Override
	public IResumeToken subscribe(IResumeToken startOffset, Handler<DataElement> handler,
			IRecordStarvationStrategy onStarve) {
		return subscriber.subscribe(startOffset, deserialize(handler), onStarve);
	}

	private BiConsumer<byte[], byte[]> deserialize(Handler<DataElement> handler) {
		return (keyBytes, valueBytes) -> {
			DataElement de = new DataElement();
			RecordKey key = deserializer.key(keyBytes);
			de.key = key;
			de.payload = valueBytes;
//			de.value = deserializer.value(key, valueBytes);
			if (de.key.id == 0) {
				// silent skip
			} else if (de.payload == null) {
				logger.warn("null payload for {} in {}:{}", de.key.id, keyBytes, valueBytes);
			} else {
				handler.handle(de);
			}
		};
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(LiveStream.class)//
				.add("iid", installationid)//
				.add("dom", domainUid)//
				.add("sub", subscriber).toString();
	}

	@Override
	public IResumeToken parse(JsonObject js) {
		return subscriber.parseToken(js);
	}

	@Override
	public String installationId() {
		return installationid;
	}

	@Override
	public String domainUid() {
		return domainUid;
	}
}
