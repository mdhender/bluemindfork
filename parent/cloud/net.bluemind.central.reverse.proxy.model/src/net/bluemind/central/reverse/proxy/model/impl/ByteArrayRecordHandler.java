package net.bluemind.central.reverse.proxy.model.impl;

import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.central.reverse.proxy.model.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.model.RecordHandler;
import net.bluemind.central.reverse.proxy.model.mapper.RecordKey;
import net.bluemind.central.reverse.proxy.model.mapper.RecordKeyMapper;
import net.bluemind.central.reverse.proxy.model.mapper.RecordValueMapper;

public class ByteArrayRecordHandler implements RecordHandler<byte[], byte[]> {

	private final Logger logger = LoggerFactory.getLogger(ByteArrayRecordHandler.class);

	private final ProxyInfoStoreClient storeClient;
	private final RecordKeyMapper<byte[]> keyMapper;
	private final RecordValueMapper<byte[]> valueMapper;

	public ByteArrayRecordHandler(ProxyInfoStoreClient storeClient, RecordKeyMapper<byte[]> keyMapper,
			RecordValueMapper<byte[]> valueMapper) {
		this.storeClient = storeClient;
		this.keyMapper = keyMapper;
		this.valueMapper = valueMapper;
	}

	@Override
	public void handle(ConsumerRecord<byte[], byte[]> rec) {
		keyMapper.map(rec.key()).flatMap(key -> store(rec, key))
				.ifPresent(storedKey -> logger.info("Storing {}:{}", storedKey.type, storedKey));
	}

	private Optional<RecordKey> store(ConsumerRecord<byte[], byte[]> rec, RecordKey key) {
		switch (key.type) {
		case "installation":
			return valueMapper.mapInstallation(rec.value()).map(storeClient::addInstallation).map(f -> key);
		case "domains":
			return valueMapper.mapDomain(rec.value()).map(storeClient::addDomain).map(f -> key);
		case "dir":
			String topicName = rec.topic();
			String domainUid = topicName.split("-")[1];
			return valueMapper.mapDir(domainUid, rec.value()).map(storeClient::addDir).map(f -> key);
		}
		return Optional.empty();
	}

}
