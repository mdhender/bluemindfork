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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.directory.hollow.datamodel.producer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Serializers {
	private static final Map<String, DirectorySerializer> active = new ConcurrentHashMap<>();

	private Serializers() {
	}

	public static DirectorySerializer put(String domain, DirectorySerializer ds) {
		active.put(domain, ds);
		return ds;
	}

	public static void remove(String domain) {
		active.remove(domain);
	}

	public static void clear() {
		active.clear();
	}

	public static DirectorySerializer forDomain(String dom) {
		return active.get(dom);
	}

}
