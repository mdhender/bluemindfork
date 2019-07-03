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
package net.bluemind.vertx.testhelper;

import org.vertx.java.core.AsyncResult;

public class FakeResult<T> implements AsyncResult<T> {

	public static <W> AsyncResult<W> ok(W r) {
		return new FakeResult<W>(null, r);
	}

	private final Throwable cause;
	private final T result;

	public FakeResult(Throwable cause, T result) {
		this.cause = cause;
		this.result = result;
	}

	@Override
	public T result() {
		return result;
	}

	@Override
	public Throwable cause() {
		return cause;
	}

	@Override
	public boolean succeeded() {
		return cause == null;
	}

	@Override
	public boolean failed() {
		return cause != null;
	}

}
