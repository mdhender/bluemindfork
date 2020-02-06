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
package net.bluemind.vertx.common;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import net.bluemind.vertx.common.impl.LoginResponse;

public abstract class ValidationHandler implements Handler<AsyncResult<Message<LocalJsonObject<LoginResponse>>>> {

	@Override
	public final void handle(AsyncResult<Message<LocalJsonObject<LoginResponse>>> loginResp) {
		if (loginResp.succeeded()) {
			LoginResponse lrb = loginResp.result().body().getValue();
			onValidation(lrb.isOk());
		} else {
			onValidation(false);
		}
	}

	public abstract void onValidation(boolean validated);

}
