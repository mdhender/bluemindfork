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

package net.bluemind.directory.hollow.datamodel;

import com.netflix.hollow.core.write.objectmapper.HollowInline;

public class Email {

	@HollowInline
	public String address;

	public boolean allAliases;
	public boolean isDefault;

	public static Email create(String address, boolean isDefault, boolean allAliases) {
		Email email = new Email();
		email.address = address;
		email.isDefault = isDefault;
		email.allAliases = allAliases;
		return email;
	}

}