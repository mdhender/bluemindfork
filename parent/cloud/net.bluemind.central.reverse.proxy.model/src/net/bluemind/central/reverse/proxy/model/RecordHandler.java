package net.bluemind.central.reverse.proxy.model;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import net.bluemind.central.reverse.proxy.model.client.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKeyMapper;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordValueMapper;
import net.bluemind.central.reverse.proxy.model.impl.ByteArrayRecordHandler;

public interface RecordHandler<T, U> extends Handler<ConsumerRecord<T, U>> {

	@Override
	void handle(ConsumerRecord<T, U> rec);

	public static RecordHandler<byte[], byte[]> createByteHandler(ProxyInfoStoreClient client, Vertx vertx) {
		return new ByteArrayRecordHandler(vertx, client, RecordKeyMapper.byteArray(), RecordValueMapper.byteArray());
	}

}
