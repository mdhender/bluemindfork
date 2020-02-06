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
package net.bluemind.locator.client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.AsciiString;
import net.bluemind.locator.client.impl.AHCHelper;

public final class LocatorClient {

	private static final Logger logger = LoggerFactory.getLogger(LocatorClient.class);

	private final String locUrl;
	private static final CharSequence ORIGIN_HEADER = new AsciiString("X-Bm-Origin");
	private static final String ORIGIN_VALUE = System.getProperty("net.bluemind.property.product", "unknown");

	public LocatorClient() {
		this.locUrl = NonOsgiActivator.get().getLocatorUrl();
	}

	public String locateHost(String serviceSlashProperty, String loginAtDomain) {
		String url = url(serviceSlashProperty, loginAtDomain);
		String ip = null;

		AsyncHttpClient cl = AHCHelper.get();
		try {
			BoundRequestBuilder prepared = cl.prepareGet(url);
			prepared.addHeader(ORIGIN_HEADER, ORIGIN_VALUE);
			ListenableFuture<Response> future = prepared.execute();
			Response response = future.get();

			if (response.getStatusCode() != 200) {
				return null;
			}
			InputStream is = response.getResponseBodyAsStream();
			BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			ip = r.readLine();
			r.close();
		} catch (FileNotFoundException fnfe) {
			logger.warn("host not found {} for {}", serviceSlashProperty, loginAtDomain);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return ip;
	}

	public Collection<String> locateHosts(String serviceSlashProperty, String loginAtDomain) {
		String url = url(serviceSlashProperty, loginAtDomain);
		List<String> ips = new ArrayList<>(10);

		AsyncHttpClient cl = AHCHelper.get();
		try {
			ListenableFuture<Response> future = cl.prepareGet(url).execute();
			Response response = future.get();

			if (response.getStatusCode() != 200) {
				return null;
			}
			InputStream is = response.getResponseBodyAsStream();
			BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			String l = null;
			while ((l = r.readLine()) != null) {
				if (!l.isEmpty()) {
					ips.add(l);
				}
			}
			r.close();
		} catch (FileNotFoundException fnfe) {
			logger.warn("host not found {} for {}", serviceSlashProperty, loginAtDomain);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return ips;
	}

	private String url(String serviceSlashProperty, String loginAtDomain) {
		StringBuilder sb = new StringBuilder(128);
		sb.append(locUrl).append("location/host/").append(serviceSlashProperty).append('/').append(loginAtDomain);
		return sb.toString();
	}
}
