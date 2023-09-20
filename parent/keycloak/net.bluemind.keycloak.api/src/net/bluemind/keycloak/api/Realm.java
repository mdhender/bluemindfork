/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.keycloak.api;

import java.util.Objects;
import java.util.Set;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class Realm {
	public String id;
	public String realm;
	public boolean enabled;
	public boolean loginWithEmailAllowed;
	public String loginTheme;
	public boolean internationalizationEnabled;
	public String defaultLocale;
	public Set<String> supportedLocales;
	public long accessCodeLifespanLogin; // login timeout, in seconds
	public long accessTokenLifespan;
	public long ssoSessionIdleTimeout;
	public long ssoSessionMaxLifespan;

	@Override
	public int hashCode() {
		return Objects.hash(accessCodeLifespanLogin, accessTokenLifespan, defaultLocale, enabled, id,
				internationalizationEnabled, loginTheme, loginWithEmailAllowed, realm, ssoSessionIdleTimeout,
				ssoSessionMaxLifespan, supportedLocales);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Realm other = (Realm) obj;
		return accessCodeLifespanLogin == other.accessCodeLifespanLogin
				&& accessTokenLifespan == other.accessTokenLifespan
				&& Objects.equals(defaultLocale, other.defaultLocale) && enabled == other.enabled
				&& Objects.equals(id, other.id) && internationalizationEnabled == other.internationalizationEnabled
				&& Objects.equals(loginTheme, other.loginTheme) && loginWithEmailAllowed == other.loginWithEmailAllowed
				&& Objects.equals(realm, other.realm) && ssoSessionIdleTimeout == other.ssoSessionIdleTimeout
				&& ssoSessionMaxLifespan == other.ssoSessionMaxLifespan
				&& Objects.equals(supportedLocales, other.supportedLocales);
	}

	@Override
	public String toString() {
		return "Realm [id=" + id + ", realm=" + realm + ", enabled=" + enabled + ", loginWithEmailAllowed="
				+ loginWithEmailAllowed + ", loginTheme=" + loginTheme + ", internationalizationEnabled="
				+ internationalizationEnabled + ", defaultLocale=" + defaultLocale + ", supportedLocales="
				+ supportedLocales + ", accessCodeLifespanLogin=" + accessCodeLifespanLogin + ", accessTokenLifespan="
				+ accessTokenLifespan + ", ssoSessionIdleTimeout=" + ssoSessionIdleTimeout + ", ssoSessionMaxLifespan="
				+ ssoSessionMaxLifespan + "]";
	}
}
