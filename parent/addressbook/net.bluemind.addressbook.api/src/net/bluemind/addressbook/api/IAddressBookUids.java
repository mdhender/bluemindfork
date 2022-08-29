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
package net.bluemind.addressbook.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;

/**
 * Returns common addressbook container UIDs. All related entries are stored
 * within the container
 */
@BMApi(version = "3")
@Path("/addressbook/uids")
public interface IAddressBookUids {

	public static final String TYPE = "addressbook";

	/**
	 * Returns the default user addressbook UID
	 * 
	 * @param userUid
	 *                    the {@link net.bluemind.user.api.User} UID
	 * @return default user addressbook UID
	 */
	@GET
	@Path("{uid}/_default_addressbook")
	public default String getDefaultUserAddressbook(@PathParam("uid") String userUid) {
		return IAddressBookUids.defaultUserAddressbook(userUid);
	}

	/**
	 * Returns the UID of collected contacts
	 * 
	 * @param userUid
	 *                    the {@link net.bluemind.user.api.User} UID
	 * @return UID of collected contacts
	 */
	@GET
	@Path("{uid}/_collected_contacts")
	public default String getCollectedContactsUserAddressbook(@PathParam("uid") String userUid) {
		return IAddressBookUids.collectedContactsUserAddressbook(userUid);
	}

	/**
	 * Returns the UID of user-created addressbooks
	 * 
	 * @param uniqueUid
	 *                      A unique UID
	 * @return the UID of the user-created addressbook
	 */
	@GET
	@Path("{uid}/_other_addressbook")
	public default String getUserCreatedAddressbook(@PathParam("uid") String uniqueUid) {
		return IAddressBookUids.userCreatedAddressbook(uniqueUid);
	}

	/**
	 * Returns the UID of the domain addressbok
	 * 
	 * @param domainUid
	 *                      the {@link net.bluemind.domain.api.Domain}'s UID
	 * @return UID of the domain addressbok
	 */
	@GET
	@Path("{domain}/_vcards")
	public default String getUserVCards(@PathParam("domain") String domainUid) {
		return IAddressBookUids.userVCards(domainUid);
	}

	public static String defaultUserAddressbook(String userUid) {
		return "book:Contacts_" + userUid;
	}

	public static String collectedContactsUserAddressbook(String userUid) {
		return "book:CollectedContacts_" + userUid;
	}

	public static String userCreatedAddressbook(String randomSeed) {
		return "book:UserCreated_" + randomSeed;
	}

	public static String userVCards(String domainUid) {
		return "addressbook_" + domainUid;
	}

}
