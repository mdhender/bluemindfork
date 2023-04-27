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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.AbstractDiscriminator;
import ch.qos.logback.core.util.OptionHelper;
import net.bluemind.common.vertx.contextlogging.ContextualData;

public class ContextualDataDiscriminator extends AbstractDiscriminator<ILoggingEvent> {

	private String key;
	private String defaultValue;

	/**
	 * Return the value associated with an ContextualData entry designated by the
	 * Key property. If that value is null, then return the value assigned to the
	 * DefaultValue property.
	 */
	public String getDiscriminatingValue(ILoggingEvent event) {
		return ContextualData.getOrDefault(key, defaultValue);
	}

	@Override
	public void start() {
		int errors = 0;
		if (OptionHelper.isEmpty(key)) {
			errors++;
			addError("The \"Key\" property must be set");
		}
		if (OptionHelper.isEmpty(defaultValue)) {
			errors++;
			addError("The \"DefaultValue\" property must be set");
		}
		if (errors == 0) {
			started = true;
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @return
	 * @see #setDefaultValue(String)
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * The default ContextualData value in case the MDC is not set for
	 * {@link #setKey(String) mdcKey}.
	 * <p/>
	 * <p>
	 * For example, if {@link #setKey(String) Key} is set to the value "someKey",
	 * and the ContextualData is not set for "someKey", then this appender will use
	 * the default value, which you can set with the help of this method.
	 *
	 * @param defaultValue
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}
