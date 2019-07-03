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
import java.util.Map;
import java.util.Set;

import net.bluemind.user.api.User;

@SuppressWarnings("serial")
public class SessionData implements Serializable {

	public String authKey;
	public User user;
	public String allowedRpcs;
	public String availableHandlers;
	public String coreUrl;
	public Map<String, String> settings;
	public boolean privateComputer;

	private long lastPing;
	public String loginAtDomain;
	public String domainUid;
	public String rolesAsString;
	public Set<String> roles;
	protected String userUid;
	public final long createStamp;

	public SessionData() {
		this.createStamp = System.currentTimeMillis();
		lastPing = createStamp;
	}

	public String getUserUid() {
		return userUid;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getAllowedRpcs() {
		return allowedRpcs;
	}

	public void setAllowedRpcs(String allowedRpcs) {
		this.allowedRpcs = allowedRpcs;
	}

	public String getCoreUrl() {
		return coreUrl;
	}

	public void setCoreUrl(String coreUrl) {
		this.coreUrl = coreUrl;
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

	public String getAvailableHandlers() {
		return availableHandlers;
	}

	public void setAvailableHandlers(String availableHandlers) {
		this.availableHandlers = availableHandlers;
	}

	public String getRoles() {
		return rolesAsString;
	}

	public void setRoles(String roles) {
		this.rolesAsString = roles;
	}
}
