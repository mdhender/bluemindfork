/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.core.rest.base;

import java.util.concurrent.ExecutorService;

import net.bluemind.lib.vertx.BMExecutor;

public class ExecutorHolder {
	private static final BMExecutor BM_EXECUTOR = new BMExecutor("BM-Core");

	private ExecutorHolder() {
	}

	public static BMExecutor get() {
		return BM_EXECUTOR;
	}

	public static ExecutorService getAsService() {
		return BM_EXECUTOR.asExecutorService();
	}
}
