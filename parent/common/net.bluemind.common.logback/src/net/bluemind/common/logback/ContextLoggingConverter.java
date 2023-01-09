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

import static ch.qos.logback.core.util.OptionHelper.extractDefaultReplacement;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import net.bluemind.common.vertx.contextlogging.ContextualData;

/**
 * Converts vertx ContextualData to logback
 */
public class ContextLoggingConverter extends ClassicConverter {

	private String key;
	private String defaultValue;

	public ContextLoggingConverter() {
		reset();
	}

	private void reset() {
		key = null;
		defaultValue = "";
	}

	@Override
	public void start() {
		String[] keyInfo = extractDefaultReplacement(getFirstOption());
		key = keyInfo[0];
		if (keyInfo[1] != null) {
			defaultValue = keyInfo[1];
		}
		super.start();
	}

	@Override
	public String convert(ILoggingEvent event) {
		ContextInternal context = (ContextInternal) Vertx.currentContext();
		if (context != null && key != null) {
			return ContextualData.getOrDefault(key, defaultValue);
		}
		return defaultValue;
	}

	@Override
	public void stop() {
		reset();
		super.stop();
	}
}