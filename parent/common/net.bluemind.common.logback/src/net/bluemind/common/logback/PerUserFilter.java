/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.common.logback;

import org.slf4j.MDC;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import net.bluemind.common.vertx.contextlogging.ContextualData;

public final class PerUserFilter extends Filter<ILoggingEvent> {
	private String endpoint;

	@Override
	public FilterReply decide(ILoggingEvent event) {
		String user = ContextualData.get("user");
		if (user == null || user.equals("anon")) {
			user = MDC.get("mapiUser");
		}
		if (user == null || user.equals("anon")) {
			return FilterReply.DENY;
		}
		return Boolean.getBoolean(user + "." + endpoint + ".logging") ? FilterReply.NEUTRAL : FilterReply.DENY;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public void start() {
		if (this.endpoint != null) {
			super.start();
		}
	}
}