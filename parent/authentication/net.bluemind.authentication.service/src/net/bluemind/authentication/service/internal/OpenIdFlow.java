/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.authentication.service.internal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.AccessTokenInfo;
import net.bluemind.authentication.api.AccessTokenInfo.TokenStatus;
import net.bluemind.authentication.api.RefreshToken;
import net.bluemind.authentication.persistence.UserRefreshTokenStore;
import net.bluemind.authentication.service.OpenIdContext;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.UserAccessToken;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.service.internal.IInCoreDomainSettings;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.utils.Trust;

public class OpenIdFlow {

	protected final BmContext context;
	protected static final Logger logger = LoggerFactory.getLogger(OpenIdFlow.class);

	public OpenIdFlow(BmContext context) {
		this.context = context;
	}

	public AccessTokenInfo refreshOpenIdToken(String domainUid, String userUid, RefreshToken refreshToken)
			throws OpenIdException {
		String applicationIdKey = refreshToken.systemIdentifier + "_appid";
		String applicationSecretKey = refreshToken.systemIdentifier + "_secret";
		String tokenEndpointKey = refreshToken.systemIdentifier + "_tokenendpoint";

		IInCoreDomainSettings settingsService = context.su().provider().instance(IInCoreDomainSettings.class,
				context.getSecurityContext().getContainerUid());
		Map<String, String> settings = settingsService.get();
		String clientSecret = settings.get(applicationSecretKey);
		String tokenEndpoint = settings.get(tokenEndpointKey);
		String applicationId = settings.get(applicationIdKey);

		Map<String, String> params = new HashMap<>();
		params.put("grant_type", "refresh_token");
		params.put("client_id", applicationId);
		params.put("client_secret", clientSecret);
		params.put("refresh_token", refreshToken.token);

		JsonObject jwtToken = postCall(tokenEndpoint, params);

		if (jwtToken.containsKey("access_token")) {
			storeAccessToken(domainUid, userUid, refreshToken.systemIdentifier, jwtToken);
			AccessTokenInfo info = new AccessTokenInfo();
			info.status = TokenStatus.TOKEN_OK;
			return info;
		} else {
			AccessTokenInfo notValid = new AccessTokenInfo();
			notValid.status = TokenStatus.TOKEN_NOT_VALID;
			return notValid;
		}
	}

	public UserAccessToken createAccessToken(String userUid, String systemIdentifier, JsonObject jwtToken) {
		String accessToken = jwtToken.getString("access_token");
		long expiration = jwtToken.getInteger("expires_in");
		long expirationDate = new Date().getTime() + (expiration * 1000);
		return new UserAccessToken(accessToken, new Date(expirationDate));
	}

	public void storeRefreshToken(OpenIdContext openIdContext, String refreshToken) {
		if (refreshToken != null) {
			UserRefreshTokenStore store = new UserRefreshTokenStore(context.getDataSource(), openIdContext.userUid);
			RefreshToken refreshTokenObject = new RefreshToken();
			refreshTokenObject.systemIdentifier = openIdContext.systemIdentifier;
			refreshTokenObject.token = refreshToken;
			refreshTokenObject.expiryTime = null; // FIXME not specified, token expiration is system dependent
			store.add(refreshTokenObject);
		}
	}

	private HttpURLConnection connect(String url) throws MalformedURLException, IOException {
		Map<String, String> sysConfMap = context.su().provider().instance(ISystemConfiguration.class)
				.getValues().values;
		HttpURLConnection connection = null;
		String proxyEnabled = sysConfMap.get(SysConfKeys.http_proxy_enabled.name());
		if (proxyEnabled == null || proxyEnabled.trim().isEmpty() || !proxyEnabled.equals("true")) {
			connection = (HttpURLConnection) new URL(url).openConnection();
		} else {
			Proxy proxy = new Proxy(Proxy.Type.HTTP,
					new InetSocketAddress(sysConfMap.get(SysConfKeys.http_proxy_hostname.name()),
							Integer.valueOf(sysConfMap.get(SysConfKeys.http_proxy_port.name()))));
			connection = (HttpURLConnection) new URL(url).openConnection(proxy);
		}
		new Trust().prepareConnection("openid-connect", connection);
		return connection;
	}

	protected JsonObject postCall(String url, Map<String, String> parameters) {
		StringBuilder json = new StringBuilder();
		HttpURLConnection conn = null;
		try {
			byte[] data = getFormDataString(parameters).getBytes();
			int postDataLength = data.length;
			conn = connect(url);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
			try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
				wr.write(data);
				wr.flush();
			}

			try (InputStream in = conn.getInputStream()) {
				int i;
				while ((i = in.read()) != -1) {
					json.append((char) i);
				}
			}
			int httpCode = conn.getResponseCode();
			if (httpCode != 200) {
				throw new OpenIdException(httpCode, json.toString());
			}
		} catch (Exception e) {
			throw new ServerFault(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		return new JsonObject(json.toString());
	}

	private String getFormDataString(Map<String, String> params) throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (first) {
				first = false;
			} else {
				result.append("&");
			}
			result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
		}
		return result.toString();
	}

	public void storeAccessToken(String domainUid, String userUid, String systemIdentifier, JsonObject jwtToken) {
		UserAccessTokenCache.get(context).put(domainUid, userUid, systemIdentifier,
				createAccessToken(userUid, systemIdentifier, jwtToken));
	}

}
