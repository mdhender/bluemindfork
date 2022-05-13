package net.bluemind.central.reverse.proxy.model;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import io.vertx.core.Handler;
import net.bluemind.central.reverse.proxy.model.impl.ByteArrayRecordHandler;
import net.bluemind.central.reverse.proxy.model.mapper.impl.ByteArrayRecordKeyMapper;
import net.bluemind.central.reverse.proxy.model.mapper.impl.ByteArrayRecordValueMapper;

public interface RecordHandler<T, U> extends Handler<ConsumerRecord<T, U>> {

	void handle(ConsumerRecord<T, U> rec);

	public static class Mapper {
		public static final ObjectMapper mapper = create();

		private static ObjectMapper create() {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.registerModule(new AfterburnerModule().setUseValueClassLoader(false));
			return objectMapper;
		}

	}
	
	public static RecordHandler<byte[], byte[]> createByteHandler(ProxyInfoStoreClient client) {
		return new ByteArrayRecordHandler(client, new ByteArrayRecordKeyMapper(Mapper.mapper), new ByteArrayRecordValueMapper());
	}

}
