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
package net.bluemind.node.api;

import net.bluemind.core.api.fault.ServerFault;

/**
 * Factory for creating a {@link INodeClient} to local or remote a {@link Host}.
 * 
 * @author tom
 * 
 */
public interface INodeClientFactory {

	/**
	 * Fetches a client to manipulate files & run commands on the BJ {@link Host}
	 * with the given IP address
	 * 
	 * @param hostIpAddress
	 * @return a usable client
	 * @throws ServerFault
	 */
	INodeClient create(String hostIpAddress) throws ServerFault;

	void delete(String hostIpAddress) throws ServerFault;

	/**
	 * The plugin providing the biggest priority will be used.
	 * 
	 * @return client implementation priority
	 */
	int getPriority();

}
