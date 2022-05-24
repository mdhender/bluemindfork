package net.bluemind.core.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

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
public final class Columns {

	public List<Column> cols;

	private final Supplier<String> caching = Suppliers.memoize(this::namesImpl);

	public static final class Column {
		public String name;
		public String enumType;

		public Column(String n, String et) {
			name = n;
			enumType = et;
		}
	}

	private Columns() {
		cols = new ArrayList<Columns.Column>();
	}

	public static Columns create() {
		Columns cols = new Columns();
		return cols;
	}

	public Columns col(String name) {
		cols.add(new Column(name, null));
		return this;
	}

	public Columns cols(Columns all) {
		cols.addAll(all.cols);
		return this;
	}

	public Columns col(String name, String enumType) {
		cols.add(new Column(name, enumType));
		return this;
	}

	public void appendNames(String prefix, StringBuilder query) {
		boolean first = true;
		for (Column c : cols) {
			if (!first) {
				query.append(", ");
			}

			if (prefix != null) {
				query.append(prefix).append(".");
			}
			query.append(c.name);
			first = false;
		}

	}

	private String namesImpl() {
		StringBuilder sb = new StringBuilder();
		appendNames(null, sb);
		return sb.toString();
	}

	public String names(String prefix) {
		return caching.get();
	}

	public void appendValues(StringBuilder query) {
		boolean first = true;
		for (Column c : cols) {
			if (!first) {
				query.append(',');
			}

			query.append("?");
			if (c.enumType != null) {
				query.append("::").append(c.enumType);
			}
			first = false;
		}
	}

	public String values() {
		StringBuilder sb = new StringBuilder();
		appendValues(sb);
		return sb.toString();
	}

}
