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
package net.bluemind.cti.wazo.config;

public enum WazoEndpoints {

	TOKEN("/auth/0.1/token"), //
	CALLD("/calld/1.0/users/me/calls"), //
	CONFD("/confd/1.1/users");

	private final String endpoint;

	private WazoEndpoints(String endpoint) {
		this.endpoint = endpoint;
	}

	public String endpoint() {
		return endpoint;
	}
}
