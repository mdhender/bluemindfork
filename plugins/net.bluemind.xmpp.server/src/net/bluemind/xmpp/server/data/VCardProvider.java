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
package net.bluemind.xmpp.server.data;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.user.api.User;
import net.bluemind.xmpp.server.CF;
import tigase.xmpp.BareJID;

public class VCardProvider implements IDataProvider {
	private static final Cache<String, String> cache = Caffeine.newBuilder().recordStats()
			.expireAfterAccess(1, TimeUnit.HOURS).build();
	private static final Logger logger = LoggerFactory.getLogger(VCardProvider.class);

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(VCardProvider.class, cache);
		}
	}

	@Override
	public String getFor(BareJID user) {
		// <vCard xmlns="vcard-temp">
		// <FN>psi</FN>
		// <NICKNAME>psi</NICKNAME>
		// </vCard>

		String s = cache.getIfPresent(user.toString());
		if (s != null) {
			logger.info("Get " + user.toString() + " VCard from cache");
			return s;
		}

		StringBuilder vc = new StringBuilder();
		try {
			// see http://xmpp.org/extensions/xep-0054.html
			ItemValue<User> u = CF.user(user);
			vc.append("<vCard xmlns=\"vcard-temp\">\n");
			vc.append("<FN>").append(u.value.contactInfos.identification.formatedName.value).append("</FN>\n");
			vc.append("<NICKNAME>").append(u.value.contactInfos.identification.formatedName.value)
					.append("</NICKNAME>\n");
			vc.append("<JABBERID>").append(u.value.defaultEmail().address).append("</JABBERID>");
			vc.append("<EMAIL><WORK/><PREF/><USERID>").append(u.value.defaultEmail().address)
					.append("</USERID></EMAIL>");

			byte[] photo = CF.userPhoto(user);

			if (photo != null) {
				// <PHOTO><TYPE>image/png</TYPE><BINVAL>iVBORw0K</BINVAL></PHOTO>
				vc.append("<PHOTO>");
				vc.append("<TYPE>");
				vc.append("image/png");
				vc.append("</TYPE>");
				// switch from gwt b64 to std b64
				vc.append("<BINVAL>").append(Base64.getEncoder().encodeToString(photo)).append("</BINVAL>");
				vc.append("</PHOTO>\n");
			}

			vc.append("</vCard>");
			String ret = vc.toString();
			logger.debug("[" + u.value.login + "] vCard generated:\n" + ret);
			cache.put(user.toString(), ret);
			return ret;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}
}
