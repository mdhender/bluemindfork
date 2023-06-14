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
package net.bluemind.dataprotect.service.action;

import java.util.function.Consumer;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sendmail.Sendmail;

public class RestoreActionExecutor<T extends IRestoreActionData>
		implements Consumer<RestoreAction<IRestoreActionData>> {

	public final BmContext context;

	public RestoreActionExecutor(BmContext context) {
		this.context = context;
	}

	@Override
	public void accept(RestoreAction<IRestoreActionData> restoreAction) {
		switch (restoreAction.type) {
        case EMAIL: {
			EmailData email = (EmailData) restoreAction.data;
			Sendmail mailer = new Sendmail();
			mailer.send(email.from, email.message);
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + restoreAction.type);
		}

	}

}
