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

import java.util.List;

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
	public List<String> supportedLocales;
	public long accessCodeLifespanLogin; // login timeout, in seconds
	public long accessTokenLifespan;
	public long ssoSessionIdleTimeout;
	public long ssoSessionMaxLifespan;
}
