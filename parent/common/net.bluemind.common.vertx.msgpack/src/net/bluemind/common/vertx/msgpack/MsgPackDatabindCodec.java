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
package net.bluemind.common.vertx.msgpack;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MsgPackDatabindCodec {

	private MsgPackDatabindCodec() {
	}

	private static final ObjectMapper msgpackMapper = buildMapper();

	/**
	 * @return an object mapper capable of serializing {@link JsonObject} and
	 *         {@link JsonArray} to an msgpack blob.
	 */
	public static final ObjectMapper mapper() {
		return msgpackMapper;
	}

	private static ObjectMapper buildMapper() {
		ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		mapper.registerModule(new MsgPackVertxModule());
		mapper.registerModule(new AfterburnerModule().setUseValueClassLoader(false));
		return mapper;
	}

}
