/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.filehosting.webdav.service;

import java.net.ProxySelector;

import com.github.sardine.Sardine;

public class TrustAllSardineFactory {

	public static Sardine begin() {
		return begin(null, null);
	}

	public static Sardine begin(String username, String password) {
		return begin(username, password, null);
	}

	public static Sardine begin(String username, String password, ProxySelector proxy) {
		return new TrustAllSardineImpl(username, password);
	}

}
