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
package net.bluemind.core.api;

import net.bluemind.core.api.fault.ServerFault;

/**
 * This class represents a version number using the
 * <code>major.minor.release</code> format.
 */
@BMApi(version = "3")
public class VersionInfo {

	/**
	 * the major, eg. major.0.3456
	 */
	public String major;
	/**
	 * the minor, eg. 1.minor.3456
	 */
	public String minor;
	/**
	 * the release, eg. 1.0.release
	 *
	 * there can be a SNAPSHOT timestamp
	 * for non-release build (eg. 4.1.0.202004021209)
	 * It is ignored
	 */
	public String release;

	public String displayName;

	public static VersionInfo checkAndCreate(String version) throws ServerFault {
		String[] v = version.split("\\.");
		if (v.length < 3 || v.length > 4){
			throw new ServerFault("version " + version + " is not parseable");
		}
		VersionInfo vi = new VersionInfo();
		vi.major = v[0];
		vi.minor = v[1];
		vi.release = v[2];
		if (!vi.valid()) {
			throw new ServerFault("version " + version + " is not valid");
		}

		return vi;
	}

	public static VersionInfo create(String version, String displayName) {
		VersionInfo vi = new VersionInfo();
		String[] v = version.split("\\.");
		vi.major = v[0];
		vi.minor = v[1];
		vi.release = v[2];
		vi.displayName = displayName;
		return vi;
	}

	public static VersionInfo create(String version) {
		return create(version, version);
	}

	public boolean valid() {
		try {
			int iMajor = Integer.parseInt(major);
			int iMinor = Integer.parseInt(minor);
			int iRelease = Integer.parseInt(release);
			if (iMajor >= 0 && iMinor >= 0 && iRelease >= 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public boolean greaterThan(VersionInfo version) {
		Integer iMajor = Integer.parseInt(major);
		Integer iMinor = Integer.parseInt(minor);
		Integer iRelease = Integer.parseInt(release);

		Integer vMajor = Integer.parseInt(version.major);
		Integer vMinor = Integer.parseInt(version.minor);
		Integer vRelease = Integer.parseInt(version.release);

		int major = iMajor.compareTo(vMajor);
		if (major < 0) {
			return false;
		} else if (major > 0) {
			return true;
		}

		int minor = iMinor.compareTo(vMinor);
		if (minor < 0) {
			return false;
		} else if (minor > 0) {
			return true;
		}

		int release = iRelease.compareTo(vRelease);
		if (release >= 0) {
			return true;
		}

		return false;
	}

	public boolean greaterThanOrEquals(VersionInfo version) {
		if (greaterThan(version) || equals(version)) {
			return true;
		}
		return false;
	}


	/**
	 * Return if the current {@link VersionInfo} describe an edge or a stable
	 * version. Stable version should have minor part == 0.
	 * 
	 * @return True if this object describe a stable version, false if it's an
	 *         edge.
	 */
	public boolean stable() {
		return minor.equals("0");
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + release;
	}

	public String fullString() {
		return (displayName != null ? displayName + " - " : "") + major + "." + minor + "." + release;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((major == null) ? 0 : major.hashCode());
		result = prime * result + ((minor == null) ? 0 : minor.hashCode());
		result = prime * result + ((release == null) ? 0 : release.hashCode());
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
		VersionInfo other = (VersionInfo) obj;
		if (major == null) {
			if (other.major != null)
				return false;
		} else if (!major.equals(other.major))
			return false;
		if (minor == null) {
			if (other.minor != null)
				return false;
		} else if (!minor.equals(other.minor))
			return false;
		if (release == null) {
			if (other.release != null)
				return false;
		} else if (!release.equals(other.release))
			return false;
		return true;
	}

}
