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
package net.bluemind.core.api.date;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class BmDateTime {

	@BMApi(version = "3")
	public enum Precision {
		Date, DateTime
	}

	public BmDateTime() {

	}

	/**
	 * ISO8601 date format : - yyyyMMddTHHmmss.SSSZ - yyyyMMddTHHmmss.SSS -
	 * yyyyMMdd The date timezone must match the {{@link #timezone} field.
	 */
	public String iso8601;

	/**
	 * Date timezone. Can be null or the date do not have timezone.
	 */
	public String timezone;

	/**
	 * Precision in the form of Date or DateTime
	 */
	public Precision precision;

	public BmDateTime(String iso8601, String timezone, Precision precision) {
		this.iso8601 = iso8601;
		this.timezone = timezone;
		this.precision = precision;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((iso8601 == null) ? 0 : iso8601.hashCode());
		result = prime * result + ((precision == null) ? 0 : precision.hashCode());
		result = prime * result + ((timezone == null) ? 0 : timezone.hashCode());
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
		BmDateTime other = (BmDateTime) obj;
		if (iso8601 == null) {
			if (other.iso8601 != null)
				return false;
		} else if (!iso8601.equals(other.iso8601))
			return false;
		if (precision != other.precision)
			return false;
		if (timezone == null) {
			if (other.timezone != null)
				return false;
		} else if (!timezone.equals(other.timezone))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ISO8601: " + iso8601 + ", Precision: " + precision.toString() + ", Timezone: " + timezone;
		// FIXME String.format not available in gwt
		// String.format("ISO8601: %s, Precision: %s, Timezone: %s",
		// iso8601, precision.toString(),
		// null == timezone ? "timezone not set" : timezone);
	}
}
