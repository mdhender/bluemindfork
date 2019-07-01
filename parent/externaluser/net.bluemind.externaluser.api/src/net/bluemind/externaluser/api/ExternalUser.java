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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.externaluser.api;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.BMApi;
import net.bluemind.directory.api.DirBaseValue;

/**
 * ExternalUser is mainly used to be able to add an external email to a group.
 * 
 * External users are part of a domain (and only one). For the same domain, you
 * can't create two external users with the same email.
 */
@BMApi(version = "3")
public class ExternalUser extends DirBaseValue {

	/**
	 * { @link net.bluemind.addressbook.api.VCard } contact informations for the
	 * external user
	 */
	public VCard contactInfos;

}
