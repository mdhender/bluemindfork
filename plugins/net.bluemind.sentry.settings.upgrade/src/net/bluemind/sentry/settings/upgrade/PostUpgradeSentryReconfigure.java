/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.sentry.settings.upgrade;

import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.sentry.settings.SentryConfiguration;
import net.bluemind.system.schemaupgrader.PostInst;
import net.bluemind.system.schemaupgrader.UpdateResult;

public class PostUpgradeSentryReconfigure implements PostInst {
	@Override
	public UpdateResult executeUpdate(IServerTaskMonitor monitor) {
		MQ.getProducer(Topic.SENTRY_CONFIG).send(SentryConfiguration.get());
		return UpdateResult.ok();
	}
}
