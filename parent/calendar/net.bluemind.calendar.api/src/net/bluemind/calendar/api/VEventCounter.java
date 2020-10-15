/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.calendar.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class VEventCounter {

	public CounterOriginator originator;
	public VEventOccurrence counter;

	public String id() {
		String evtId = counter.recurid == null ? "0" : counter.recurid.iso8601;
		String cn = originator.commonName == null ? "" : originator.commonName;
		String email = originator.email == null ? "" : originator.email;
		return String.format("%s#%s#%s", cn, email, evtId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id().hashCode();
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
		VEventCounter other = (VEventCounter) obj;
		return id().equals(other.id());
	}

	@BMApi(version = "3")
	public static class CounterOriginator {

		public String commonName;
		public String email;

		public static CounterOriginator from(String commonName, String email) {
			CounterOriginator originator = new CounterOriginator();
			originator.commonName = commonName;
			originator.email = email;
			return originator;
		}

	}
}
