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
package net.bluemind.core.container.api;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.acl.AccessControlEntry;

/**
 * Internal management container
 */
public interface IInternalContainerManagement extends IContainerManagement {

	/**
	 * Same as {@link IContainerManagement.ContainerDescriptor} but don't send email
	 * notification if sendNotification is false
	 * 
	 * @param entries
	 * @param skipNotification
	 * @throws ServerFault
	 */
	void setAccessControlList(List<AccessControlEntry> entries, boolean sendNotification) throws ServerFault;

}
