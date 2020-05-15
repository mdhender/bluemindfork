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

package net.bluemind.server.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class Server {

	public String ip;
	public String fqdn;
	public String name;
	public List<String> tags = new LinkedList<>();

	public String address() {
		return ip != null ? ip : fqdn;
	}

	public Server copy() {
		Server ret = new Server();
		ret.ip = ip;
		ret.fqdn = fqdn;
		ret.name = name;
		ret.tags = new ArrayList<>(tags);
		return ret;
	}

	public static Server tagged(String ip, String... tags) {
		Server srv = new Server();
		srv.ip = ip;
		srv.tags = Arrays.asList(tags);
		return srv;
	}

	@Override
	public String toString() {
		return "Server{addr: " + address() + ", tags: " + Arrays.toString(tags.toArray()) + "}";
	}

}
