/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.common.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import net.bluemind.common.vertx.contextlogging.ContextualData;

public final class EndpointFilter extends Filter<ILoggingEvent> {
	String endpoint;

	@Override
	public FilterReply decide(ILoggingEvent event) {
		if (endpoint == null) {
			return FilterReply.DENY;
		}
		return endpoint.equals(ContextualData.getOrDefault("endpoint", "none")) ? FilterReply.NEUTRAL
				: FilterReply.DENY;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
}