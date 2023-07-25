package net.bluemind.common.logback;

import net.bluemind.common.vertx.contextlogging.ContextualData;

public class VertxContextUserProvider implements ContextUserProvider {

	@Override
	public String user() {
		return ContextualData.get("user");
	}
}
