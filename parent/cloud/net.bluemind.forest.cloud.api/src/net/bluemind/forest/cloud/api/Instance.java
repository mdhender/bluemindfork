/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.forest.cloud.api;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class Instance {

	@BMApi(version = "3")
	public static class Node {
		public String uid;
		public Set<String> tags = Collections.emptySet();
		public String address;

		public static Node create(String uid, String address, String... tags) {
			Node n = new Node();
			n.uid = uid;
			n.address = address;
			n.tags = Sets.newHashSet(tags);
			return n;
		}
	}

	@BMApi(version = "3")
	public static class Partition {
		public String domain;
		public Set<String> aliases;

		public static Partition create(String domain, String... aliases) {
			Partition p = new Partition();
			p.domain = domain;
			p.aliases = Sets.newHashSet(aliases);
			return p;
		}
	}

	@BMApi(version = "3")
	public static class Version {
		public int major;
		public int minor;
		public int build;

		public static Version create(int ma, int mi, int b) {
			Version v = new Version();
			v.major = ma;
			v.minor = mi;
			v.build = b;
			return v;
		}
	}

	public String installationId;
	public String coreToken;
	public String externalUrl;
	public Version version;
	public List<Node> topology = Collections.emptyList();
	public List<Partition> aliases = Collections.emptyList();

}
