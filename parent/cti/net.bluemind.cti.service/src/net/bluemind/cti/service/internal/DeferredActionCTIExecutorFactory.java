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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cti.service.internal;

import net.bluemind.cti.service.CTIDeferredAction;
import net.bluemind.deferredaction.registry.IDeferredActionExecutor;
import net.bluemind.deferredaction.registry.IDeferredActionExecutorFactory;

public class DeferredActionCTIExecutorFactory implements IDeferredActionExecutorFactory {

	@Override
	public String getSupportedActionId() {
		return CTIDeferredAction.ACTION_ID;
	}

	@Override
	public IDeferredActionExecutor create() {
		return new DeferredActionCTIExecutor();
	}

}
