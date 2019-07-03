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

import java.util.Set;

@BMApi(version = "3")
public class Email {
	public String address;
	public boolean allAliases;
	public boolean isDefault;

	public static Email create(String address, boolean isDefault) {
		return create(address, isDefault, false);
	}

	public static Email create(String address, boolean isDefault, boolean allAliases) {
		Email e = new Email();
		e.address = address;
		e.isDefault = isDefault;
		e.allAliases = allAliases;
		return e;
	}

	@Override
	public String toString() {
		return address.toLowerCase();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + (allAliases ? 1231 : 1237);
		result = prime * result + (isDefault ? 1231 : 1237);
		return result;
	}

	public String localPart() {
		return address.split("@")[0];
	}

	public String domainPart() {
		return address.split("@")[1];
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Email other = (Email) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (allAliases != other.allAliases)
			return false;
		if (isDefault != other.isDefault)
			return false;
		return true;
	}

	public boolean match(String mailto, Set<String> domainNames) {

		if (allAliases) {
			String[] s = mailto.split("@");
			String left = s[0];
			String right = s[1];

			String[] t = address.split("@");
			String tleft = t[0];

			if (tleft.equals(left)) {
				return domainNames.contains(right);
			} else {
				return false;
			}
		} else {
			return address.equals(mailto);
		}

	}

}
