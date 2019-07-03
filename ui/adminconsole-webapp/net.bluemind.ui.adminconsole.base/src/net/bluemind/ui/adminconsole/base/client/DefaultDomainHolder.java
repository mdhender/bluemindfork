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
package net.bluemind.ui.adminconsole.base.client;

import java.util.List;

import com.google.gwt.user.client.Cookies;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public class DefaultDomainHolder {

	public static ItemValue<Domain> get(List<ItemValue<Domain>> domains) {
		String uid = Cookies.getCookie("ac-default-domain");
		if (uid != null) {
			return domains.stream().filter(d -> d.uid.equals(uid)).findFirst().orElse(null);
		} else {
			return null;
		}
	}

	public static void set(String uid) {
		Cookies.setCookie("ac-default-domain", uid);
	}

}
