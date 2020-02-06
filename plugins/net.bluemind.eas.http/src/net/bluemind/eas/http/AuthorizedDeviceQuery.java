/* BEGIN LICENSE
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
package net.bluemind.eas.http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.eas.dto.OptionalParams;

public class AuthorizedDeviceQuery {

	private AuthenticatedEASQuery event;
	private String partnershipId;
	private Vertx vertx;

	public AuthorizedDeviceQuery(Vertx vertx, AuthenticatedEASQuery event, String partnershipId) {
		this.event = event;
		this.partnershipId = partnershipId;
		this.vertx = vertx;
	}

	public Vertx vertx() {
		return vertx;
	}

	public HttpServerRequest request() {
		return event.request();
	}

	public String loginAtDomain() {
		return event.loginAtDomain();
	}

	public String sid() {
		return event.sid();
	}

	public OptionalParams optionalParams() {
		return event.optionalParams();
	}

	public String command() {
		return event.command();
	}

	public String deviceIdentifier() {
		return event.deviceIdentifier();
	}

	public String partnershipId() {
		return partnershipId;
	}

	public String deviceType() {
		return event.deviceType();
	}

	public double protocolVersion() {
		return event.protocolVersion();
	}

	public Long policyKey() {
		return event.policyKey();
	}

}
