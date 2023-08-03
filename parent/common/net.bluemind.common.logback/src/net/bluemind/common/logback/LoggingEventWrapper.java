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

import java.time.Instant;
import java.util.Map;

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;

final class LoggingEventWrapper extends LoggingEvent {
	private final ILoggingEvent event;
	private final Map<String, String> mdcPropertyMap;
	private final LoggerContextVO vo;

	LoggingEventWrapper(ILoggingEvent event, Map<String, String> mdcPropertyMap) {
		this.event = event;
		this.mdcPropertyMap = mdcPropertyMap;

		final LoggerContextVO oldVo = event.getLoggerContextVO();
		if (oldVo != null) {
			vo = new LoggerContextVO(oldVo.getName(), mdcPropertyMap, oldVo.getBirthTime());
		} else {
			vo = null;
		}
	}

	@Override
	public Object[] getArgumentArray() {
		return event.getArgumentArray();
	}

	@Override
	public Level getLevel() {
		return event.getLevel();
	}

	@Override
	public String getLoggerName() {
		return event.getLoggerName();
	}

	@Override
	public String getThreadName() {
		return event.getThreadName();
	}

	@Override
	public IThrowableProxy getThrowableProxy() {
		return event.getThrowableProxy();
	}

	@Override
	public void prepareForDeferredProcessing() {
		event.prepareForDeferredProcessing();
	}

	@Override
	public LoggerContextVO getLoggerContextVO() {
		return vo;
	}

	@Override
	public String getMessage() {
		return event.getMessage();
	}

	@Override
	public long getTimeStamp() {
		return event.getTimeStamp();
	}

	// To keep compatibility with logback 1.4.x
	// @Override
	public Instant getInstant() {
		return Instant.ofEpochMilli(event.getTimeStamp());
	}

	@Override
	public StackTraceElement[] getCallerData() {
		return event.getCallerData();
	}

	@Override
	public boolean hasCallerData() {
		return event.hasCallerData();
	}

	@Override
	public Marker getMarker() {
		return event.getMarker();
	}

	@Override
	public String getFormattedMessage() {
		return event.getFormattedMessage();
	}

	@Override
	public Map<String, String> getMDCPropertyMap() {
		return mdcPropertyMap;
	}

	/**
	 * A synonym for {@link #getMDCPropertyMap}.
	 * 
	 * @deprecated Use {@link #getMDCPropertyMap()}.
	 */
	@Override
	@Deprecated
	public Map<String, String> getMdc() {
		return event.getMDCPropertyMap();
	}

	@Override
	public String toString() {
		return event.toString();
	}
}