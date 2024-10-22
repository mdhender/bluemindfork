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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.ldap.importation.internal.tools;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.ldap.importation.api.LdapConstants;

public class LdapUuidMapper extends UuidMapper {
	/**
	 * @param uuid ldap entryUUID attribute
	 */
	public LdapUuidMapper(String uuid) {
		super(uuid);
	}

	public static UuidMapper fromEntry(String attributeName, Entry entry) {
		if (attributeName == null || attributeName.isEmpty()) {
			throw new ServerFault("Null or empty attribute name", ErrorCode.INVALID_PARAMETER);
		}

		if (!entry.containsAttribute(attributeName)) {
			throw new ServerFault("No attribute: " + attributeName + " in entry: " + entry.getDn().getName(),
					ErrorCode.INVALID_PARAMETER);
		}

		String uuidVal;
		try {
			uuidVal = entry.get(attributeName).getString();
		} catch (LdapInvalidAttributeValueException e) {
			throw new ServerFault(
					"Unable to get attribute: " + attributeName + " from entry: " + entry.getDn().getName(), e);
		}

		return new LdapUuidMapper(uuidVal);
	}

	/**
	 * Get UuidMapper from BlueMind external ID
	 * 
	 * @param extId BlueMind external ID
	 * @return
	 */
	public static Optional<UuidMapper> fromExtId(String extId) {
		if (extId == null || !extId.startsWith(LdapConstants.EXTID_PREFIX)) {
			return Optional.empty();
		}

		String guid = extId.replaceFirst(LdapConstants.EXTID_PREFIX, "");
		if (guid.trim().isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(new LdapUuidMapper(guid));
	}

	/**
	 * Get UuidMapper from a list of BlueMind external ID
	 * 
	 * @param extIds BlueMind external ID list
	 * @return
	 */
	public static Set<UuidMapper> fromExtIdList(Set<String> extIds) {
		return extIds.stream().map(extId -> LdapUuidMapper.fromExtId(extId)).filter(Optional::isPresent)
				.map(Optional::get).collect(Collectors.toSet());
	}

	@Override
	public String getExtId() {
		return LdapConstants.EXTID_PREFIX + uuid;
	}

	@Override
	public String getGuid() {
		return uuid;
	}
}
