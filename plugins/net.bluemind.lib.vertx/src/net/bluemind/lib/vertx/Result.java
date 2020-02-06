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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lib.vertx;

import java.util.Objects;

import io.vertx.core.AsyncResult;

public class Result<T> implements AsyncResult<T> {
	private final Throwable cause;
	private final T res;

	private Result(T res, Throwable cause) {
		this.res = res;
		this.cause = cause;
	}

	@Override
	public T result() {
		return res;
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

	public static <T> AsyncResult<T> success(T r) {
		return new Result<>(r, null);
	}

	public static AsyncResult<Void> success() {
		return new Result<>(null, null);
	}

	public static AsyncResult<Void> fail(Throwable cause) {
		Objects.requireNonNull(cause);
		return new Result<>(null, cause);
	}

}
