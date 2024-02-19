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
package net.bluemind.webmodule.authenticationfilter.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;

@SuppressWarnings("serial")
public class SessionData implements Serializable {
	public final String authKey;

	public final JsonObject jwtToken;
	public final String realm;
	public final String openIdClientSecret;
	public final long refreshTimerId;

	public final Status passwordStatus;

	public final Map<String, String> settings;
	public final boolean privateComputer;

	public final String loginAtDomain;
	public final String domainUid;
	public final String rolesAsString;
	public final String userUid;
	public final long createStamp;

	public final String accountType;
	public final String login;
	public final String defaultEmail;
	public final String givenNames;
	public final String familyNames;
	public final String formatedName;
	public final String dataLocation;
	public final String mailboxCopyGuid;

	private SessionData(String authKey, //
			JsonObject jwtToken, //
			String realm, //
			String openIdClientSecret, //
			long refreshTimerId, //
			Status passwordStatus, //
			Map<String, String> settings, //
			boolean privateComputer, //
			String loginAtDomain, //
			String domainUid, //
			String rolesAsString, //
			String userUid, //
			long createStamp, //
			String accountType, //
			String login, //
			String defaultEmail, //
			String givenNames, //
			String familyNames, //
			String formatedName, //
			String dataLocation, //
			String mailboxCopyGuid //
	) {
		this.authKey = authKey;
		this.jwtToken = jwtToken;
		this.realm = realm;
		this.openIdClientSecret = openIdClientSecret;
		this.refreshTimerId = refreshTimerId;
		this.passwordStatus = passwordStatus;
		this.settings = filterSettings(settings);
		this.privateComputer = privateComputer;

		this.loginAtDomain = loginAtDomain;
		this.domainUid = domainUid;
		this.rolesAsString = rolesAsString.intern();
		this.userUid = userUid;
		this.createStamp = createStamp;

		this.accountType = accountType;
		this.login = login;
		this.defaultEmail = defaultEmail;
		this.givenNames = givenNames;
		this.familyNames = familyNames;
		this.formatedName = formatedName;
		this.dataLocation = dataLocation;
		this.mailboxCopyGuid = sanitizeMailboxCopyGuid(mailboxCopyGuid);
	}

	public SessionData(LoginResponse loginResponse) {
		this.authKey = loginResponse.authKey;

		this.jwtToken = null;
		this.realm = null;
		this.openIdClientSecret = null;
		this.refreshTimerId = -1;

		this.passwordStatus = loginResponse.status;
		this.settings = filterSettings(loginResponse.authUser.settings);
		this.privateComputer = false;

		this.loginAtDomain = loginResponse.latd;
		this.domainUid = loginResponse.authUser.domainUid;
		this.rolesAsString = rolesAsString(loginResponse.authUser.roles);
		this.userUid = loginResponse.authUser.uid;
		this.createStamp = System.currentTimeMillis();

		this.login = loginResponse.authUser.value.login;
		this.accountType = loginResponse.authUser.value.accountType.name();

		this.defaultEmail = loginResponse.authUser.value.defaultEmail() != null
				? loginResponse.authUser.value.defaultEmail().address
				: null;

		this.givenNames = loginResponse.authUser.value.contactInfos != null
				? loginResponse.authUser.value.contactInfos.identification.name.givenNames
				: null;
		this.familyNames = loginResponse.authUser.value.contactInfos != null
				? loginResponse.authUser.value.contactInfos.identification.name.familyNames
				: null;
		this.formatedName = loginResponse.authUser.value.contactInfos != null
				? loginResponse.authUser.value.contactInfos.identification.formatedName.value
				: null;

		this.dataLocation = loginResponse.authUser.value.dataLocation;
		this.mailboxCopyGuid = sanitizeMailboxCopyGuid(loginResponse.authUser.value.mailboxCopyGuid);
	}

	private String sanitizeMailboxCopyGuid(String mailboxCopyGuid) {
		return mailboxCopyGuid != null ? mailboxCopyGuid : "";
	}

	private String rolesAsString(Set<String> roles) {
		// for 13k sessions, we end up with 3MB of duplicate strings here
		return roles.stream().collect(Collectors.joining(",")).intern();
	}

	public SessionData setOpenId(JsonObject jwtToken, String realm, String openIdClientSecret, long refreshTimerId) {
		return new SessionData(authKey, jwtToken, realm, openIdClientSecret, refreshTimerId, passwordStatus, settings,
				privateComputer, loginAtDomain, domainUid, rolesAsString, userUid, createStamp, accountType, login,
				defaultEmail, givenNames, familyNames, formatedName, dataLocation, mailboxCopyGuid);
	}

	public static JsonObject toJson(SessionData sd) {
		JsonObject jsonObject = new JsonObject();

		jsonObject.put("authKey", sd.authKey);
		jsonObject.put("jwtToken", sd.jwtToken);
		jsonObject.put("realm", sd.realm);
		jsonObject.put("openIdClientSecret", sd.openIdClientSecret);

		jsonObject.put("passwordStatus", sd.passwordStatus);

		JsonObject settingsAsJson = new JsonObject();
		sd.settings.forEach(settingsAsJson::put);
		jsonObject.put("settings", settingsAsJson);

		jsonObject.put("privateComputer", sd.privateComputer);

		jsonObject.put("loginAtDomain", sd.loginAtDomain);
		jsonObject.put("domainUid", sd.domainUid);
		jsonObject.put("rolesAsString", sd.rolesAsString);
		jsonObject.put("userUid", sd.userUid);
		jsonObject.put("createStamp", sd.createStamp);

		jsonObject.put("accountType", sd.accountType);
		jsonObject.put("login", sd.login);
		jsonObject.put("defaultEmail", sd.defaultEmail);
		jsonObject.put("givenNames", sd.givenNames);
		jsonObject.put("familyNames", sd.familyNames);
		jsonObject.put("formatedName", sd.formatedName);
		jsonObject.put("dataLocation", sd.dataLocation);
		jsonObject.put("mailboxCopyGuid", sd.mailboxCopyGuid);
		return jsonObject;
	}

	private Map<String, String> filterSettings(Map<String, String> settings) {
		if (settings == null) {
			return Collections.emptyMap();
		}
		return Map.of("lang", settings.get("lang"), "default_app", settings.get("default_app"));
	}

	public static SessionData fromJson(JsonObject jsonObject) {
		String authKey = jsonObject.getString("authKey");
		JsonObject jwtToken = jsonObject.getJsonObject("jwtToken");
		String realm = jsonObject.getString("realm");
		String openIdClientSecret = jsonObject.getString("openIdClientSecret");

		Status passwordStatus = Status.valueOf(jsonObject.getString("passwordStatus"));

		JsonObject settingsAsJson = jsonObject.getJsonObject("settings");
		Map<String, String> settings = new HashMap<>();
		// Will be filtered by SessionData constructor
		settingsAsJson.forEach(e -> settings.put(e.getKey(), (String) e.getValue()));

		boolean privateComputer = jsonObject.getBoolean("privateComputer");

		String loginAtDomain = jsonObject.getString("loginAtDomain");
		String domainUid = jsonObject.getString("domainUid");
		String rolesAsString = jsonObject.getString("rolesAsString");
		String userUid = jsonObject.getString("userUid");
		long createStamp = jsonObject.getLong("createStamp");

		String accountType = jsonObject.getString("accountType");
		String login = jsonObject.getString("login");
		String defaultEmail = jsonObject.getString("defaultEmail");
		String givenNames = jsonObject.getString("givenNames");
		String familyNames = jsonObject.getString("familyNames");
		String formatedName = jsonObject.getString("formatedName");
		String dataLocation = jsonObject.getString("dataLocation");

		String mailboxCopyGuid = jsonObject.getString("mailboxCopyGuid");

		return new SessionData(authKey, jwtToken, realm, openIdClientSecret, -1, passwordStatus, settings,
				privateComputer, loginAtDomain, domainUid, rolesAsString, userUid, createStamp, accountType, login,
				defaultEmail, givenNames, familyNames, formatedName, dataLocation, mailboxCopyGuid);
	}
}
