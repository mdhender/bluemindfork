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
package net.bluemind.eas.dto.device;

public final class DeviceId {
	private final String loginAtDomain;
	private final String identifier;
	private final String devType;
	private final String internalId;
	private final String uniqueIdentifier;

	/**
	 * @param loginAtDomain
	 * @param identifier
	 * @parem devType the device type (iPhone, SAMSUNG92000, etc)
	 * @param internalId the partnershipId
	 */
	public DeviceId(String loginAtDomain, String identifier, String devType, String internalId) {
		this.loginAtDomain = loginAtDomain;
		this.identifier = identifier;
		this.internalId = internalId;
		this.devType = devType;
		this.uniqueIdentifier = loginAtDomain + "::" + identifier;
	}

	public String getLoginAtDomain() {
		return loginAtDomain;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	public String getInternalId() {
		return internalId;
	}

	public String getType() {
		return devType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uniqueIdentifier == null) ? 0 : uniqueIdentifier.hashCode());
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
		DeviceId other = (DeviceId) obj;
		if (uniqueIdentifier == null) {
			if (other.uniqueIdentifier != null)
				return false;
		} else if (!uniqueIdentifier.equals(other.uniqueIdentifier))
			return false;
		return true;
	}

}
