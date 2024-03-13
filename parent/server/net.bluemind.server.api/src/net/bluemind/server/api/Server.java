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
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class Server implements Comparable<Server> {

	public String ip;
	public String fqdn;
	public String name;
	public List<String> tags = new ArrayList<>();

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

	/*
	 * This is used to sort ItemValue<Server> by weight. Higher weight must come
	 * first when creating multiple servers
	 */
	public int weight() {
		if (tags.contains(TagDescriptor.bm_core.getTag())) {
			return 100;
		}
		if (tags.contains(TagDescriptor.bm_es.getTag())) {
			return 90;
		}
		return 50;
	}

	@Override
	public int compareTo(Server o) {
		return Integer.compare(o.weight(), weight());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + ((fqdn == null) ? 0 : fqdn.hashCode());
		result = prime * result + ((tags == null) ? 0 : tags.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (o instanceof Server) { // NOSONAR: GWT / API don't understand java 21 casts
			Server os = (Server) o;
			var ipEquals = ((ip == null && os.ip == null) || (ip != null && ip.equals(os.ip)));
			var nameEquals = ((name == null && os.name == null) || (name != null && name.equals(os.name)));
			var fqdnEquals = ((fqdn == null && os.fqdn == null) || (fqdn != null && fqdn.equals(os.fqdn)));
			var tagsEquals = ((tags == null && os.tags == null) || (tags != null && tags.equals(os.tags)));
			return ipEquals && nameEquals && fqdnEquals && tagsEquals;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "Server{addr: " + address() + ", tags: " + Arrays.toString(tags.toArray()) + "}";
	}
}
