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
package net.bluemind.system.service.internal;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.IMailDeliveryMgmt;

public class PostfixMgmt implements IMailDeliveryMgmt {
	private final BmContext context;

	public PostfixMgmt(BmContext context) {
		this.context = context;
	}

	@Override
	public TaskRef repair() throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can regenerate postfix maps", ErrorCode.PERMISSION_DENIED);
		}

		return context.provider().instance(ITasksManager.class).run(new BlockingServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				VertxPlatform.eventBus().publish("postfix.map.dirty", new JsonObject());
			}
		});

	}
}
