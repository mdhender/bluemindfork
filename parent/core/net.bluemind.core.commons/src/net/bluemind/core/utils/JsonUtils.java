/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.utils;

import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.core.api.fault.ServerFault;

public class JsonUtils {
	private static final ObjectMapper objectMapper;

	static {
		objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		SimpleModule module = new SimpleModule();
		module.addSerializer(java.sql.Date.class, new DateSerializer());
		objectMapper.registerModule(module);
		objectMapper.registerModule(new AfterburnerModule().setUseValueClassLoader(false));
	}

	private JsonUtils() {
	}

	public static Object read(String value, Type type) throws Exception {
		JavaType typ = objectMapper.getTypeFactory().constructType(type);

		return objectMapper.readerFor(typ).readValue(value);
	}

	public static final class ValueReader<T> {
		private ObjectReader impl;

		private ValueReader(ObjectReader r) {
			this.impl = r;
		}

		public T read(String v) {
			try {
				return impl.readValue(v);
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		}
	}

	public static <T> ValueReader<T> reader(Class<T> type) {
		return new ValueReader<>(objectMapper.readerFor(type));
	}

	public static <T> T read(String value, Class<T> type) {
		try {
			return objectMapper.readValue(value, type);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	public static final class ListReader<T> {
		private final CollectionType listType;

		ListReader(Class<T> elemType) {
			this.listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elemType);
		}

		public List<T> read(String value) {
			try {
				return objectMapper.readValue(value, listType);
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		}
	}

	public static <T> ListReader<T> listReader(Class<T> type) {
		return new ListReader<>(type);
	}

	public static <T> List<T> readSome(String value, Class<T> type) {
		try {
			return objectMapper.readValue(value,
					objectMapper.getTypeFactory().constructCollectionType(List.class, type));
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	public static String asString(Object o) {
		try {
			return objectMapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			throw new ServerFault(e);
		}
	}

	public static ByteBuf asBuffer(Object o) {
		return Unpooled.wrappedBuffer(asBytes(o));
	}

	public static byte[] asBytes(Object o) {
		try {
			return objectMapper.writeValueAsBytes(o);
		} catch (JsonProcessingException e) {
			throw new ServerFault(e);
		}
	}

}
