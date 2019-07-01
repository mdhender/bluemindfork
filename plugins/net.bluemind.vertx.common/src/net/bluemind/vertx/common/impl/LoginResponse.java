/*BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012
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
 * END LICENSE
 */

package net.bluemind.vertx.common.impl;

public final class LoginResponse {

	private final boolean ok;
	private final String coreUrl;
	private final String token;
	private final String principal;

	public LoginResponse(boolean isOk, String coreUrl, String token, String principal) {
		this.ok = isOk;
		this.coreUrl = coreUrl;
		this.token = token;
		this.principal = principal;
	}

	/**
	 * @return the status
	 */
	public boolean isOk() {
		return ok;
	}

	/**
	 * @return the coreUrl
	 */
	public String getCoreUrl() {
		return coreUrl;
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @return the principal
	 */
	public String getPrincipal() {
		return principal;
	}

}
