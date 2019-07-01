package net.bluemind.vertx.common;

import org.vertx.java.core.json.JsonObject;

@SuppressWarnings("serial")
public final class LocalJsonObject<T> extends JsonObject {

	private final Object object;

	public LocalJsonObject(Object object) {
		this.object = object;
	}

	@SuppressWarnings("unchecked")
	public T getValue() {
		return (T) object;
	}

	@Override
	public String encode() {
		throw new RuntimeException("Not encodable");
	}

	@Override
	public JsonObject copy() {
		return this;
	}

}
