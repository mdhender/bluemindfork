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
package net.bluemind.dataprotect.common.email;

import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.service.action.EmailData;
import net.bluemind.dataprotect.service.action.RestoreActionExecutor;

public class SendIcs extends SendEmail {

	public SendIcs(Restorable item, RestoreActionExecutor<EmailData> executor, IServerTaskMonitor monitor) {
		super(item, executor, monitor);
	}

	@Override
	protected String getMimeType() {
		return "text/calendar";
	}

	@Override
	protected String getExtension() {
		return ".ics";
	}

}
