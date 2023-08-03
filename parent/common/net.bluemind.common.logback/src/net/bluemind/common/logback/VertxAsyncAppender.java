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

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import net.bluemind.common.vertx.contextlogging.ContextualData;

/**
 * This appender will copy all ContextualData properties to MDC for use in the
 * AsyncAppender from logback-classic
 * 
 * Direct use of ContextualData is not possible because the logback async thread
 * will not have access to ContextualData
 */
public class VertxAsyncAppender extends AsyncAppender {
	@Override
	protected void append(ILoggingEvent eventObject) {
		Map<String, String> cd = ContextualData.getAll();
		if (!cd.isEmpty()) {
			Map<String, String> mergedmap = new HashMap<>(eventObject.getMDCPropertyMap());
			mergedmap.putAll(cd);
			eventObject = new LoggingEventWrapper(eventObject, mergedmap);
		}
		super.append(eventObject);
	}
}
