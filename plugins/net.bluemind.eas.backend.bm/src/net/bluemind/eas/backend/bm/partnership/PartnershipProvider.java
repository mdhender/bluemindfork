/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.eas.backend.bm.partnership;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;

import io.vertx.core.Handler;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.IDevice;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.eas.dto.device.DeviceValidationRequest;
import net.bluemind.eas.dto.device.DeviceValidationResponse;
import net.bluemind.eas.partnership.IDevicePartnershipProvider;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.network.topology.Topology;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import net.bluemind.utils.IniFile;

public class PartnershipProvider implements IDevicePartnershipProvider {

	private static final Logger logger = LoggerFactory.getLogger(PartnershipProvider.class);
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory(MetricsRegistry.get(), PartnershipProvider.class);

	private static final Cache<String, DeviceValidationResponse> cache = PolledMeter.using(MetricsRegistry.get())
			.withId(new IdFactory(MetricsRegistry.get(), PartnershipProvider.class).name("partnerships"))
			.monitorValue(CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).recordStats().build(), c -> {
				return c.stats().hitRate() * 100;
			});

	private String coreUrl;

	@Override
	public void setupAndCheck(final DeviceValidationRequest req, final Handler<DeviceValidationResponse> respHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("[{}] Validating {}", req.loginAtDomain, req.deviceIdentifier);
		}

		if (req.deviceIdentifier == null || req.deviceIdentifier.equals("validate")) {
			DeviceValidationResponse dr = new DeviceValidationResponse();
			dr.success = true;
			// FIXME device id?
			// dr.internalId = found.getId();
			respHandler.handle(dr);
			return;
		}

		String cacheKey = req.loginAtDomain + "#" + req.deviceIdentifier;
		DeviceValidationResponse inCache = cache.getIfPresent(cacheKey);
		if (inCache != null) {
			respHandler.handle(inCache);
			return;
		}

		Iterator<String> latd = Splitter.on("@").split(req.loginAtDomain).iterator();
		@SuppressWarnings("unused")
		String login = latd.next();
		String domain = latd.next();

		try {
			// BM-8155
			IDomains domainsService = provider().instance(IDomains.class);
			ItemValue<Domain> dom = domainsService.findByNameOrAliases(domain);

			IUser userService = provider().instance(IUser.class, dom.uid);
			ItemValue<User> me = userService.byEmail(req.loginAtDomain);

			if (logger.isDebugEnabled()) {
				logger.debug("[{}] I have uid {}", req.loginAtDomain, me.uid);
			}

			IDevice deviceService = provider().instance(IDevice.class, me.uid);
			ItemValue<Device> device = deviceService.byIdentifier(req.deviceIdentifier);

			if (device != null) {
				DeviceValidationResponse dr = new DeviceValidationResponse();
				dr.success = device.value.hasPartnership;

				if (!dr.success) {
					dr.success = syncUnknown();
				}

				if (!dr.success) {
					dr.success = allowUnknown();
				}

				dr.internalId = device.uid;

				if (dr.success) {
					cache.put(cacheKey, dr);
				}

				respHandler.handle(dr);

			} else {
				logger.info("Device with identifier {} not found for user {}", req.deviceIdentifier, me.uid);
				Device d = new Device();
				d.identifier = req.deviceIdentifier;
				d.type = req.deviceType;
				d.owner = me.uid;

				String deviceUid = UUID.randomUUID().toString();
				deviceService.create(deviceUid, d);

				boolean allowed = syncUnknown();
				if (!allowed) {
					allowed = allowUnknown();
				}

				if (allowed) {
					DeviceValidationResponse dr = new DeviceValidationResponse();
					dr.success = true;
					dr.internalId = deviceUid;
					cache.put(cacheKey, dr);
					respHandler.handle(dr);
				} else {
					fail(respHandler);
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			fail(respHandler);
		}
	}

	private String locateCore() {
		if (coreUrl == null) {
			coreUrl = "http://" + Topology.get().core().value.address() + ":8090";
		}
		return coreUrl;
	}

	private void fail(Handler<DeviceValidationResponse> respHandler) {
		registry.counter(idFactory.name("deviceValidation", "status", "failure")).increment();
		DeviceValidationResponse dr = new DeviceValidationResponse();
		dr.success = false;
		respHandler.handle(dr);
	}

	private IServiceProvider provider() {
		return ClientSideServiceProvider.getProvider(locateCore(), Token.admin0())
				.setOrigin("bm-eas-PartnershipProvider");
	}

	private boolean syncUnknown() throws ServerFault {
		ISystemConfiguration service = provider().instance(ISystemConfiguration.class);
		SystemConf sc = service.getValues();
		return sc.values.containsKey("eas_sync_unknown") && sc.booleanValue("eas_sync_unknown");
	}

	private boolean allowUnknown() {
		IniFile ini = new IniFile("/etc/bm-eas/sync_perms.ini") {
			@Override
			public String getCategory() {
				return null;
			}
		};
		String freeForAll = ini.getProperty("allow.unknown.pda");
		if ("true".equals(freeForAll)) {
			return true;
		}
		return false;
	}

}
