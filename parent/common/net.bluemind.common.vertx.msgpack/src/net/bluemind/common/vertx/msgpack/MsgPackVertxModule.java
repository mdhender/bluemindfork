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

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@SuppressWarnings("serial")
public class MsgPackVertxModule extends SimpleModule {

	class JsonObjectSerializer extends JsonSerializer<JsonObject> {
		@Override
		public void serialize(JsonObject value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeObject(value.getMap());
		}
	}

	class MsgpackCompliantSerializer extends JsonSerializer<IMsgpackCompliant> {
		@Override
		public void serialize(IMsgpackCompliant value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException {
			jgen.writeObject(value.getMap());
		}
	}

	class JsonArraySerializer extends JsonSerializer<JsonArray> {
		@Override
		public void serialize(JsonArray value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeObject(value.getList());
		}
	}

	class ByteArraySerializer extends JsonSerializer<byte[]> {

		@Override
		public void serialize(byte[] value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeBinary(value);
		}
	}

	class ByteArrayDeserializer extends JsonDeserializer<byte[]> {

		@Override
		public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			return p.getBinaryValue();
		}
	}

	class BufferSerializer extends JsonSerializer<Buffer> {

		@Override
		public void serialize(Buffer value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeBinary(value.getBytes());
		}
	}

	class BufferDeserializer extends JsonDeserializer<Buffer> {

		@Override
		public Buffer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			return Buffer.buffer(p.getBinaryValue());
		}
	}

	class InstantSerializer extends JsonSerializer<Instant> {
		@Override
		public void serialize(Instant value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeString(ISO_INSTANT.format(value));
		}

	}

	class InstantDeserializer extends JsonDeserializer<Instant> {
		@Override
		public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			String text = p.getText();
			try {
				return Instant.from(ISO_INSTANT.parse(text));
			} catch (DateTimeException e) {
				throw new InvalidFormatException(p, "Expected an ISO 8601 formatted date time", text, Instant.class);
			}
		}
	}

	public MsgPackVertxModule() {
		// custom types
		addSerializer(IMsgpackCompliant.class, new MsgpackCompliantSerializer());
		addSerializer(JsonObject.class, new JsonObjectSerializer());
		addSerializer(JsonArray.class, new JsonArraySerializer());
		// he have 2 extensions: RFC-7493
		addSerializer(Instant.class, new InstantSerializer());
		addDeserializer(Instant.class, new InstantDeserializer());
		addSerializer(byte[].class, new ByteArraySerializer());
		addDeserializer(byte[].class, new ByteArrayDeserializer());
		addSerializer(Buffer.class, new BufferSerializer());
		addDeserializer(Buffer.class, new BufferDeserializer());
	}

}
