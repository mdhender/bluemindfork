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
package net.bluemind.gwtconsoleapp.base.handler;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;

public abstract class DefaultAsyncHandler<T> implements AsyncHandler<T> {

	protected final AsyncHandler<Void> parentHandler;

	public DefaultAsyncHandler() {
		this(null);
	}

	public DefaultAsyncHandler(AsyncHandler<Void> parentHandler) {
		this.parentHandler = parentHandler;
	}

	public abstract void success(T value);

	@Override
	public void failure(Throwable e) {
		if (null != parentHandler) {
			parentHandler.failure(e);
		} else {
			Notification.get().reportError(e);
		}
	}

}
