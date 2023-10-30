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
import net.bluemind.central.reverse.proxy.model.client.PostfixMapsStoreClient;
import net.bluemind.central.reverse.proxy.model.client.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.model.common.DomainSettings;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKey;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKeyMapper;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordValueMapper;

public class ByteArrayRecordHandler implements RecordHandler<byte[], byte[]> {
	private final Logger logger = LoggerFactory.getLogger(ByteArrayRecordHandler.class);

	private final Vertx vertx;
	private final ProxyInfoStoreClient proxyInfoStoreClient;
	private final PostfixMapsStoreClient postfixMapsStoreClient;
	private final RecordKeyMapper<byte[]> keyMapper;
	private final RecordValueMapper<byte[]> valueMapper;

	public ByteArrayRecordHandler(Vertx vertx, ProxyInfoStoreClient proxyInfoStoreClient,
			PostfixMapsStoreClient postfixMapsStoreClient, RecordKeyMapper<byte[]> keyMapper,
			RecordValueMapper<byte[]> valueMapper) {
		this.vertx = vertx;
		this.proxyInfoStoreClient = proxyInfoStoreClient;
		this.postfixMapsStoreClient = postfixMapsStoreClient;
		this.keyMapper = keyMapper;
		this.valueMapper = valueMapper;
	}

	@Override
	public void handle(ConsumerRecord<byte[], byte[]> rec) {
		keyMapper.map(rec.key()).flatMap(key -> safeStore(rec, key))
				.ifPresent(futureStoredKey -> futureStoredKey
						.onSuccess(storedKey -> logger.info("[model] Stored {}:{}", storedKey.type, storedKey))
						.onFailure(t -> logger.error("[model] Failed to store: {}", keyMapper.map(rec.key()), t)));
	}

	public Optional<Future<RecordKey>> safeStore(ConsumerRecord<byte[], byte[]> rec, RecordKey key) {
		try {
			return store(rec, key);
		} catch (RuntimeException r) {
			return Optional.of(Future.failedFuture(r.getMessage()));
		}
	}

	private Optional<Future<RecordKey>> store(ConsumerRecord<byte[], byte[]> rec, RecordKey key) {
		switch (key.type) {
		case "installation":
			return valueMapper.mapInstallation(rec.value())
					.map(installation -> Future.all(proxyInfoStoreClient.addInstallation(installation).map(oldIp -> {
						if (Objects.nonNull(oldIp)) {
							publishInstallationIpChange(installation, oldIp);
						}

						return key;
					}), postfixMapsStoreClient.addInstallation(installation)).map(v -> key));

		case "domains":
			byte[] val = rec.value();
			if ("net.bluemind.domain.api.Domain".equals(key.valueClass)) {
				return valueMapper.mapDomain(val)
						.map(domain -> Future
								.all(proxyInfoStoreClient.addDomain(domain), postfixMapsStoreClient.addDomain(domain))
								.map(v -> key));
			}

			if ("net.bluemind.domain.api.DomainSettings".equals(key.valueClass)) {
				logger.info("DDomainSettings: {} {}", key, new String(val));
				Optional<DomainSettings> set = valueMapper.mapDomainSettings(val);
				logger.info("Convert: {}", set);
				return set
						.map(domainSettings -> postfixMapsStoreClient.addDomainSettings(domainSettings).map(v -> key));
			}

			return Optional.empty();

		case "dir":
			if (!"net.bluemind.directory.service.DirEntryAndValue".equals(key.valueClass)) {
				logger.debug("Unsupported dir entry value class {}", key.valueClass);
				return Optional.empty();
			}

			if (key.operation != null && key.operation.equals("DELETE")) {
				return valueMapper.getValueUid(rec.value())
						.map(deletedUid -> postfixMapsStoreClient.removeDir(deletedUid).map(v -> key));
			}

			String domainUid = key.uid;
			return valueMapper.mapDir(domainUid, rec.value()).map(dir -> Future
					.all(proxyInfoStoreClient.addDir(dir), postfixMapsStoreClient.addDir(dir)).map(v -> key));

		case "memberships":
			return valueMapper.mapMemberShips(rec.value())
					.map(member -> postfixMapsStoreClient.manageMember(member).map(v -> key));

		default:
			logger.debug("[model] Skipping unknown type {}", key);
		}

		return Optional.empty();
	}

	private void publishInstallationIpChange(InstallationInfo installation, String oldIp) {
		if (!installation.ip.equals(oldIp) && Objects.nonNull(oldIp)) {
			logger.info("[model] Announcing installation ip change for {}: {} -> {}", installation.dataLocationUid,
					oldIp, installation.ip);
			JsonObject ip = new JsonObject().put("ip", oldIp);
			vertx.eventBus().publish(ProxyEventBusAddress.ADDRESS, ip, ProxyEventBusAddress.INSTALLATION_IP_CHANGE);
		}
	}

}
