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
package net.bluemind.eas.client;

public class AccountInfos {
	private String login;
	private String password;
	private String userId;
	private String devId;
	private String devType;
	private String url;
	private String userAgent;

	public AccountInfos(String login, String password, String devId, String devType, String url, String userAgent) {
		this.login = login;
		int idx = login.indexOf('@');
		if (idx > 0) {
			// String d = login.substring(idx + 1);
			this.userId = login; // d + "\\" + login.substring(0, idx);
		}

		this.password = password;
		this.devId = devId;
		this.devType = devType;
		this.url = url;
		this.userAgent = userAgent;
	}

	public String getLogin() {
		return login;
	}

	public String getPassword() {
		return password;
	}

	public String getUserId() {
		return userId;
	}

	public String getDevId() {
		return devId;
	}

	public String getDevType() {
		return devType;
	}

	public String getUrl() {
		return url;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public String authValue() {
		StringBuilder sb = new StringBuilder();
		sb.append("Basic ");
		String encoded = new String(java.util.Base64.getEncoder().encode((userId + ":" + password).getBytes()));
		sb.append(encoded);
		String ret = sb.toString();
		return ret;
	}

	public void setDevType(String type) {
		this.devType = type;
	}

}
