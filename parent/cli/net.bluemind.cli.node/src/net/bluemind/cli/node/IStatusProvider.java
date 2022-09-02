/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cli.node;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.node.api.INodeClient;
import net.bluemind.server.api.Server;

public interface IStatusProvider {

	void report(CliContext ctx, ItemValue<Server> srv, INodeClient nc);

}
