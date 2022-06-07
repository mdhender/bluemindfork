/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.webappdata.service.internal;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.validator.IValidator;
import net.bluemind.webappdata.api.WebAppData;

public class WebAppDataValidator implements IValidator<WebAppData> {

	@Override
	public void create(WebAppData data) {
		throw new ServerFault("Cant check if an item with same key already exists for this user...",
				ErrorCode.DEPRECATED);
	}

	@Override
	public void update(WebAppData oldData, WebAppData data) {
		if (!oldData.key.equals(data.key)) {
			String errorMsg = "You can't change key for the same itemUid, delete it and create another one if needed.";
			throw new ServerFault(errorMsg, ErrorCode.INVALID_PARAMETER);
		}
		validate(data);
	}

	public void create(WebAppData data, WebAppData sameKey) {
		if (sameKey != null) {
			throw new ServerFault("Key '" + data.key + "' already exists for this user", ErrorCode.ALREADY_EXISTS);
		}
		validate(data);
	}

	private void validate(WebAppData data) {
		if (data == null || data.key == null) {
			throw new ServerFault("WebAppData (or its key) is null", ErrorCode.INVALID_PARAMETER);
		}
		isKeyValid(data.key);
	}

	private final String KEY_SEPARATOR = ":";

	private void isKeyValid(String key) {
		String[] splitted = key.split(KEY_SEPARATOR);
		if (splitted.length <= 2) {
			String errorMsg = "WebAppData key format must be 'appName:appFeat:name'. Here are some example: 'mail-app:folders:expanded' / 'mail-app:message_list:size' / 'mail-app:composition:show_formatting_toolbar'";
			throw new ServerFault(errorMsg, ErrorCode.INVALID_PARAMETER);
		}
	}
}
