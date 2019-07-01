package net.bluemind.core.rest;

import org.vertx.java.core.json.JsonObject;

@SuppressWarnings("serial")
public final class LocalJsonObject<T> extends JsonObject {

	private final T object;

	public LocalJsonObject(T object) {
		this.object = object;
	}

	public T getValue() {
		return object;
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
