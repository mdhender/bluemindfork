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
public class AuditLogQuery {

	public String domainUid;
	public String author;
	public String with;
	public String description;
	public String container;
	public Date from;
	public Date to;
	public String key;
	public String logtype;
	public int size = 10;

	public AuditLogQuery() {

	}

	public AuditLogQuery(LogMailQueryBuilder builder) {
		domainUid = builder.domainUid;
		author = builder.author;
		with = builder.with;
		description = builder.description;
		container = builder.container;
		from = builder.from;
		to = builder.to;
		key = builder.key;
		logtype = builder.logtype;
		size = builder.size;
	}

	public class LogMailQueryBuilder {
		public String domainUid;
		String author;
		String with;
		String description;
		String container;
		Date from;
		Date to;
		String key;
		String logtype;
		int size;

		public LogMailQueryBuilder domainUid(String d) {
			domainUid = d;
			return this;
		}

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

		public LogMailQueryBuilder from(Date f) {
			from = f;
			return this;
		}

		public LogMailQueryBuilder to(Date t) {
			to = t;
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

		public LogMailQueryBuilder size(int s) {
			size = s;
			return this;
		}

		public AuditLogQuery build() {
			return new AuditLogQuery(this);
		}

	}

	@Override
	public String toString() {
		return "domainUid: " + domainUid + " ,logtype: " + logtype + " ,author: " + author + ",with: " + with
				+ " ,description: " + description + " ,key:" + key + " ,from: " + from + " ,to: " + to + " ,size: "
				+ size;
	}

}
