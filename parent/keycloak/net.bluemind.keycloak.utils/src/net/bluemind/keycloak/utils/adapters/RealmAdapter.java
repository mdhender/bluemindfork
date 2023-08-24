/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.keycloak.utils.adapters;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.keycloak.api.Realm;

public class RealmAdapter {
	private static final Logger logger = LoggerFactory.getLogger(RealmAdapter.class);

	private static final List<String> supportedLocales = Arrays.asList("en", "fr", "de");
	private static final String DEFAULT_LANG = "en";

	public final Realm realm;

	private RealmAdapter(Realm realm) {
		this.realm = realm;
	}

	public static RealmAdapter build(String domainUid) {
		Realm realm = new Realm();
		realm.enabled = true;

		realm.id = domainUid;
		realm.realm = domainUid;

		realm.loginWithEmailAllowed = true;

		realm.loginTheme = "bluemind";

		realm.internationalizationEnabled = true;
		realm.supportedLocales = supportedLocales;
		realm.defaultLocale = getDomainLocale(domainUid);

		realm.accessCodeLifespanLogin = Duration.ofDays(1).toSeconds();
		realm.accessTokenLifespan = Duration.ofHours(1).toSeconds();

		realm.ssoSessionIdleTimeout = Duration.ofDays(1).toSeconds();
		realm.ssoSessionMaxLifespan = Duration.ofDays(365).toSeconds();

		return new RealmAdapter(realm);
	}

	private static String getDomainLocale(String domainUid) {
		String lang = DEFAULT_LANG;

		try {
			lang = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IDomainSettings.class, domainUid).get().get(DomainSettingsKeys.lang.name());
		} catch (ServerFault sf) {
			logger.warn("Unable to get default lang for domain '{}', use default 'en'", domainUid, sf);
		}

		return supportedLocales.contains(lang) ? lang : DEFAULT_LANG;
	}

	public static Realm fromJson(JsonObject json) {
		if (json == null) {
			return null;
		}

		Realm realm = new Realm();
		realm.enabled = json.getBoolean("enabled");

		realm.id = json.getString("id");
		realm.realm = json.getString("realm");

		realm.loginWithEmailAllowed = json.getBoolean("loginWithEmailAllowed");

		realm.loginTheme = json.getString("loginTheme");

		realm.internationalizationEnabled = json.getBoolean("internationalizationEnabled");
		JsonArray locales = json.getJsonArray("supportedLocales");
		realm.supportedLocales = new ArrayList<>(locales.size());
		for (int i = 0; i < locales.size(); i++) {
			realm.supportedLocales.add(locales.getString(i));
		}
		realm.defaultLocale = json.getString("defaultLocale");

		realm.accessCodeLifespanLogin = json.getInteger("accessCodeLifespanLogin");
		realm.accessTokenLifespan = json.getInteger("accessTokenLifespan");

		realm.ssoSessionIdleTimeout = json.getInteger("ssoSessionIdleTimeout");
		realm.ssoSessionMaxLifespan = json.getInteger("ssoSessionMaxLifespan");

		return realm;
	}

	public JsonObject toJson() {
		JsonObject realmJson = new JsonObject();
		realmJson.put("enabled", realm.enabled);

		realmJson.put("id", realm.id);
		realmJson.put("realm", realm.id);

		realmJson.put("loginWithEmailAllowed", realm.loginWithEmailAllowed);

		realmJson.put("loginTheme", realm.loginTheme); // provide by bm-keycloak

		realmJson.put("internationalizationEnabled", realm.internationalizationEnabled);
		realmJson.put("supportedLocales", new JsonArray(realm.supportedLocales));
		realmJson.put("defaultLocale", realm.defaultLocale);

		realmJson.put("accessCodeLifespanLogin", realm.accessCodeLifespanLogin);
		realmJson.put("accessTokenLifespan", realm.accessTokenLifespan);

		realmJson.put("ssoSessionIdleTimeout", realm.ssoSessionIdleTimeout);
		realmJson.put("ssoSessionMaxLifespan", realm.ssoSessionMaxLifespan);

		return realmJson;
	}
}
