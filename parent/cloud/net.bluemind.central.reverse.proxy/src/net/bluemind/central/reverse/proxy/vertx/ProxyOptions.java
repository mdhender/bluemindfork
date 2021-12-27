package net.bluemind.central.reverse.proxy.vertx;

import io.vertx.core.json.JsonObject;

/**
 * Proxy options.
 */
public class ProxyOptions {

	/**
	 * Enable WebSocket support : {@code true}
	 */
	public static final boolean DEFAULT_SUPPORT_WEBSOCKET = false;

	private boolean supportWebSocket;

	public ProxyOptions(JsonObject json) {
		ProxyOptionsConverter.fromJson(json, this);
	}

	public ProxyOptions() {
		supportWebSocket = DEFAULT_SUPPORT_WEBSOCKET;
	}

	/**
	 * @return whether WebSocket are supported
	 */
	public boolean getSupportWebSocket() {
		return supportWebSocket;
	}

	/**
	 * Set whether WebSocket are supported.
	 *
	 * @param supportWebSocket {@code true} to enable WebSocket support,
	 *                         {@code false} otherwise
	 * @return a reference to this, so the API can be used fluently
	 */
	public ProxyOptions setSupportWebSocket(boolean supportWebSocket) {
		this.supportWebSocket = supportWebSocket;
		return this;
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		ProxyOptionsConverter.toJson(this, json);
		return json;
	}

	private static class ProxyOptionsConverter {

		public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, ProxyOptions obj) {
			for (java.util.Map.Entry<String, Object> member : json) {
				if ("supportWebSocket".equals(member.getKey()) && member.getValue() instanceof Boolean) {
					obj.setSupportWebSocket((Boolean) member.getValue());
				}
			}
		}

		public static void toJson(ProxyOptions obj, JsonObject json) {
			toJson(obj, json.getMap());
		}

		public static void toJson(ProxyOptions obj, java.util.Map<String, Object> json) {
			json.put("supportWebSocket", obj.getSupportWebSocket());
		}
	}

}
