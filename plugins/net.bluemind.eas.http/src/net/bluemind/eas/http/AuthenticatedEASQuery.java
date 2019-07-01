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

import org.vertx.java.core.http.HttpServerRequest;

import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.vertx.common.http.BasicAuthHandler.AuthenticatedRequest;

/**
 * Instances of this class hold decoded parameters for a successfully
 * authenticated request
 */
public final class AuthenticatedEASQuery {

	private final OptionalParams op;
	private final String deviceIdentifier;
	private final String deviceType;
	private final String command;
	private final double protocolVersion;
	private final Long policyKey;
	private final HttpServerRequest req;
	private final String login;
	private final String sid;

	public AuthenticatedEASQuery(AuthenticatedRequest event, double protocolVersion, Long policyKey,
			String deviceIdentifier, String deviceType, String command, OptionalParams op) {
		this.req = event.req;
		this.login = event.login;
		this.sid = event.sid;
		this.op = op;
		this.deviceIdentifier = deviceIdentifier;
		this.deviceType = deviceType;
		this.command = command;
		this.protocolVersion = protocolVersion;
		this.policyKey = policyKey;
	}

	public HttpServerRequest request() {
		return req;
	}

	public String loginAtDomain() {
		return login;
	}

	public String sid() {
		return sid;
	}

	public OptionalParams optionalParams() {
		return op;
	}

	public String command() {
		return command;
	}

	public String deviceIdentifier() {
		return deviceIdentifier;
	}

	public String deviceType() {
		return deviceType;
	}

	public double protocolVersion() {
		return protocolVersion;
	}

	public Long policyKey() {
		return policyKey;
	}

}
