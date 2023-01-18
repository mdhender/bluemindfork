/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.common.vertx.msgpack.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class AbstractCodecPerfTests {

	private static final int benchLoop = 1_000_000;
	private static final int binarySize = 2048;

	ObjectMapper mapper;
	ObjectWriter writer;
	ObjectReader reader;

	@Before
	public void before() {
		mapper = mapper();
		writer = mapper.writerFor(JsonObject.class);
		reader = mapper.readerFor(Map.class);
	}

	protected abstract ObjectMapper mapper();

	@Test
	public void testMapJsonObjects() throws IOException {
		JsonObject js = new JsonObject();
		js.put("type", "fille.du.bedouin");
		js.put("emptyArray", new JsonArray());
		js.put("handles", new JsonArray().add(1).add(23).add(42));
		byte[] randBytes = new byte[binarySize];
		ThreadLocalRandom.current().nextBytes(randBytes);
		js.put("randBytes", randBytes);

		System.err.println("BEFORE: " + js.encodePrettily());

		byte[] vxContent = writer.writeValueAsBytes(js);

		Map<String, Object> nodes = reader.readValue(vxContent);
		JsonObject reRead = new JsonObject(nodes);
		System.err.println("AFTER: " + reRead.encodePrettily());
		assertEquals(js.encode(), reRead.encode());

		// warm it up
		int loop = 500_000;
		for (int i = 0; i < loop; i++) {
			JsonObject vxRead = vxTransfo(js);
			assertEquals(vxRead.encode(), js.encode());
		}

		// test it
		long time = System.currentTimeMillis();
		for (int i = 0; i < benchLoop; i++) {
			JsonObject vxRead = vxTransfo(js);
			assertNotNull(vxRead);
		}
		System.err.println("vx: " + (System.currentTimeMillis() - time) + "ms.");

	}

	private JsonObject vxTransfo(JsonObject js) throws JsonProcessingException, IOException {
		byte[] vx = writer.writeValueAsBytes(js);
		JsonObject vxRead = new JsonObject(reader.<Map<String, Object>>readValue(vx));
		return vxRead;
	}

}
