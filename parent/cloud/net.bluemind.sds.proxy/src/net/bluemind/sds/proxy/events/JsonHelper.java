package net.bluemind.sds.proxy.events;

import java.util.Arrays;

import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;

public class JsonHelper {

	public static boolean isValidJson(JsonObject json, String... fieldNames) {
		return Arrays.stream(fieldNames).map(fieldName -> json.containsKey(fieldName)).reduce(true,
				(acc, value) -> acc && value);
	}

	public static JsonObject getJsonFromString(String payload) {
		return Strings.isNullOrEmpty(payload) ? new JsonObject() : new JsonObject(payload);
	}
}
