/*BEGIN LICENSE
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
package net.bluemind.core.api;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class ParametersValidator {
	public static void notNull(Object obj) throws ServerFault {
		if (obj == null) {
			throw new ServerFault("Invalid parameter", ErrorCode.INVALID_PARAMETER);
		}
	}

	public static void notNullAndNotEmpty(String str) throws ServerFault {
		if (StringUtils.isBlank(str)) {
			throw new ServerFault("Invalid parameter", ErrorCode.INVALID_PARAMETER);
		}
	}

	public static void nullOrNotEmpty(String str) throws ServerFault {
		if (null != str) {
			notNullAndNotEmpty(str);
		}
	}

	public static void notEmpty(Collection<?> col) throws ServerFault {
		if (null == col || col.isEmpty()) {
			throw new ServerFault("Invalid parameter", ErrorCode.INVALID_PARAMETER);
		}
	}
}
