/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.proxy.support;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class AHCWithProxy {
	private static final Logger logger = LoggerFactory.getLogger(AHCWithProxy.class);

	private static final int TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
	private static final int DEFAULT_READ_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
	private static final int MAX_REDIRECT = 5;
	private static final int DEFAULT_POOLED_CONN_IDLE_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(5,
			TimeUnit.SECONDS);

	/**
	 * Get BlueMind default configuration
	 * 
	 * @return DefaultAsyncHttpClientConfig.Builder set with 5 max redirect, no
	 *         retry and 30s timeout, accepting all certificates
	 */
	public static DefaultAsyncHttpClientConfig.Builder defaultConfig() {
		Builder configBuilder = new DefaultAsyncHttpClientConfig.Builder().setFollowRedirect(true)
				.setMaxRedirects(MAX_REDIRECT).setPooledConnectionIdleTimeout(DEFAULT_POOLED_CONN_IDLE_TIMEOUT)
				.setMaxRequestRetry(0).setRequestTimeout(TIMEOUT).setReadTimeout(DEFAULT_READ_TIMEOUT);

		try {
			configBuilder.setSslContext(
					SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
		} catch (SSLException e) {
			logger.warn("Unable to init insecure SslContext, continue with default", e);
		}

		return configBuilder;
	}

	/**
	 * Get {@link org.asynchttpclient.AsyncHttpClient} with BlueMind default
	 * configuration
	 * 
	 * @param systemConfAsMap used keys are proxy
	 *                        {@link net.bluemind.system.api.SysConfKeys} keys
	 * @return AsyncHttpClient with proxy selector set
	 */
	public static AsyncHttpClient build(Map<String, String> systemConfAsMap) {
		return build(defaultConfig(), systemConfAsMap);
	}

	/**
	 * Get {@link org.asynchttpclient.AsyncHttpClient} with BlueMind default
	 * configuration
	 * 
	 * @param systemConf {@link net.bluemind.system.api.SystemConf}
	 * @return AsyncHttpClient with proxy selector set
	 */
	public static AsyncHttpClient build(SystemConf systemConf) {
		return build(defaultConfig(), systemConf);
	}

	/**
	 * Get {@link org.asynchttpclient.AsyncHttpClient}
	 * 
	 * @param config          {@link org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder}
	 * @param systemConfAsMap used keys are proxy
	 *                        {@link net.bluemind.system.api.SysConfKeys} keys
	 * @return AsyncHttpClient with proxy selector set
	 */
	public static AsyncHttpClient build(DefaultAsyncHttpClientConfig.Builder config,
			Map<String, String> systemConfAsMap) {
		return build(config, getProxySystemConf(systemConfAsMap));
	}

	/**
	 * Get {@link org.asynchttpclient.AsyncHttpClient}
	 * 
	 * @param config     {@link org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder}
	 * @param systemConf
	 * @return AsyncHttpClient with proxy selector set
	 */
	public static AsyncHttpClient build(DefaultAsyncHttpClientConfig.Builder config, SystemConf systemConf) {
		return new DefaultAsyncHttpClient(config.setProxyServerSelector(new BMProxyServerSelector(systemConf)).build());
	}

	private static SystemConf getProxySystemConf(Map<String, String> systemConfAsMap) {
		boolean enabled = Boolean.valueOf(systemConfAsMap.get(SysConfKeys.http_proxy_enabled.name()));
		if (!enabled) {
			return new SystemConf();
		}

		Map<String, String> systemConf = new HashMap<>();
		systemConf.put(SysConfKeys.http_proxy_enabled.name(), Boolean.toString(enabled));
		systemConf.put(SysConfKeys.http_proxy_hostname.name(),
				systemConfAsMap.get(SysConfKeys.http_proxy_hostname.name()));
		systemConf.put(SysConfKeys.http_proxy_port.name(), systemConfAsMap.get(SysConfKeys.http_proxy_port.name()));
		systemConf.put(SysConfKeys.http_proxy_login.name(), systemConfAsMap.get(SysConfKeys.http_proxy_login.name()));
		systemConf.put(SysConfKeys.http_proxy_password.name(),
				systemConfAsMap.get(SysConfKeys.http_proxy_password.name()));
		systemConf.put(SysConfKeys.http_proxy_exceptions.name(),
				systemConfAsMap.get(SysConfKeys.http_proxy_exceptions.name()));

		return SystemConf.create(systemConf);

	}
}
