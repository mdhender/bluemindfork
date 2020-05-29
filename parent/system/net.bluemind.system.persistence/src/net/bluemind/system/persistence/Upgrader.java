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
package net.bluemind.system.persistence;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.google.common.base.Strings;

import net.bluemind.system.api.Database;

public class Upgrader implements Comparable<Upgrader> {
	public UpgradePhase phase;
	public String server;
	public Database database;
	public String upgraderId;
	public boolean success;
	private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

	public Upgrader() {
	}

	public Upgrader phase(UpgradePhase phase) {
		this.phase = phase;
		return this;
	}

	public Upgrader database(Database database) {
		this.database = database;
		return this;
	}

	public Upgrader server(String server) {
		this.server = server;
		return this;
	}

	public Upgrader success(boolean success) {
		this.success = success;
		return this;
	}

	public Upgrader upgraderId(Date date, int sequence) {
		this.upgraderId = toId(date, sequence);
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((upgraderId == null) ? 0 : upgraderId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Upgrader other = (Upgrader) obj;
		if (upgraderId == null) {
			if (other.upgraderId != null)
				return false;
		} else if (!upgraderId.equals(other.upgraderId))
			return false;
		return true;
	}

	public static enum UpgradePhase {
		SCHEMA_UPGRADE, POST_SCHEMA_UPGRADE
	}

	@Override
	public int compareTo(Upgrader o) {
		return upgraderId.compareTo(o.upgraderId);
	}

	public static String toId(Date date, int sequence) {
		if (date instanceof java.sql.Date) {
			date = new java.util.Date(date.getTime());
		}
		return formatter.format(date.toInstant().atZone(ZoneId.of("UTC"))) + Strings.padStart("" + sequence, 10, '0');
	}

}
