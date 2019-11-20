package net.bluemind.sds.proxy.events;

import java.util.Arrays;

import org.vertx.java.core.json.JsonObject;

import com.google.common.base.Strings;

public class JsonHelper {

	public static boolean isValidJson(JsonObject json, String... fieldNames) {
		return Arrays.stream(fieldNames).map(fieldName -> json.containsField(fieldName)).reduce(true,
				(acc, value) -> acc && value);
	}

	public static JsonObject getJsonFromString(String payload) {
		return Strings.isNullOrEmpty(payload) ? new JsonObject() : new JsonObject(payload);
	}
}
