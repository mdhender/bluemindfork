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
package net.bluemind.core.rest.filter;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.rest.base.RestRootHandler;
import net.bluemind.core.rest.log.CallLogger;
import net.bluemind.eclipse.common.IHasPriority;

public class CallLoggerFilter extends RestFilterAdapter implements IHasPriority {

	private static final String ROOT_HANDLER_NAME = RestRootHandler.class.getSimpleName();

	@Override
	public AsyncHandler<RestResponse> preAuthorization(RestRequest request,
			AsyncHandler<RestResponse> responseHandler) {
		CallLogger callLogger = CallLogger.start(ROOT_HANDLER_NAME, request);
		return callLogger.responseHandler(responseHandler);
	}

	@Override
	public int priority() {
		// because we want it as first filter
		return Integer.MAX_VALUE;
	}

}
