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
package net.bluemind.xivo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

import net.bluemind.xivo.client.impl.AHCHelper;
import net.bluemind.xivo.common.Auth;

/**
 * Simple client class for Xivo REST services
 * 
 */
public final class XivoClient {

	private String restUrl;
	private static final Logger logger = LoggerFactory.getLogger(XivoClient.class);
	private static final String JSON_TYPE = "application/json";

	public XivoClient(String host) {
		if (host.contains(":")) {
			restUrl = "http://" + host + "/xuc/api/1.0/";
		} else {
			restUrl = "http://" + host + ":9000/xuc/api/1.0/";
		}
		logger.info("restUrl {} ", restUrl);
	}

	public void handshake(String domain) {
		String url = url().append("handshake/").append(domain).append('/').toString();
		try {
			Response resp = post(url, null).execute().get();
			int status = resp.getStatusCode();
			logger.info("[{}] Handshake required.", domain);
			if (status != 200) {
				throw new XivoFault("Status " + status + " received from XIVO");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void setDND(String login, String domain, boolean doNotDisturb) throws XivoFault {
		String url = url().append("dnd/").append(domain).append('/').append(login).append('/').toString();

		JsonObject jso = new JsonObject();
		jso.putBoolean("state", doNotDisturb);

		try {
			Response resp = post(url, jso).execute().get();
			int status = resp.getStatusCode();
			logger.info("[{}] DND for user {}@{} set to {}", status, login, domain, doNotDisturb);
			if (status != 200) {
				throw new XivoFault("Status " + status + " received from XIVO");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void dial(String login, String domain, String phoneNumber) throws XivoFault {
		String url = url().append("dial/").append(domain).append('/').append(login).append('/').toString();

		JsonObject jso = new JsonObject();
		jso.putString("number", phoneNumber);

		try {
			logger.info("Dialing {} for user {}@{}...", phoneNumber, login, domain);
			Response resp = post(url, jso).execute().get();
			int status = resp.getStatusCode();
			if (status != 200) {
				throw new XivoFault("Status " + status + " received from XIVO");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private StringBuilder url() {
		StringBuilder sb = new StringBuilder(64);
		sb.append(restUrl);
		return sb;
	}

	private BoundRequestBuilder post(String url, JsonObject jso) {
		BoundRequestBuilder req = AHCHelper.get().preparePost(url);
		req.addHeader(Headers.APIKEY, Auth.key());

		if (jso != null) {
			req.addHeader("Content-Type", JSON_TYPE);
			req.addHeader("Accept", JSON_TYPE);
			if (logger.isDebugEnabled()) {
				logger.debug("POST {}:\n{}", url, jso.encodePrettily());
			}
			byte[] body = jso.encode().getBytes();
			req.setBody(body);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("POST {} (empty body)", url);
			}
			req.setBody(new byte[0]);
		}

		return req;

	}

	/**
	 * @param string
	 * @param string2
	 * @param number
	 * @throws XivoFault
	 */
	public void forward(String login, String domain, String phoneNumber) throws XivoFault {
		String url = url().append("uncForward/").append(domain).append('/').append(login).append('/').toString();

		boolean enabled = !phoneNumber.isEmpty();
		JsonObject jso = new JsonObject();
		jso.putString("destination", phoneNumber);
		jso.putBoolean("state", enabled);

		try {
			logger.info("Forward {} for user {}@{}...", enabled ? phoneNumber : "disabled", login, domain);
			Response resp = post(url, jso).execute().get();
			int status = resp.getStatusCode();
			if (status != 200) {
				throw new XivoFault("Status " + status + " received from XIVO");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
