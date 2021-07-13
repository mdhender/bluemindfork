package net.bluemind.central.reverse.proxy.vertx;

import io.vertx.core.Future;

public interface DispatchInfoMatcher<T, U> {

	Future<U> match(T context);
}
