package net.bluemind.core.rest.sockjs.vertx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.SockJSSocket;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.base.IRestBusHandler;
import net.bluemind.core.rest.base.IRestCallHandler;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.utils.JsonUtils;

public class RestSockJsProxyHandler implements Handler<Buffer> {
	private static final Logger logger = LoggerFactory.getLogger(RestSockJsProxyHandler.class);
	final SockJSSocket sock;
	final Map<String, Handler<Message<?>>> handlers = new HashMap<>();
	private final List<String> remoteAddress;
	private final IRestBusHandler restbus;
	private Vertx vertx;
	private IRestCallHandler proxy;

	public RestSockJsProxyHandler(Vertx vertx, SockJSSocket sock, IRestCallHandler proxy, IRestBusHandler restbus) {
		this.vertx = vertx;
		this.sock = sock;

		this.remoteAddress = Arrays.asList(sock.remoteAddress().toString());
		this.restbus = restbus;
		this.proxy = proxy;
	}

	@Override
	public void handle(Buffer data) {
		logger.debug("handle sock data {}", data);

		JsonObject msg = new JsonObject(data.toString());

		RestRequestWithId request = parseRequest(msg);
		Optional<String> id = request.id;
		if ("register".equals(request.method)) {
			registerHandler(request);
		} else if ("unregister".equals(request.method)) {
			unregisterHandler(request.path);
		} else if ("log".equals(request.method)) {
			SecurityContext session = getSession(request);
			if (!session.isAnonymous()) {
				log(session, request);
			}
		} else if ("event".equals(request.method)) {
			sendEvent(request);
		} else {
			proxy.call(request, new AsyncHandler<RestResponse>() {

				@Override
				public void success(RestResponse value) {
					sendResponse(id, value);
				}

				@Override
				public void failure(Throwable e) {
					sendFault(id, e);
				}
			});
		}
	}

	private void log(SecurityContext session, RestRequestWithId request) {
		JsonObject o = new JsonObject(request.body.toString());
		logger.error("{}@{} Client error : {}:{}", session.getSubject(), session.getContainerUid(), o.getString("name"),
				o.getString("message"));
	}

	private SecurityContext getSession(RestRequestWithId request) {
		String key = request.headers.get("X-BM-ApiKey");
		if (key != null) {
			SecurityContext ret = Sessions.sessionContext(key);
			if (ret != null) {
				return ret;
			}
		}
		return SecurityContext.ANONYMOUS.from(request.remoteAddresses);
	}

	public void sendResponse(Optional<String> id, RestResponse value) {
		id.ifPresent(requestId -> {
			if (sock.writeQueueFull()) {
				sock.drainHandler((v) -> {
					sock.write(new Buffer(response(requestId, value)));
				});
			} else {
				sock.write(new Buffer(response(requestId, value)));
			}
		});
	}

	public void sendFault(Optional<String> id, Throwable e) {
		id.ifPresent(requestId -> {
			sendResponse(id, RestResponse.fault(e));
		});
	}

	private String response(String id, RestResponse response) {
		Map<String, Object> r = new HashMap<String, Object>();
		r.put("requestId", id);
		r.put("statusCode", response.statusCode);
		r.put("headers", toMap(response.headers));
		String t = JsonUtils.asString(r);
		t = t.substring(0, t.length() - 1);
		if (response.data != null) {
			String v = response.data.toString("utf-8");
			if (v.length() == 0) {
				return t + ",\"body\":null}";
			} else {
				return t + ",\"body\":" + response.data.toString("utf-8") + "}";
			}
		} else {
			return t + ",\"body\":null}";
		}
	}

	private Map<String, String> toMap(MultiMap headers) {
		Map<String, String> ret = new HashMap<>();
		headers.forEach((a) -> ret.put(a.getKey(), a.getValue()));
		return ret;
	}

	public void unregisterHandler(String path) {
		logger.debug("unregister handler at {} ", path);
		Handler<Message<?>> handler = handlers.get(path);
		if (handler != null) {
			restbus.unregisterHandler(path, handler);
			handlers.remove(path);
		} else {
			logger.warn("unregiter handler {} not found", path);
		}
	}

	public void close() {
		vertx.eventBus().send("websocket." + sock.writeHandlerID() + ".closed", (String) null);

		handlers.forEach((path, handler) -> {
			logger.debug("unregister handler on {}", path);
			restbus.unregisterHandler(path, handler);
		});
		handlers.clear();
	}

	public void registerHandler(RestRequestWithId request) {
		logger.debug("register handler at {} for {}", request.path, request.id);
		String path = request.path;
		if (handlers.containsKey(path)) {
			restbus.unregisterHandler(path, handlers.get(path));
			handlers.remove(path);
		}
		Handler<Message<?>> handler = (msg) -> {
			@SuppressWarnings("unchecked")
			Message<JsonObject> m = (Message<JsonObject>) msg;
			JsonObject body = m.body();
			Buffer data = null;
			if (body != null) {
				data = new Buffer(body.encode());
			}
			sendResponse(Optional.of(path), RestResponse.ok(200, data));
		};
		restbus.register(request, (v) -> {
			handlers.put(path, handler);
			return handler;
		}, (e) -> {
			if (logger.isDebugEnabled()) {
				logger.warn("Cannot register sock handler, path: {}", request.path, e);
			} else {
				logger.warn("Cannot register sock handler: {}:{}", request.path, e.getMessage());
			}
			sendFault(request.id, e);
		});
	}

	public void sendEvent(RestRequestWithId request) {
		JsonObject jsBody = null;
		if (request.body != null) {
			jsBody = new JsonObject(request.body.toString());
		} else {
			jsBody = new JsonObject();
		}

		logger.debug("send event {} to {} , {}", request.id.orElse("<unknown id>"), request.path, jsBody);
		jsBody.putString("sockId", sock.writeHandlerID());
		if (request.id.isPresent()
				&& (request.path.equals("xmpp/sessions-manager:open")
						|| (request.path.startsWith("xmpp/session/") && request.path.endsWith("/roster:entries")))
				|| (request.path.startsWith("xmpp/muc/") && request.path.endsWith(":create"))) {
			restbus.sendEvent(request, jsBody, m -> {
				JsonObject body = m != null ? m.body() : null;
				Buffer data = null;
				if (body != null) {
					data = new Buffer(body.encode());
				}
				logger.debug("send RESPONSE {} WITH id !!! {} {}", request.path, request.id, body);
				sendResponse(request.id, RestResponse.ok(200, data));
			});
			logger.debug("send EVENT {} WITH id !!! {} {}", request.path, request.id, jsBody);
		} else {
			restbus.sendEvent(request, jsBody);
		}
	}

	private RestRequestWithId parseRequest(JsonObject msg) {

		String requestId = msg.getString("requestId");

		String verb = msg.getString("method");
		if (verb == null) {
			throw new IllegalArgumentException("method is null");
		}

		String path = msg.getString("path");
		if (path == null) {
			throw new IllegalArgumentException("path is null");
		}
		CaseInsensitiveMultiMap headers = new CaseInsensitiveMultiMap();
		headers.add(asMap(msg.getObject("headers")));

		CaseInsensitiveMultiMap params = new CaseInsensitiveMultiMap();
		params.add(asMap(msg.getObject("params")));
		JsonElement jsonBody = msg.getElement("body");
		Buffer body = jsonBody != null ? new Buffer(jsonBody.toString()) : null;
		RestRequestWithId request = new RestRequestWithId(requestId, "sockjs", remoteAddress, verb, headers, path,
				params, body);
		return request;
	}

	private Map<String, String> asMap(JsonObject object) {
		if (object != null) {
			Map<String, String> v = object.toMap().entrySet().stream()
					.filter(a -> a.getValue() != null && a.getKey() != null).collect(Collectors.toMap(a -> {
						return a.getKey();
					}, b -> {
						return (String) b.getValue();
					}));
			return v;
		} else {
			return new HashMap<>();
		}
	}
}
