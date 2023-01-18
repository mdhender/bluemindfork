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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import net.bluemind.common.vertx.msgpack.MsgPackDatabindCodec;

public class MsgpackDatabindCodecTests {

	ObjectMapper mapper;
	ObjectReader reader;
	ObjectMapper vertxMapper;
	ObjectReader vxReader;

	@Before
	public void before() {
		mapper = MsgPackDatabindCodec.mapper();
		reader = mapper.readerFor(Map.class);
		vertxMapper = DatabindCodec.mapper();
		vxReader = vertxMapper.readerFor(Map.class);
	}

	@Test
	public void testMapJsonObjects() throws IOException {
		JsonObject js = new JsonObject();
		js.put("type", "fille.du.bedouin");
		js.put("emptyArray", new JsonArray());
		js.put("handles", new JsonArray().add(1).add(23).add(42));
		byte[] randBytes = new byte[128];
		ThreadLocalRandom.current().nextBytes(randBytes);
		js.put("randBytes", randBytes);

		System.err.println("BEFORE: " + js.encodePrettily());

		byte[] content = mapper.writeValueAsBytes(js);
		byte[] vxContent = vertxMapper.writeValueAsBytes(js);
		System.err.println("msgpack: " + content.length + " vxClassic: " + vxContent.length);
		assertTrue("msgpack binary must be small than std json encoding", content.length < vxContent.length);

		Map<String, Object> nodes = reader.readValue(content);
		JsonObject msgPackRead = new JsonObject(nodes);
		System.err.println("AFTER: " + msgPackRead.encodePrettily());
		assertEquals(js.encode(), msgPackRead.encode());

	}

}
