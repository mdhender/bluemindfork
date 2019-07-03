/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.directory.hollow.datamodel.consumer;

import java.util.List;

import com.google.common.base.MoreObjects;

public class Query {

	public enum QueryType {
		VALUE, AND, OR
	}

	public final QueryType type;
	public final List<Query> children;
	public final String key;
	public final String value;

	private Query(QueryType type, List<Query> children, String key, String value) {
		this.type = type;
		this.children = children;
		this.key = key;
		this.value = value;
	}

	public static Query contentQuery(String key, String value) {
		return new Query(QueryType.VALUE, null, key, value);
	}

	public static Query andQuery(List<Query> children) {
		return new Query(QueryType.AND, children, null, null);
	}

	public static Query orQuery(List<Query> children) {
		return new Query(QueryType.OR, children, null, null);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(Query.class)//
				.add("type", type)//
				.add("key", key)//
				.add("value", value)//
				.add("children", children)//
				.toString();
	}
}
