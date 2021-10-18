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

public class WazoUnauthorizedException extends WazoApiException {

	private static final long serialVersionUID = 1L;
	private static final String MESSAGE = "You need to be wazo Admin to execute bluemind Wazo ";

	public WazoUnauthorizedException(String message) {
		super(message);
	}

	public WazoUnauthorizedException(Exception e) {
		super(e);
	}

	public WazoUnauthorizedException(WazoEndpoints endpoint, Exception e) {
		super(MESSAGE, endpoint, e);
	}

	public WazoUnauthorizedException(String host, WazoEndpoints endpoint, Exception e) {
		super(MESSAGE, host, endpoint, e);
	}

	public WazoUnauthorizedException(WazoEndpoints endpoint) {
		super(MESSAGE, endpoint);
	}

	public WazoUnauthorizedException(String host, WazoEndpoints endpoint) {
		super(MESSAGE, host, endpoint);
	}
}
