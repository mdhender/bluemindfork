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

public class WazoApiResponseException extends WazoApiException {

	private static final long serialVersionUID = 1L;
	private static final String MESSAGE = "Error occurs on Wazo API call ";

	public WazoApiResponseException(String message) {
		super(message);
	}

	public WazoApiResponseException(Exception e) {
		super(e);
	}

	public WazoApiResponseException(WazoEndpoints endpoint, String response) {
		super(MESSAGE, endpoint, response);
	}

	public WazoApiResponseException(String host, WazoEndpoints endpoint, String response) {
		super(MESSAGE, host, endpoint, response);
	}

	public WazoApiResponseException(WazoEndpoints endpoint, Exception e) {
		super(MESSAGE, endpoint, e);
	}

	public WazoApiResponseException(String host, WazoEndpoints endpoint, Exception e) {
		super(MESSAGE, host, endpoint, e);
	}

}
