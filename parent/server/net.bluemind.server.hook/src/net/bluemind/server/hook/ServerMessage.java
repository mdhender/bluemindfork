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

package net.bluemind.server.hook;

import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.server.api.Server;

/**
 * Provides some context where Server are created, updated or deleted (including
 * assignments).
 *
 */
public final class ServerMessage {

	public SecurityContext securityContext;
	public ContainerDescriptor installation;

	/**
	 * the value of the server (the previous value for update or delete events)
	 */
	public ItemValue<Server> server;

	/**
	 * This is defined on tag, untagged and assignments operations
	 */
	public String tag;

	public String assignedDomain;

}
