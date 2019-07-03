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
package net.bluemind.core.commons.gwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import net.bluemind.core.api.Stream;

public class GwtSerDerUtils {

	public static class ByteSerDer implements GwtSerDer<Byte> {

		@Override
		public Byte deserialize(JSONValue jsonValue) {
			if (jsonValue != null && jsonValue.isNumber() != null) {
				return (byte) jsonValue.isNumber().doubleValue();
			}
			return null;
		}

		@Override
		public JSONValue serialize(Byte o) {
			if (o == null)
				return null;

			return new JSONNumber(o);
		}

	}

	public static ByteSerDer BYTE = new ByteSerDer();

	public static class BooleanSerDer implements GwtSerDer<Boolean> {

		@Override
		public Boolean deserialize(JSONValue jsonValue) {
			if (jsonValue != null && jsonValue.isBoolean() != null) {
				return jsonValue.isBoolean().booleanValue();
			}
			return null;
		}

		@Override
		public JSONValue serialize(Boolean o) {
			if (o == null)
				return null;

			return JSONBoolean.getInstance(o);
		}

	}

	public static BooleanSerDer BOOLEAN = new BooleanSerDer();

	public static class LongSerDer implements GwtSerDer<Long> {

		@Override
		public Long deserialize(JSONValue jsonValue) {
			if (jsonValue != null && jsonValue.isNumber() != null) {
				return Long.valueOf((long) jsonValue.isNumber().doubleValue());
			}
			return null;
		}

		@Override
		public JSONValue serialize(Long o) {
			if (o == null)
				return null;

			return new JSONNumber((o).doubleValue());
		}

	}

	public static LongSerDer LONG = new LongSerDer();

	public static class IntSerDer implements GwtSerDer<Integer> {

		@Override
		public Integer deserialize(JSONValue jsonValue) {
			if (jsonValue != null && jsonValue.isNumber() != null) {
				return Integer.valueOf((int) jsonValue.isNumber().doubleValue());
			}
			return null;
		}

		@Override
		public JSONValue serialize(Integer o) {
			if (o == null)
				return null;

			return new JSONNumber((o).doubleValue());
		}

	}

	public static IntSerDer INT = new IntSerDer();

	public static class StringSerDer implements GwtSerDer<String> {

		@Override
		public String deserialize(JSONValue jsonValue) {
			if (jsonValue != null && jsonValue.isString() != null) {
				return jsonValue.isString().stringValue();
			}
			return null;
		}

		@Override
		public JSONValue serialize(String o) {
			if (o == null)
				return null;

			return new JSONString(o);
		}

	}

	public static StringSerDer STRING = new StringSerDer();

	public static class DoubleSerDer implements GwtSerDer<Double> {

		@Override
		public Double deserialize(JSONValue jsonValue) {
			if (jsonValue != null && jsonValue.isNumber() != null) {
				return jsonValue.isNumber().doubleValue();
			}
			return null;
		}

		@Override
		public JSONValue serialize(Double o) {
			if (o == null)
				return null;

			return new JSONNumber(o);
		}

	}

	public static DoubleSerDer DOUBLE = new DoubleSerDer();

	public static class DateSerDer implements GwtSerDer<Date> {

		@Override
		public Date deserialize(JSONValue jsonValue) {
			if (jsonValue != null && jsonValue.isNumber() != null) {
				long time = (long) jsonValue.isNumber().doubleValue();
				return new Date(time);
			}
			return null;
		}

		@Override
		public JSONValue serialize(Date o) {
			if (o == null)
				return null;
			return new JSONNumber(o.getTime());
		}

	}

	public static DateSerDer DATE = new DateSerDer();

	public static class StreamSerDer implements GwtSerDer<Stream> {

		@Override
		public Stream deserialize(JSONValue jsonValue) {
			if (jsonValue != null && jsonValue.isNumber() != null) {
				return new GwtStream(jsonValue.toString());
			}
			return null;
		}

		@Override
		public JSONValue serialize(Stream o) {
			if (o == null)
				return null;

			return new JSONString(o.toString());
		}

	}

	public static StreamSerDer STREAM = new StreamSerDer();

	public static class ListSerDer<T> implements GwtSerDer<List<T>> {

		private GwtSerDer<T> elementSerDer;

		public ListSerDer(GwtSerDer<T> elementSerDer) {
			this.elementSerDer = elementSerDer;
		}

		@Override
		public List<T> deserialize(JSONValue json) {
			if (json != null && json.isArray() != null) {
				JSONArray array = json.isArray();

				List<T> ret = new ArrayList<T>(array.size());
				for (int i = 0; i < array.size(); i++) {
					ret.add(elementSerDer.deserialize(array.get(i)));
				}
				return ret;
			} else if (json == null) {
				GWT.log("try de deserialize something that is null");
			} else if (json.isArray() != null) {
				GWT.log("try de deserialize something that is not a array");
			}

			return null;
		}

		@Override
		public JSONValue serialize(List<T> o) {
			if (o == null) {
				return null;
			}

			JSONArray ret = new JSONArray();
			for (int i = 0; i < o.size(); i++) {
				T element = o.get(i);
				ret.set(i, elementSerDer.serialize(element));
			}

			return ret;
		}

	}

	public static class ByteArraySerDer implements GwtSerDer<byte[]> {

		public ByteArraySerDer() {
		}

		@Override
		public byte[] deserialize(JSONValue json) {
			if (json != null && json.isString() != null) {
				String value = json.isString().stringValue();
				return btoa(value).getBytes();
			}

			return null;
		}

		native String btoa(String b64)
		/*-{
			return btoa(b64);
		}-*/;

		native String atob(String encoded)
		/*-{
			return atob(encoded);
		}-*/;

		@Override
		public JSONValue serialize(byte[] o) {
			if (o == null) {
				return null;
			}

			String value = atob(new String(o));
			return new JSONString(value);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static class ArraySerDer<T> implements GwtSerDer<T[]> {

		private GwtSerDer<T> elementSerDer;
		private T[] zeroArray;

		public ArraySerDer(GwtSerDer<T> elementSerDer, T[] zeroArray) {
			this.elementSerDer = elementSerDer;
			this.zeroArray = zeroArray;
		}

		@Override
		public T[] deserialize(JSONValue json) {
			if (json != null && json.isArray() != null) {
				JSONArray array = json.isArray();

				List<T> ret = new ArrayList<T>(array.size());
				for (int i = 0; i < array.size(); i++) {
					ret.add(elementSerDer.deserialize(array.get(i)));
				}
				// FIXME need to now array type
				return ret.toArray(zeroArray);
			}

			return null;
		}

		@Override
		public JSONValue serialize(T[] o) {
			if (o == null) {
				return null;
			}

			JSONArray ret = new JSONArray();
			for (int i = 0; i < o.length; i++) {
				T element = o[i];
				ret.set(i, elementSerDer.serialize(element));
			}

			return ret;
		}
	}

	public static class SetSerDer<T> implements GwtSerDer<Set<T>> {

		private GwtSerDer<T> elementSerDer;

		public SetSerDer(GwtSerDer<T> elementSerDer) {
			this.elementSerDer = elementSerDer;
		}

		@Override
		public Set<T> deserialize(JSONValue json) {
			if (json != null && json.isArray() != null) {
				JSONArray array = json.isArray();

				Set<T> ret = new HashSet<>(array.size());
				for (int i = 0; i < array.size(); i++) {
					ret.add(elementSerDer.deserialize(array.get(i)));
				}
				return ret;
			}

			return null;
		}

		@Override
		public JSONValue serialize(Set<T> o) {
			if (o == null) {
				return null;
			}

			JSONArray ret = new JSONArray();
			Iterator<T> it = o.iterator();
			for (int i = 0; i < o.size(); i++) {
				T element = it.next();
				ret.set(i, elementSerDer.serialize(element));
			}

			return ret;
		}

	}

	public static class CollectionSerDer<T> implements GwtSerDer<Collection<T>> {

		private GwtSerDer<T> elementSerDer;

		public CollectionSerDer(GwtSerDer<T> elementSerDer) {
			this.elementSerDer = elementSerDer;
		}

		@Override
		public Collection<T> deserialize(JSONValue json) {
			if (json != null && json.isArray() != null) {
				JSONArray array = json.isArray();

				List<T> ret = new ArrayList<>(array.size());
				for (int i = 0; i < array.size(); i++) {
					ret.add(elementSerDer.deserialize(array.get(i)));
				}
				return ret;
			}

			return null;
		}

		@Override
		public JSONValue serialize(Collection<T> o) {
			if (o == null) {
				return null;
			}

			JSONArray ret = new JSONArray();
			Iterator<T> it = o.iterator();
			for (int i = 0; i < o.size(); i++) {
				T element = it.next();
				ret.set(i, elementSerDer.serialize(element));
			}

			return ret;
		}

	}

	public static class MapSerDer<V> implements GwtSerDer<Map<String, V>> {

		private GwtSerDer<String> keySerDer;
		private GwtSerDer<V> valueSerDef;

		public MapSerDer(GwtSerDer<String> elementSerDer, GwtSerDer<V> valueSerDer) {
			this.keySerDer = elementSerDer;
			this.valueSerDef = valueSerDer;
		}

		@Override
		public Map<String, V> deserialize(JSONValue json) {
			if (json != null && json.isObject() != null) {
				JSONObject array = json.isObject();

				Map<String, V> ret = new HashMap<>();
				for (String k : array.keySet()) {
					ret.put(k, valueSerDef.deserialize(array.get(k)));

				}
				return ret;
			}

			return null;
		}

		@Override
		public JSONValue serialize(Map<String, V> o) {
			if (o == null) {
				return null;
			}

			JSONObject ret = new JSONObject();
			for (String key : o.keySet()) {
				V value = o.get(key);
				if (value == null) {
					ret.put(key, JSONNull.getInstance());
				} else {
					ret.put(key, valueSerDef.serialize(value));
				}
			}

			return ret;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object deserializeEnum(Class klass, JSONValue jsonValue) {
		if (jsonValue != null && jsonValue.isString() != null) {

			return Enum.valueOf(klass, jsonValue.isString().stringValue());
		}
		return null;
	}

}
