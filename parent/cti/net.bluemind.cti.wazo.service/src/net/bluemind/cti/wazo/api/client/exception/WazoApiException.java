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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.cti.wazo.config.WazoEndpoints;

public abstract class WazoApiException extends ServerFault {

	private static final long serialVersionUID = 1L;

	public WazoApiException(String message) {
		super(message);
	}

	public WazoApiException(Exception e) {
		super(e);
	}

	public WazoApiException(String message, WazoEndpoints endpoint) {
		super(message.concat(displayUrl(endpoint)));
	}

	public WazoApiException(String message, String host, WazoEndpoints endpoint) {
		super(message.concat(displayUrl(host, endpoint)));
	}

	public WazoApiException(String message, WazoEndpoints endpoint, String response) {
		super(message.concat(displayUrl(endpoint)).concat(" : ").concat(response));
	}

	public WazoApiException(String message, String host, WazoEndpoints endpoint, String response) {
		super(message.concat(displayUrl(host, endpoint)).concat(" : ").concat(response));
	}

	public WazoApiException(String message, WazoEndpoints endpoint, Exception e) {
		super(message.concat(displayUrl(endpoint)), e);
	}

	public WazoApiException(String message, String host, WazoEndpoints endpoint, Exception e) {
		super(message.concat(displayUrl(host, endpoint)), e);
	}

	private static String displayUrl(String host, WazoEndpoints endpoint) {
		return new String("'").concat(host).concat(endpoint.endpoint()).concat("'");
	}

	private static String displayUrl(WazoEndpoints endpoint) {
		return new String("'").concat(endpoint.endpoint()).concat("'");
	}

}
