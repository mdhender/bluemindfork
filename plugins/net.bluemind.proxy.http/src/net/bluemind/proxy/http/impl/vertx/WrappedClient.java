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

package net.bluemind.proxy.http.impl.vertx;

import java.util.Set;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.http.WebSocketVersion;

public final class WrappedClient implements HttpClient {

	private static final Logger logger = LoggerFactory.getLogger(WrappedClient.class);
	private final HttpClient hc;
	private final Thread creatingThread;

	public WrappedClient(HttpClient hc) {
		this.hc = hc;
		this.creatingThread = Thread.currentThread();
	}

	private final void check() {
		Thread cur = Thread.currentThread();
		if (cur != creatingThread) {
			String log = "Created in " + creatingThread.getName() + ", used from " + cur.getName();
			logger.error(log, new RuntimeException(log));
		}
	}

	public HttpClient setTCPNoDelay(boolean tcpNoDelay) {
		check();
		return hc.setTCPNoDelay(tcpNoDelay);
	}

	public HttpClient setTrustAll(boolean trustAll) {
		check();
		return hc.setTrustAll(trustAll);
	}

	public HttpClient setSSL(boolean ssl) {
		check();
		return hc.setSSL(ssl);
	}

	public HttpClient setSendBufferSize(int size) {
		check();
		return hc.setSendBufferSize(size);
	}

	public boolean isSSL() {
		check();
		return hc.isSSL();
	}

	public HttpClient setSSLContext(SSLContext sslContext) {
		check();
		return hc.setSSLContext(sslContext);
	}

	public HttpClient setTCPKeepAlive(boolean keepAlive) {
		check();
		return hc.setTCPKeepAlive(keepAlive);
	}

	public boolean isTrustAll() {
		check();
		return hc.isTrustAll();
	}

	public HttpClient setReceiveBufferSize(int size) {
		check();
		return hc.setReceiveBufferSize(size);
	}

	public HttpClient setSoLinger(int linger) {
		check();
		return hc.setSoLinger(linger);
	}

	public HttpClient setReuseAddress(boolean reuse) {
		check();
		return hc.setReuseAddress(reuse);
	}

	public HttpClient setKeyStorePath(String path) {
		check();
		return hc.setKeyStorePath(path);
	}

	public HttpClient setTrafficClass(int trafficClass) {
		check();
		return hc.setTrafficClass(trafficClass);
	}

	public HttpClient setUsePooledBuffers(boolean pooledBuffers) {
		check();
		return hc.setUsePooledBuffers(pooledBuffers);
	}

	public HttpClient exceptionHandler(Handler<Throwable> handler) {
		check();
		return hc.exceptionHandler(handler);
	}

	public int getSendBufferSize() {
		check();
		return hc.getSendBufferSize();
	}

	public HttpClient setMaxPoolSize(int maxConnections) {
		check();
		return hc.setMaxPoolSize(maxConnections);
	}

	public int getReceiveBufferSize() {
		check();
		return hc.getReceiveBufferSize();
	}

	public boolean isTCPNoDelay() {
		check();
		return hc.isTCPNoDelay();
	}

	public String getKeyStorePath() {
		check();
		return hc.getKeyStorePath();
	}

	public boolean isReuseAddress() {
		check();
		return hc.isReuseAddress();
	}

	public boolean isTCPKeepAlive() {
		check();
		return hc.isTCPKeepAlive();
	}

	public HttpClient setKeyStorePassword(String pwd) {
		check();
		return hc.setKeyStorePassword(pwd);
	}

	public int getTrafficClass() {
		check();
		return hc.getTrafficClass();
	}

	public int getSoLinger() {
		check();
		return hc.getSoLinger();
	}

	public int getMaxPoolSize() {
		check();
		return hc.getMaxPoolSize();
	}

	public boolean isUsePooledBuffers() {
		check();
		return hc.isUsePooledBuffers();
	}

	public HttpClient setMaxWaiterQueueSize(int maxWaiterQueueSize) {
		check();
		return hc.setMaxWaiterQueueSize(maxWaiterQueueSize);
	}

	public String getKeyStorePassword() {
		check();
		return hc.getKeyStorePassword();
	}

	public HttpClient setTrustStorePath(String path) {
		check();
		return hc.setTrustStorePath(path);
	}

	public int getMaxWaiterQueueSize() {
		check();
		return hc.getMaxWaiterQueueSize();
	}

	public HttpClient setConnectionMaxOutstandingRequestCount(int connectionMaxOutstandingRequestCount) {
		check();
		return hc.setConnectionMaxOutstandingRequestCount(connectionMaxOutstandingRequestCount);
	}

	public String getTrustStorePath() {
		check();
		return hc.getTrustStorePath();
	}

	public HttpClient setTrustStorePassword(String pwd) {
		check();
		return hc.setTrustStorePassword(pwd);
	}

	public int getConnectionMaxOutstandingRequestCount() {
		check();
		return hc.getConnectionMaxOutstandingRequestCount();
	}

	public HttpClient setKeepAlive(boolean keepAlive) {
		check();
		return hc.setKeepAlive(keepAlive);
	}

	public String getTrustStorePassword() {
		check();
		return hc.getTrustStorePassword();
	}

	public boolean isKeepAlive() {
		check();
		return hc.isKeepAlive();
	}

	public HttpClient setPipelining(boolean pipelining) {
		check();
		return hc.setPipelining(pipelining);
	}

	public boolean isPipelining() {
		check();
		return hc.isPipelining();
	}

	public HttpClient setPort(int port) {
		check();
		return hc.setPort(port);
	}

	public int getPort() {
		check();
		return hc.getPort();
	}

	public HttpClient setHost(String host) {
		check();
		return hc.setHost(host);
	}

	public String getHost() {
		check();
		return hc.getHost();
	}

	public HttpClient connectWebsocket(String uri, Handler<WebSocket> wsConnect) {
		check();
		return hc.connectWebsocket(uri, wsConnect);
	}

	public HttpClient connectWebsocket(String uri, WebSocketVersion wsVersion, Handler<WebSocket> wsConnect) {
		check();
		return hc.connectWebsocket(uri, wsVersion, wsConnect);
	}

	public HttpClient connectWebsocket(String uri, WebSocketVersion wsVersion, MultiMap headers,
			Handler<WebSocket> wsConnect) {
		check();
		return hc.connectWebsocket(uri, wsVersion, headers, wsConnect);
	}

	public HttpClient connectWebsocket(String uri, WebSocketVersion wsVersion, MultiMap headers,
			Set<String> subprotocols, Handler<WebSocket> wsConnect) {
		check();
		return hc.connectWebsocket(uri, wsVersion, headers, subprotocols, wsConnect);
	}

	public HttpClient getNow(String uri, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.getNow(uri, responseHandler);
	}

	public HttpClient getNow(String uri, MultiMap headers, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.getNow(uri, headers, responseHandler);
	}

	public HttpClientRequest options(String uri, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.options(uri, responseHandler);
	}

	public HttpClientRequest get(String uri, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.get(uri, responseHandler);
	}

	public HttpClientRequest head(String uri, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.head(uri, responseHandler);
	}

	public HttpClientRequest post(String uri, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.post(uri, responseHandler);
	}

	public HttpClientRequest put(String uri, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.put(uri, responseHandler);
	}

	public HttpClientRequest delete(String uri, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.delete(uri, responseHandler);
	}

	public HttpClientRequest trace(String uri, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.trace(uri, responseHandler);
	}

	public HttpClientRequest connect(String uri, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.connect(uri, responseHandler);
	}

	public HttpClientRequest patch(String uri, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.patch(uri, responseHandler);
	}

	public HttpClientRequest request(String method, String uri, Handler<HttpClientResponse> responseHandler) {
		check();
		return hc.request(method, uri, responseHandler);
	}

	public void close() {
		check();
		hc.close();
	}

	public HttpClient setVerifyHost(boolean verifyHost) {
		check();
		return hc.setVerifyHost(verifyHost);
	}

	public boolean isVerifyHost() {
		check();
		return hc.isVerifyHost();
	}

	public HttpClient setConnectTimeout(int timeout) {
		check();
		return hc.setConnectTimeout(timeout);
	}

	public int getConnectTimeout() {
		return hc.getConnectTimeout();
	}

	public HttpClient setTryUseCompression(boolean tryUseCompression) {
		check();
		return hc.setTryUseCompression(tryUseCompression);
	}

	public boolean getTryUseCompression() {
		check();
		return hc.getTryUseCompression();
	}

	public HttpClient setMaxWebSocketFrameSize(int maxSize) {
		check();
		return hc.setMaxWebSocketFrameSize(maxSize);
	}

	public int getMaxWebSocketFrameSize() {
		check();
		return hc.getMaxWebSocketFrameSize();
	}

}
