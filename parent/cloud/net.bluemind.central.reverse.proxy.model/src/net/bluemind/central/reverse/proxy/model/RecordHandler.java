package net.bluemind.central.reverse.proxy.model;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.vertx.core.Handler;
import net.bluemind.central.reverse.proxy.model.impl.ByteArrayRecordHandler;
import net.bluemind.central.reverse.proxy.model.mapper.impl.ByteArrayRecordKeyMapper;
import net.bluemind.central.reverse.proxy.model.mapper.impl.ByteArrayRecordValueMapper;

public interface RecordHandler<T, U> extends Handler<ConsumerRecord<T, U>> {

	void handle(ConsumerRecord<T, U> rec);

	public static RecordHandler<byte[], byte[]> createByteHandler(ProxyInfoStoreClient client) {
		return new ByteArrayRecordHandler(client, new ByteArrayRecordKeyMapper(), new ByteArrayRecordValueMapper());
	}

}
