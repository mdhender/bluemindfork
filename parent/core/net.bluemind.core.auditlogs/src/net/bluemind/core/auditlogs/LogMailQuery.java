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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.auditlogs;

import java.util.Date;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class LogMailQuery {

	public String author;

	public String with;

	public String description;

	// TODO SCL : bonne valeur ?
	public String container;

	public Date timestamp;

	public String key;

	public String logtype;

	public LogMailQuery() {

	}

	public LogMailQuery(LogMailQueryBuilder builder) {
		author = builder.author;
		with = builder.with;
		description = builder.description;
		container = builder.container;
		timestamp = builder.timestamp;
		key = builder.key;
		logtype = builder.logtype;
	}

	public class LogMailQueryBuilder {
		String author;
		String with;
		String description;
		String container;
		Date timestamp;
		String key;
		String logtype;

		public LogMailQueryBuilder author(String a) {
			author = a;
			return this;
		}

		public LogMailQueryBuilder with(String w) {
			with = w;
			return this;
		}

		public LogMailQueryBuilder description(String d) {
			description = d;
			return this;
		}

		public LogMailQueryBuilder container(String c) {
			container = c;
			return this;
		}

		public LogMailQueryBuilder timestamp(Date t) {
			timestamp = t;
			return this;
		}

		public LogMailQueryBuilder key(String k) {
			key = k;
			return this;
		}

		public LogMailQueryBuilder logtype(String l) {
			logtype = l;
			return this;
		}

		public LogMailQuery build() {
			return new LogMailQuery(this);
		}

	}

}
