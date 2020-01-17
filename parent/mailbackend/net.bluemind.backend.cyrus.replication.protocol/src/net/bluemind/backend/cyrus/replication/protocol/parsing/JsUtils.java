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
package net.bluemind.backend.cyrus.replication.protocol.parsing;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import io.vertx.core.json.JsonArray;

public final class JsUtils {

	/**
	 * Converts an array of enum-based strings to a set. If array is null, returns
	 * an empty set.
	 * 
	 * @param enumKlass
	 * @param array
	 * @return
	 */
	public static <E extends Enum<E>> EnumSet<E> asSet(Class<E> enumKlass, JsonArray array) {
		if (array == null) {
			return EnumSet.noneOf(enumKlass);
		}
		int len = array.size();
		if (len == 0) {
			return EnumSet.noneOf(enumKlass);
		}
		ArrayList<E> asList = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			String s = array.getString(i);
			E value = E.valueOf(enumKlass, s);
			asList.add(value);
		}
		return EnumSet.copyOf(asList);

	}

	/**
	 * Converts an array of strings to a set.
	 * 
	 * Returns an empty set if array is null
	 * 
	 * @param array
	 * @return
	 */
	public static Set<String> asSet(JsonArray array) {
		Set<String> asList = new HashSet<>();
		if (array == null) {
			return asList;
		}
		int len = array.size();
		for (int i = 0; i < len; i++) {
			String s = array.getString(i);
			asList.add(s);
		}
		return asList;
	}

	/**
	 * T is the type of an element in the json array
	 * 
	 * @param array
	 * @param f
	 * @return
	 */
	public static <T, R> List<R> asList(JsonArray array, Function<T, R> f) {
		int len = array.size();
		List<R> ret = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			T val = (T) array.getValue(i);
			ret.add(f.apply(val));
		}
		return ret;
	}

	/**
	 * T is the type of an element in the array
	 * 
	 * @param array
	 * @param f
	 * @return
	 */
	public static <T, R> R[] asArray(JsonArray array, Class<R> klass, Function<T, R> f) {
		int len = array.size();
		List<R> ret = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			T val = (T) array.getValue(i);
			ret.add(f.apply(val));
		}
		@SuppressWarnings("unchecked")
		R[] retArray = (R[]) Array.newInstance(klass, len);
		return ret.toArray(retArray);
	}

	public static <K, V> Map<K, V> index(JsonArray array, Function<V, K> keyForArrayValue) {
		Map<K, V> indexed = new HashMap<>();
		if (array == null) {
			return indexed;
		}
		int len = array.size();
		for (int i = 0; i < len; i++) {
			V val = (V) array.getValue(i);
			K key = keyForArrayValue.apply(val);
			indexed.put(key, val);
		}
		return indexed;
	}

	public static JsonArray toArray(String... strings) {
		JsonArray ret = new JsonArray();
		for (String s : strings) {
			ret.add(s);
		}
		return ret;
	}

	public static <T, R> JsonArray toArray(Collection<T> items, Function<T, R> toArrayItem) {
		JsonArray ret = new JsonArray();
		items.forEach(t -> ret.add(toArrayItem.apply(t)));
		return ret;
	}

	public static <V> void forEach(JsonArray array, Consumer<V> toApply) {
		forEach(array, (V a, Integer b) -> toApply.accept(a));
	}

	public static <V> void forEach(JsonArray array, BiConsumer<V, Integer> toApply) {
		int len = array.size();
		for (int i = 0; i < len; i++) {
			toApply.accept((V) array.getValue(i), i);
		}
	}

}
