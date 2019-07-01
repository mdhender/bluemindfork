/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.directory.hollow.datamodel;

import java.util.Set;

public class OfflineAddressBook {

	public String domainName;
	public Set<String> domainAliases;

	/**
	 * Display name of the address list. Can change between generation versions of
	 * the same address list.
	 */
	public String name;

	/**
	 * The AddressList-X500-DN of the address list container object. Can change
	 * between generation versions of the same address list. MUST contain Teletex
	 * characters only, as specified by the non-space-teletex rule in section 2.1.
	 */
	public String distinguishedName;

	/**
	 * The sequence number of the OAB. This number increases by one between
	 * generation versions of the same address list.
	 */
	public int sequence;

	/**
	 *
	 * A string formatted GUID that represents the address list container object.
	 * This value never changes between generation versions of the same address
	 * list. This value is formatted as "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx". MUST
	 * contain Teletex characters only, as specified by the non-space-teletex rule
	 * in section 2.1
	 */
	public String containerGuid;

	/**
	 * optional.
	 * 
	 * DN for the root departmental group in the department hierarchy for the
	 * organization. The DN (3) can change between generation versions of the same
	 * address list. MUST contain Teletex characters only, as specified by the
	 * non-space-teletex rule in section 2.1.
	 * 
	 */
	public String hierarchicalRootDepartment;

}
