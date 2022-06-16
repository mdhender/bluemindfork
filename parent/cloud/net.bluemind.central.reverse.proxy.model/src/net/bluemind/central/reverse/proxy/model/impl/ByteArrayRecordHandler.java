package net.bluemind.central.reverse.proxy.model.impl;

import java.util.Objects;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress;
import net.bluemind.central.reverse.proxy.model.RecordHandler;
import net.bluemind.central.reverse.proxy.model.client.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKey;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKeyMapper;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordValueMapper;

public class ByteArrayRecordHandler implements RecordHandler<byte[], byte[]> {

	private final Logger logger = LoggerFactory.getLogger(ByteArrayRecordHandler.class);

	private final Vertx vertx;
	private final ProxyInfoStoreClient storeClient;
	private final RecordKeyMapper<byte[]> keyMapper;
	private final RecordValueMapper<byte[]> valueMapper;

	public ByteArrayRecordHandler(Vertx vertx, ProxyInfoStoreClient storeClient, RecordKeyMapper<byte[]> keyMapper,
			RecordValueMapper<byte[]> valueMapper) {
		this.vertx = vertx;
		this.storeClient = storeClient;
		this.keyMapper = keyMapper;
		this.valueMapper = valueMapper;
	}

	@Override
	public void handle(ConsumerRecord<byte[], byte[]> rec) {
		keyMapper.map(rec.key()).flatMap(key -> store(rec, key))
				.ifPresent(futureStoredKey -> futureStoredKey
						.onSuccess(storedKey -> logger.info("[model] Stored {}:{}", storedKey.type, storedKey))
						.onFailure(t -> logger.error("[model] Failed to store: {}", keyMapper.map(rec.key()), t)));
	}

	private Optional<Future<RecordKey>> store(ConsumerRecord<byte[], byte[]> rec, RecordKey key) {
		switch (key.type) {
		case "installation":
			return valueMapper.mapInstallation(rec.value())
					.map(installation -> storeClient.addInstallation(installation).map(oldIp -> {
						publishInstallationIpChange(installation, oldIp);
						return key;
					}));
		case "domains":
			return valueMapper.mapDomain(rec.value()).map(domain -> storeClient.addDomain(domain).map(v -> key));
		case "dir":
			String topicName = rec.topic();
			String domainUid = topicName.split("-")[1];
			return valueMapper.mapDir(domainUid, rec.value()).map(dir -> storeClient.addDir(dir).map(v -> key));
		default:
			logger.info("[model] Skipping unknown type {}", key);
		}
		return Optional.empty();
	}

	private void publishInstallationIpChange(InstallationInfo installation, String oldIp) {
		if (!installation.ip.equals(oldIp) && Objects.nonNull(oldIp)) {
			logger.info("[model] Announcing installation ip change for {}: {} -> {}", installation.dataLocation, oldIp,
					installation.ip);
			JsonObject ip = new JsonObject().put("ip", oldIp);
			vertx.eventBus().publish(ProxyEventBusAddress.ADDRESS, ip, ProxyEventBusAddress.INSTALLATION_IP_CHANGE);
		}
	}

}
