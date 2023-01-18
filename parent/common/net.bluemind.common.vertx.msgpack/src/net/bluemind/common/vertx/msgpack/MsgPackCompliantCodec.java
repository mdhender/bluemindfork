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

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.netty.handler.codec.EncoderException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class MsgPackCompliantCodec implements MessageCodec<IMsgpackCompliant, JsonObject> {

	private ObjectMapper mapper;
	private ObjectReader reader;

	public MsgPackCompliantCodec() {
		this.mapper = MsgPackDatabindCodec.mapper();
		this.reader = mapper.readerFor(Map.class);
	}

	@Override
	public void encodeToWire(Buffer buffer, IMsgpackCompliant s) {
		try {
			byte[] content = mapper.writeValueAsBytes(s);
			buffer.appendInt(content.length);
			buffer.appendBytes(content);
		} catch (JsonProcessingException e) {
			throw new EncoderException(e);
		}

	}

	@Override
	public JsonObject decodeFromWire(int pos, Buffer buffer) {
		int length = buffer.getInt(pos);
		pos += 4;
		byte[] content = buffer.slice(pos, pos + length).getBytes();
		try {
			return new JsonObject(reader.<Map<String, Object>>readValue(content));
		} catch (IOException e) {
			throw new EncoderException(e);
		}
	}

	@Override
	public JsonObject transform(IMsgpackCompliant s) {
		return s.asJson();
	}

	@Override
	public String name() {
		return "MsgpackCompliant";
	}

	@Override
	public byte systemCodecID() {
		return -1;
	}

}
