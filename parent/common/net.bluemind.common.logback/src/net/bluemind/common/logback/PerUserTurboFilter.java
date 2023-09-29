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

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

public final class PerUserTurboFilter extends TurboFilter {

	private String endpoint;
	private ContextUserProvider userProvider;
	private Level perUserLevel;

	@Override
	public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
		if (!isStarted()) {
			return FilterReply.NEUTRAL;
		}

		String user = userProvider.user();
		if (user == null || user.equals("anon")) {
			return FilterReply.NEUTRAL;
		}

		if (Boolean.getBoolean(user + "." + endpoint + ".logging") && level.isGreaterOrEqual(perUserLevel)) {
			return FilterReply.ACCEPT;
		} else {
			return FilterReply.NEUTRAL;
		}
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public void setUserProvider(ContextUserProvider userProvider) {
		this.userProvider = userProvider;
	}

	public void setPerUserLevel(Level level) {
		this.perUserLevel = level;
	}

	@Override
	public void start() {
		if (this.endpoint != null && userProvider != null && this.perUserLevel != null) {
			super.start();
		}
	}
}