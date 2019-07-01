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
package net.bluemind.vertx.common;

public class LoginRequest {

	private final String login;
	private final String pass;
	private final String origin;
	private final String role;

	private LoginRequest(String login, String password, String origin, String role) {
		this.login = login;
		this.pass = password;
		this.origin = origin;
		this.role = role;
	}

	public static final LocalJsonObject<LoginRequest> of(String login, String password, String origin) {
		return new LocalJsonObject<>(new LoginRequest(login, password, origin, null));
	}

	public static final LocalJsonObject<LoginRequest> of(String login, String password, String origin, String role) {
		return new LocalJsonObject<>(new LoginRequest(login, password, origin, role));
	}

	/**
	 * @return the login
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * @return the pass
	 */
	public String getPass() {
		return pass;
	}

	/**
	 * @return the origin
	 */
	public String getOrigin() {
		return origin;
	}

	/**
	 * @return the role
	 */
	public String getRole() {
		return role;
	}
}
