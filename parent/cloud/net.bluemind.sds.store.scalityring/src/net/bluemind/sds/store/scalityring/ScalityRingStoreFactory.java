/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.sds.store.scalityring;

import java.util.function.Supplier;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;
import com.netflix.spectator.api.Registry;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.system.api.ArchiveKind;

public class ScalityRingStoreFactory implements ISdsBackingStoreFactory {
	private static final Logger logger = LoggerFactory.getLogger(ScalityRingStoreFactory.class);
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory("scalityring", MetricsRegistry.get(),
			ScalityRingStoreFactory.class);
	private static Supplier<AsyncHttpClient> httpClient = Suppliers.memoize(ScalityRingStoreFactory::getHttpClient);

	public ScalityRingStoreFactory() {
		// ok
	}

	@Override
	public ISdsBackingStore create(Vertx vertx, JsonObject configuration) {
		String type = configuration.getString("storeType");
		if (type == null || !type.equals(kind().toString())) {
			throw new IllegalArgumentException(
					"Configuration is not for a scality ring backend: " + configuration.encode());
		}
		logger.debug("Configuring with {}", configuration.encode());
		ScalityConfiguration scalityConfig = ScalityConfiguration.from(configuration);
		return new ScalityRingStore(scalityConfig, httpClient.get(), registry, idFactory);
	}

	@Override
	public ArchiveKind kind() {
		return ArchiveKind.ScalityRing;
	}

	protected static AsyncHttpClient getHttpClient() {
		boolean trustAll = true;
		AsyncHttpClientConfig httpClientConfig = new DefaultAsyncHttpClientConfig.Builder().setFollowRedirect(false)
				.setMaxConnectionsPerHost(ScalityRingStore.PARALLELISM)
				.setMaxConnections(ScalityRingStore.PARALLELISM * 2) //
				.setTcpNoDelay(true).setMaxRedirects(0).setMaxRequestRetry(0) //
				.setRequestTimeout(60_000).setConnectTimeout(15_000) //
				.setUseInsecureTrustManager(trustAll) //
				.setPooledConnectionIdleTimeout(10_000) //
				.setUserAgent("BlueMind/ScalityRing") //
				.build();
		return new DefaultAsyncHttpClient(httpClientConfig);
	}

}
