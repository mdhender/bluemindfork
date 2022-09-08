package net.bluemind.serialization.client;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Supplier;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;

import net.bluemind.network.topology.Topology;

public class BmHollowClient implements AutoCloseable {

	public enum Type {
		snapshot, delta, version;
	}

	private static final Logger logger = LoggerFactory.getLogger(BmHollowClient.class);
	private static String host;
	private final Type type;
	private final String dataset;
	private final String subset;
	private final long requestedVersion;
	private InputStream inputStream;
	private String versionHeader;

	private static final Supplier<AsyncHttpClient> ahc = Suppliers.memoize(DefaultAsyncHttpClient::new);

	public BmHollowClient(Type type, String dataset, String subset, long version) {
		this.type = type;
		this.dataset = dataset;
		this.subset = subset;
		this.requestedVersion = type == Type.version ? 0 : version;
	}

	public InputStream openStream() {
		String path = String.format("http://%s:8090/serdata/%s/%s/%d/%s", getBaseUrl(), dataset, subset,
				requestedVersion, type.name());
		try {
			logger.info("Reading hollow from {}...", path);
			Response resp = ahc.get().prepareGet(path).execute().get();
			this.versionHeader = resp.getHeader("X-BM-DATASET_VERSION");
			this.inputStream = resp.getResponseBodyAsStream();
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			throw new HollowRetrievalException(e);
		}
		return this.inputStream;
	}

	private static String getBaseUrl() {
		if (null == host) {
			BmHollowClient.host = Topology.getIfAvailable().map(t -> t.core().value.address()).orElse("127.0.0.1");
		}
		return BmHollowClient.host;
	}

	@Override
	public void close() {
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (Exception e) {
				// OK
			}
		}
	}

	public long getVersion() {
		try {
			return Long.valueOf(new BufferedReader(new InputStreamReader(openStream())).readLine().trim());
		} catch (Exception e) {
			throw new HollowRetrievalException(e);
		}
	}

	public long getVersionHeader() {
		return Long.valueOf(versionHeader);
	}

}
