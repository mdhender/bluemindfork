package net.bluemind.common.logback;

import org.slf4j.MDC;

public class MDCContextUserProvider implements ContextUserProvider {

	@Override
	public String user() {
		return MDC.get("mapiUser");
	}

}
