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
package net.bluemind.hps.auth.core2;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;

import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.user.api.User;

@SuppressWarnings("serial")
public class SessionData implements Serializable {
	public String authKey;
	public Status passwordStatus;

	public Map<String, String> settings;
	public boolean privateComputer;

	private long lastPing;
	public String loginAtDomain;
	public String domainUid;
	public String rolesAsString;
	public Set<String> roles;
	protected String userUid;
	public final long createStamp;

	public final String accountType;
	public final String login;
	public final String defaultEmail;
	public final String givenNames;
	public final String familyNames;
	public final String formatedName;
	public final String dataLocation;

	private SessionData(String authKey, //
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
			String dataLocation //
	) {
		this.authKey = authKey;
		this.passwordStatus = passwordStatus;

		this.settings = settings;
		this.privateComputer = privateComputer;

		this.lastPing = System.currentTimeMillis();
		this.loginAtDomain = loginAtDomain;
		this.domainUid = domainUid;
		this.rolesAsString = rolesAsString;
		this.userUid = userUid;
		this.createStamp = createStamp;

		this.accountType = accountType;
		this.login = login;
		this.defaultEmail = defaultEmail;
		this.givenNames = givenNames;
		this.familyNames = familyNames;
		this.formatedName = formatedName;
		this.dataLocation = dataLocation;
	}

	public SessionData(User user) {
		this.createStamp = System.currentTimeMillis();
		lastPing = createStamp;

		this.login = user.login;
		this.accountType = user.accountType.name();

		this.defaultEmail = user.defaultEmail() != null ? user.defaultEmail().address : null;

		this.givenNames = user.contactInfos != null ? user.contactInfos.identification.name.givenNames : null;
		this.familyNames = user.contactInfos != null ? user.contactInfos.identification.name.familyNames : null;
		this.formatedName = user.contactInfos != null ? user.contactInfos.identification.formatedName.value : null;

		this.dataLocation = user.dataLocation;
	}

	public String getUserUid() {
		return userUid;
	}

	public Map<String, String> getSettings() {
		return settings;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}

	public boolean isPrivateComputer() {
		return privateComputer;
	}

	public void setPrivateComputer(boolean privateComputer) {
		this.privateComputer = privateComputer;
	}

	public long getLastPing() {
		return lastPing;
	}

	public void setLastPing(long lastPing) {
		this.lastPing = lastPing;
	}

	public void setRole(Set<String> roles) {
		// for 13k sessions, we end up with 3MB of duplicate strings here
		rolesAsString = Joiner.on(",").join(roles).intern();
		this.roles = roles.stream().map(String::intern).collect(Collectors.toSet());
	}

	public void setRole(String rolesAsString) {
		// for 13k sessions, we end up with 3MB of duplicate strings here
		this.rolesAsString = rolesAsString.intern();
		roles = Arrays.asList(rolesAsString.split(",")).stream().map(String::intern).collect(Collectors.toSet());
	}

	public static JsonObject toJson(SessionData sd) {
		JsonObject jsonObject = new JsonObject();

		jsonObject.put("authKey", sd.authKey);
		jsonObject.put("passwordStatus", sd.passwordStatus);

		JsonObject settingsAsJson = new JsonObject();
		sd.settings.forEach((k, v) -> settingsAsJson.put(k, v));
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

		return jsonObject;
	}

	public static SessionData fromJson(JsonObject jsonObject) {
		String authKey = jsonObject.getString("authKey");

		Status passwordStatus = Status.valueOf(jsonObject.getString("passwordStatus"));

		JsonObject settingsAsJson = jsonObject.getJsonObject("settings");
		Map<String, String> settings = new HashMap<>();
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

		SessionData sessionData = new SessionData(authKey, passwordStatus, settings, privateComputer, loginAtDomain,
				domainUid, rolesAsString, userUid, createStamp, accountType, login, defaultEmail, givenNames,
				familyNames, formatedName, dataLocation);
		sessionData.setRole(rolesAsString);
		return sessionData;
	}
}
