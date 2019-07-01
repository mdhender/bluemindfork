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
package net.bluemind.cti.backend;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.cti.api.Status;
import net.bluemind.user.api.User;

public interface ICTIBackend {

	/**
	 * @param domain
	 * @param caller
	 * @param imSetPhonePresence
	 *            empty phone number == disabled
	 * @throws ServerFault
	 */
	void forward(String domain, ItemValue<User> caller, String imSetPhonePresence) throws ServerFault;

	void dnd(String domain, ItemValue<User> caller, boolean dndEnabled) throws ServerFault;

	void dial(String domain, ItemValue<User> caller, String number) throws ServerFault;

	Status.PhoneState getPhoneState(String domain, ItemValue<User> caller) throws ServerFault;

}
