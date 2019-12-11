/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.calendar.service.deferredaction;

import net.bluemind.deferredaction.registry.IDeferredActionExecutor;
import net.bluemind.deferredaction.registry.IDeferredActionExecutorFactory;

public class DeferredActionEventExecutorFactory implements IDeferredActionExecutorFactory {

	public String getSupportedActionId() {
		return EventDeferredAction.ACTION_ID;
	}

	public IDeferredActionExecutor create() {
		return new DeferredActionEventExecutor();
	}

}