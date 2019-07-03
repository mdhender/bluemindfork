package net.bluemind.lib.vertx.internal;

import org.vertx.java.core.AsyncResult;

public class Result<T> implements AsyncResult<T> {

	private final Throwable cause;

	public Result(Throwable cause) {
		this.cause = cause;
	}

	public Result() {
		this(null);
	}

	@Override
	public T result() {
		return null;
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
