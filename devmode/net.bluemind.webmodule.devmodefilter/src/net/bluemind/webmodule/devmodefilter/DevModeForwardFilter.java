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
package net.bluemind.webmodule.devmodefilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.streams.Pump;

import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.eclipse.common.IHasPriority;
import net.bluemind.webmodule.devmodefilter.DevModeState.ServerPort;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.NeedVertx;

/**
 * format du fichier /root/dev-filters.properties redirection http:
 * https-forward=/adminconsole/net.bluemind.ui.adminconsole.main:192.168.0.1:8995:/net.bluemind.ui.adminconsole.main
 * port-forward=SRC_PORT:DST_HOST
 *
 */
public class DevModeForwardFilter implements IWebFilter, NeedVertx, IHasPriority {
	private static final Logger logger = LoggerFactory.getLogger(DevModeForwardFilter.class);
	private HttpClientProvider clientProvider;

	private StateWatcher stateWatcher;
	private List<ForwardFilter> forwardFilters;
	private List<Pattern> denyFilters;

	public DevModeForwardFilter() {

	}

	@Override
	public HttpServerRequest filter(HttpServerRequest request) {
		String path = request.path();

		if (denyFilters.stream().anyMatch(f -> f.matcher(path).matches())) {
			request.response().setStatusCode(404).setStatusMessage("denied by devfilter").end();
			return null;
		}

		Optional<ForwardFilter> match = forwardFilters.stream().filter(f -> f.match(path) != null).findFirst();

		if (!match.isPresent()) {
			logger.debug("no devmode for path {}", path);
			return request;
		}

		request.exceptionHandler(e -> {
			logger.error("Request Error : " + e.getMessage(), e);
		});
		ForwardFilter ff = match.get();
		String to = ff.match(path);

		String uri = to;
		logger.info("forward request {} to {}:{}/{}", request.uri(), ff.serverPort.ip, ff.serverPort.port, uri);

		HttpClient client = clientProvider.getClient(ff.serverPort.ip, ff.serverPort.port);
		if (client.isKeepAlive()) {
			client.setKeepAlive(false);
		}

		client.exceptionHandler(e -> {
			logger.error("Client Error: " + e.getMessage(), e);
		});

		HttpClientRequest remoteRequest = client.request(request.method(), uri, r -> {
			logger.info("response for request http://{}:{}{}{} : {} {}", ff.serverPort.ip, ff.serverPort.port, uri,
					r.statusCode(), r.statusMessage());

			request.response().headers().add(r.headers());

			request.response().setStatusCode(r.statusCode());
			request.response().setStatusMessage(r.statusMessage());
			request.response().exceptionHandler(e -> {
				logger.error("Response error :" + e.getMessage(), e);
				request.response().setStatusCode(500).setStatusMessage(e.getMessage() != null ? e.getMessage() : "null")
						.end();
			});

			request.response().setChunked(true);

			r.exceptionHandler(e -> {
				logger.error("Client response error", e);
				request.response().setStatusCode(500).setStatusMessage(e.getMessage() != null ? e.getMessage() : "null")
						.end();
			});
			Pump p = Pump.createPump(r, request.response());
			r.endHandler(v -> request.response().end());
			p.start();
		});

		remoteRequest.headers().add(request.headers());
		remoteRequest.exceptionHandler(h -> {
			logger.error("Remote error: " + h.getMessage(), h);
			request.response().setStatusCode(500).setStatusMessage(h.getMessage() != null ? h.getMessage() : "null")
					.end();
		});
		remoteRequest.setChunked(HttpHeaders.CHUNKED.equals(request.headers().get(HttpHeaders.TRANSFER_ENCODING)));

		request.endHandler(v -> {
			remoteRequest.end();
		});

		Pump.createPump(request, remoteRequest).start();
		return null;

	}

	@Override
	public void setVertx(Vertx vertx) {
		this.clientProvider = new HttpClientProvider(vertx);
		updateState(new DevModeState());
		stateWatcher = new StateWatcher(vertx, (state) -> {
			updateState(state);
			return null;
		});
		stateWatcher.start();
	}

	private static class ForwardFilter {
		public final ServerPort serverPort;
		public final Pattern pattern;
		private String replace;

		public ForwardFilter(ServerPort serverPort, Pattern pattern, String replace) {
			this.serverPort = serverPort;
			this.pattern = pattern;
			this.replace = replace;
		}

		public String match(String path) {
			Matcher m = pattern.matcher(path);
			if (m.matches()) {
				return m.replaceAll(replace);
			} else {
				return null;
			}
		}

	}

	private void updateState(DevModeState state) {
		forwardFilters = new ArrayList<>(state.filters.size());
		state.filters.stream().filter(f -> f.serverId != null).forEach(f -> {
			ServerPort server = state.servers.get(f.serverId);
			Pattern pattern = Pattern.compile(f.search);
			ForwardFilter ff = new ForwardFilter(server, pattern, f.replace);
			forwardFilters.add(ff);
		});

		denyFilters = new ArrayList<>(state.filters.size());
		state.filters.stream().filter(f -> f.serverId == null).forEach(f -> {
			Pattern pattern = Pattern.compile(f.search);
			denyFilters.add(pattern);
		});
	}

	public static void main(String[] args) {
		Pattern p = Pattern.compile("/adminconsole/net.bluemind.ui.adminconsole.main/(.*+)?");
		Matcher m = p.matcher("/cal/index.html?reload-devmode");

		System.out.println(m.replaceAll("/api/$1"));
		System.out.println(m.matches());
	}

	@Override
	public int priority() {
		return 10;
	}
}
