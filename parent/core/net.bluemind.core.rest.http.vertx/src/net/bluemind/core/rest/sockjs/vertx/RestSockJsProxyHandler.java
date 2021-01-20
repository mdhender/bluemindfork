package net.bluemind.core.rest.sockjs.vertx;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.JsonEvent;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.base.IRestBusHandler;
import net.bluemind.core.rest.base.IRestCallHandler;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.utils.JsonUtils;

public class RestSockJsProxyHandler implements Handler<JsonEvent> {
	private static final Logger logger = LoggerFactory.getLogger(RestSockJsProxyHandler.class);
	final SockJSSocket sock;
	final Map<String, Future<MessageConsumer<JsonObject>>> handlers = new HashMap<>();
	private final List<String> remoteAddress;
	private final IRestBusHandler restbus;
	private final Vertx vertx;
	private final IRestCallHandler proxy;

	public RestSockJsProxyHandler(Vertx vertx, SockJSSocket sock, IRestCallHandler proxy, IRestBusHandler restbus) {
		this.vertx = vertx;
		this.sock = sock;

		this.remoteAddress = Arrays.asList(sock.remoteAddress().toString());
		this.restbus = restbus;
		this.proxy = proxy;
	}

	@Override
	public void handle(JsonEvent jsEv) {
		JsonObject msg = jsEv.objectValue();

		RestRequestWithId request = parseRequest(msg);
		if (logger.isDebugEnabled()) {
			logger.debug("C [verb: {}]: {}", request.verb, msg.encode());
		}
		Optional<String> id = request.id;
		switch (request.verb) {
		case "register":
			registerHandler(request);
			break;
		case "unregister":
			unregisterHandler(request.path);
			break;
		case "log":
			SecurityContext session = getSession(request);
			if (!session.isAnonymous()) {
				log(session, request);
			}
			break;
		case "event":
			sendEvent(request);
			break;
		default:
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
			break;
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
			Buffer toWrite = Buffer.buffer(response(requestId, value));
			sock.write(toWrite);
			if (sock.writeQueueFull()) {
				logger.warn("Websocket {} queue full", sock);
				sock.pause();
				sock.drainHandler(v -> sock.resume());
			}
		});
	}

	private static final JsonObject EMPTY_JS = new JsonObject();

	/**
	 * Write
	 * '{"headers":{},"requestId":"sockjs.tests.rocks","statusCode":200,"body":{"time":1599469771870}}'
	 * on the wire
	 * 
	 * @param requestId
	 * @param body
	 */
	private void dispatchEventBusMessage(String requestId, JsonObject body) {
		JsonObject js = new JsonObject();
		js.put("headers", EMPTY_JS).put("requestId", requestId).put("statusCode", 200).put("body", body);
		sock.write(js.toBuffer());
		if (sock.writeQueueFull()) {
			logger.warn("Websocket {} queue full after message from {}", sock, requestId);
			sock.pause();
			sock.drainHandler(v -> sock.resume());
		}
	}

	public void sendFault(Optional<String> id, Throwable e) {
		sendResponse(id, RestResponse.fault(e));
	}

	private String response(String id, RestResponse response) {
		Map<String, Object> r = new HashMap<>();
		r.put("requestId", id);
		r.put("statusCode", response.statusCode);
		r.put("headers", toMap(response.headers));
		String t = JsonUtils.asString(r);
		t = t.substring(0, t.length() - 1);
		if (response.data != null) {
			String v = response.data.toString(StandardCharsets.UTF_8);
			if (v.length() == 0) {
				return t + ",\"body\":null}";
			} else {
				return t + ",\"body\":" + v + "}";
			}
		} else {
			return t + ",\"body\":null}";
		}
	}

	private Map<String, String> toMap(MultiMap headers) {
		Map<String, String> ret = new HashMap<>();
		headers.forEach(a -> ret.put(a.getKey(), a.getValue()));
		return ret;
	}

	public void unregisterHandler(String path) {
		Future<MessageConsumer<JsonObject>> handler = handlers.remove(path);
		if (handler != null) {
			handler.onSuccess(MessageConsumer::unregister);
		} else if (logger.isDebugEnabled()) {
			logger.debug("Handler '{}' not found for unregistration", path);
		}
	}

	public void close() {
		vertx.eventBus().send("websocket." + sock.writeHandlerID() + ".closed", (String) null);

		handlers.forEach((path, handler) -> {
			logger.debug("unregister handler on {}", path);
			handler.onSuccess(MessageConsumer::unregister);
		});
		handlers.clear();
	}

	public void registerHandler(RestRequestWithId request) {
		logger.debug("register handler at {} for {}", request.path, request.id);
		String path = request.path;
		if (handlers.containsKey(path)) {
			handlers.get(path).onSuccess(MessageConsumer::unregister);
			handlers.remove(path);
		}
		Handler<Message<JsonObject>> handler = msg -> {
			JsonObject body = msg.body();
			dispatchEventBusMessage(path, body);
		};
		Future<MessageConsumer<JsonObject>> cons = restbus.register(request, () -> {
			sendResponse(request.id, RestResponse.ok(200, null));
			return handler;
		}, e -> {
			logger.warn("Cannot register sock handler, path: {}", request.path, e);
			sendFault(request.id, e);
		});
		handlers.put(path, cons);
	}

	public void sendEvent(RestRequestWithId request) {
		JsonObject jsBody = null;
		jsBody = (request.body != null) ? new JsonObject(request.body.toString()) : new JsonObject();

		if (logger.isDebugEnabled()) {
			logger.debug("send event {} to {} , {}", request.id.orElse("<unknown id>"), request.path, jsBody);
		}
		jsBody.put("sockId", sock.writeHandlerID());
		if (request.id.isPresent()
				&& (request.path.equals("xmpp/sessions-manager:open")
						|| (request.path.startsWith("xmpp/session/") && request.path.endsWith("/roster:entries")))
				|| (request.path.startsWith("xmpp/muc/") && request.path.endsWith(":create"))) {
			restbus.sendEvent(request, jsBody, m -> {
				JsonObject body = m != null ? m.body() : null;
				Buffer data = null;
				if (body != null) {
					data = Buffer.buffer(body.encode());
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
		MultiMap headers = asMap(msg.getJsonObject("headers"));
		MultiMap params = asMap(msg.getJsonObject("params"));

		Object jsonBody = msg.getValue("body");

		Buffer body = jsonBody != null ? Buffer.buffer(jsonBody.toString()) : null;
		return new RestRequestWithId(requestId, "sockjs", remoteAddress, verb, headers, path, params, body);
	}

	private static final MultiMap EMPTY_MAP = MultiMap.caseInsensitiveMultiMap();

	private MultiMap asMap(JsonObject object) {
		if (object != null) {
			MultiMap map = MultiMap.caseInsensitiveMultiMap();
			object.iterator().forEachRemaining(entry -> {
				String k = entry.getKey();
				Object v = entry.getValue();
				if (k != null && v != null) {
					map.add(k, (String) v);
				}
			});
			return map;
		} else {
			return EMPTY_MAP;
		}
	}
}
