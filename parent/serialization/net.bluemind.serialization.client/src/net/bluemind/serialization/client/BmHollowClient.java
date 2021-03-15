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
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private HttpURLConnection con;

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
			URL url = new URL(path);
			this.con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			this.inputStream = con.getInputStream();
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

			}
		}

		if (con != null) {
			try {
				con.disconnect();
			} catch (Exception e) {

			}
		}
	}

	public long getVersion() {
		try {
			return Long.valueOf(new BufferedReader(new InputStreamReader(openStream())).readLine().trim());
		} catch (Exception e) {
			logger.warn("Cannot read version from hollow connection", e);
			throw new RuntimeException(e);
		}
	}

	public long getVersionHeader() {
		return Long.valueOf(con.getHeaderField("X-BM-DATASET_VERSION"));
	}

}
