/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.cti.wazo.api.client.exception;

import net.bluemind.cti.wazo.config.WazoEndpoints;

public class WazoConnectionException extends WazoApiException {

	private static final long serialVersionUID = 1L;
	private static final String MESSAGE = "Unable to execute bluemind Wazo ";

	public WazoConnectionException(String message) {
		super(message);
	}

	public WazoConnectionException(Exception e) {
		super(e);
	}

	public WazoConnectionException(WazoEndpoints endpoint, Exception e) {
		super(MESSAGE, endpoint, e);
	}

	public WazoConnectionException(String host, WazoEndpoints endpoint, Exception e) {
		super(MESSAGE, host, endpoint, e);
	}

}
