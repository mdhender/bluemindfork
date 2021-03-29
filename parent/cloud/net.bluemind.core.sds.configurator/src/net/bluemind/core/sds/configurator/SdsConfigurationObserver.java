package net.bluemind.core.sds.configurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class SdsConfigurationObserver implements ISystemConfigurationObserver {

	private Logger logger = LoggerFactory.getLogger(SdsConfigurationObserver.class);

	public SdsConfigurationObserver() {
	}

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf current) throws ServerFault {
		if (!current.isArchiveKindSds()) {
			return;
		}

		JsonObject json = new JsonObject()//
				.put("storeType", current.stringValue(SysConfKeys.archive_kind.name()))//
				.put("endpoint", current.stringValue(SysConfKeys.sds_s3_endpoint.name()))//
				.put("accessKey", current.stringValue(SysConfKeys.sds_s3_access_key.name()))//
				.put("secretKey", current.stringValue(SysConfKeys.sds_s3_secret_key.name()))//
				.put("region", current.stringValue(SysConfKeys.sds_s3_region.name()))//
				.put("bucket", current.stringValue(SysConfKeys.sds_s3_bucket.name()));

		Topology.get().nodes().stream().filter(iv -> iv.value.tags.contains("mail/imap")).forEach(iv -> {
			JsonObject configBackend = new JsonObject().put("backend", iv.value.address()).put("config", json);
			logger.info("reconfigure SDS {}", configBackend);
			VertxPlatform.eventBus().send("sds.sysconf.changed", configBackend);
		});
	}

}
