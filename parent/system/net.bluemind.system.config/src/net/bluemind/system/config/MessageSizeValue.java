/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.system.config;

import java.util.Collections;

import net.bluemind.system.api.SystemConf;

public class MessageSizeValue {
	public final long oldValue;
	public final long newValue;

	public MessageSizeValue(long oldValue, long newValue) {
		this.oldValue = oldValue / 1024 / 1024;
		this.newValue = newValue / 1024 / 1024;
	}

	public static MessageSizeValue create(long newValue) {
		return new MessageSizeValue(0, newValue);
	}

	public boolean isSet() {
		return newValue > 0;
	}

	public boolean hasChanged() {
		return oldValue != newValue;
	}

	public static MessageSizeValue getMessageSizeLimit(String key, SystemConf previous, SystemConf conf) {
		long messageSizeLimit = conf.convertedValue(key, val -> Long.parseLong(val), 0l);
		long prevMessageSizeLimit = previous.convertedValue(key, val -> Long.parseLong(val), 0l);

		return new MessageSizeValue(prevMessageSizeLimit, messageSizeLimit);
	}

	public static MessageSizeValue getMessageSizeLimit(String key, SystemConf conf) {
		SystemConf emptyConf = new SystemConf();
		emptyConf.values = Collections.emptyMap();
		return getMessageSizeLimit(key, emptyConf, conf);
	}

}
