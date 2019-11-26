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

public class SchemaVersion implements Comparable<SchemaVersion> {
	public int major;
	public int build;
	public UpgradePhase phase;
	public String component;
	public boolean success;

	public SchemaVersion(int major, int build) {
		this.major = major;
		this.build = build;
	}

	public SchemaVersion(String version) {
		this.major = Integer.parseInt(version.substring(0, version.indexOf(".")));
		this.build = Integer.parseInt(version.substring(version.indexOf(".") + 1));
	}

	public SchemaVersion() {
	}

	public SchemaVersion phase(UpgradePhase phase) {
		this.phase = phase;
		return this;
	}

	public SchemaVersion component(String component) {
		this.component = component;
		return this;
	}

	public SchemaVersion success(boolean success) {
		this.success = success;
		return this;
	}

	public void fromDbSchemaversion(long dbSchemaVersion) {
		String version = String.valueOf(dbSchemaVersion);
		while (version.length() < 12) {
			version = "0" + version;
		}
		this.major = Integer.parseInt(version.substring(0, 3).replaceFirst("^0+(?!$)", ""));
		this.build = Integer.parseInt(version.substring(3, 12).replaceFirst("^0+(?!$)", ""));
	}

	public long toDbSchemaVersion() {
		return Long.parseLong(String.format("%03d%09d", major, build));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + build;
		result = prime * result + ((component == null) ? 0 : component.hashCode());
		result = prime * result + major;
		result = prime * result + phase.hashCode();
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
		SchemaVersion other = (SchemaVersion) obj;
		if (build != other.build)
			return false;
		if (component == null) {
			if (other.component != null)
				return false;
		} else if (!component.equals(other.component))
			return false;
		if (major != other.major)
			return false;
		if (phase != other.phase)
			return false;
		return true;
	}

	@Override
	public int compareTo(SchemaVersion o) {
		int phase = Integer.compare(this.phase.ordinal(), o.phase.ordinal());
		if (phase != 0) {
			return phase;
		}
		return Long.compare(this.toDbSchemaVersion(), o.toDbSchemaVersion());
	}

	public static enum UpgradePhase {
		SCHEMA_UPGRADE, POST_SCHEMA_UPGRADE
	}

}
