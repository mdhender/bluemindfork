package net.bluemind.sds.proxy.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.sds.proxy.events.JsonHelper;

public class JSONHelperTest {

	@Test
	public void validJSON() {
		String[] fieldNames = new String[] { "prop1", "prop2" };
		JsonObject json = new JsonObject().putString("prop1", "val").putString("prop2", "value");
		assertTrue(JsonHelper.isValidJson(json, fieldNames));
	}

	@Test
	public void missingProp() {
		String[] fieldNames = new String[] { "prop1", "prop2" };
		JsonObject json = new JsonObject().putString("prop1", "val");
		assertFalse(JsonHelper.isValidJson(json, fieldNames));
	}

	@Test
	public void unnecessaryProp() {
		String[] fieldNames = new String[] { "prop1", "prop2" };
		JsonObject json = new JsonObject().putString("prop1", "val").putString("prop2", "val").putString("prop3",
				"val");
		assertTrue(JsonHelper.isValidJson(json, fieldNames));
	}

	@Test
	public void nullString() {
		assertEquals(new JsonObject(), JsonHelper.getJsonFromString(null));
	}

	@Test
	public void emptyString() {
		assertEquals(new JsonObject(), JsonHelper.getJsonFromString(""));
	}

	@Test
	public void validJsonString() {
		assertEquals(new JsonObject().putString("prop1", "value"),
				JsonHelper.getJsonFromString("{\"prop1\": \"value\"}"));
	}

}
