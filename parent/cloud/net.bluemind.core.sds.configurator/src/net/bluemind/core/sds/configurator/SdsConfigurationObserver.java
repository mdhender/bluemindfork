package net.bluemind.core.sds.configurator;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.network.topology.Topology;
import net.bluemind.sds.proxy.mgmt.SdsProxyManager;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.helper.ArchiveHelper;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class SdsConfigurationObserver implements ISystemConfigurationObserver {

	private Logger logger = LoggerFactory.getLogger(SdsConfigurationObserver.class);

	public SdsConfigurationObserver() {
	}

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf current) throws ServerFault {
		if (!ArchiveHelper.isSdsArchiveKind(current)) {
			return;
		}

		JsonObject json = new JsonObject()//
				.put("storeType", current.stringValue(SysConfKeys.archive_kind.name()))//
				.put("endpoint", current.stringValue(SysConfKeys.sds_s3_endpoint.name()))//
				.put("accessKey", current.stringValue(SysConfKeys.sds_s3_access_key.name()))//
				.put("secretKey", current.stringValue(SysConfKeys.sds_s3_secret_key.name()))//
				.put("region", current.stringValue(SysConfKeys.sds_s3_region.name()))//
				.put("bucket", current.stringValue(SysConfKeys.sds_s3_bucket.name()))//
				.put("insecure", current.booleanValue(SysConfKeys.sds_s3_insecure.name(), false));

		Topology.get().nodes().stream().filter(iv -> iv.value.tags.contains("mail/imap")).forEach(iv -> {
			JsonObject configBackend = new JsonObject().put("backend", iv.value.address()).put("config", json);
			logger.info("reconfigure SDS {}", configBackend);
			// we keep this one synchronous instead of firing an event, otherwise it
			// conflicts with CyrusSysConfObserver
			try (SdsProxyManager sm = new SdsProxyManager(null, iv.value.address())) {
				sm.applyConfiguration(json).get(10, TimeUnit.SECONDS);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}

		});
	}

}
