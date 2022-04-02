/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.store;

import java.util.Iterator;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;

import io.vertx.core.json.JsonObject;

public interface ITopicStore {

	Set<String> topicNames();

	Set<String> topicNames(String installationId);

	TopicSubscriber getSubscriber(String topicName);

	TopicPublisher getPublisher(TopicDescriptor td);

	TopicManager getManager();

	default boolean isEnabled() {
		return true;
	}

	public interface IResumeToken {

		JsonObject toJson();

	}

	public interface TopicDescriptor {

		String installation();

		String domainUid();

		String owner();

		String type();

		String uid();

		static String trimInstallationId(String installationId) {
			return installationId;
		}

		default String fullName() {
			return installation() + "/" + domainUid() + "/" + owner() + "/" + type() + "/" + uid();
		}

		default String physicalTopic() {
			return installation().replace("bluemind-", "").replace("-", "") + "-" + domainUid();
		}

		default String partitionKey(String uid) {
			return (!owner().equals("system")) ? owner() : uid;
		}

	}

	public static final class DefaultTopicDescriptor implements TopicDescriptor {

		private String install;
		private String domainUid;
		private String owner;
		private String type;
		private String uid;

		public DefaultTopicDescriptor(String install, String dom, String own, String type, String uid) {
			this.install = install;
			this.domainUid = dom;
			this.owner = own;
			this.type = type;
			this.uid = uid;
		}

		/**
		 * @deprecated
		 * @param fn
		 * @return
		 */
		public static DefaultTopicDescriptor of(String fn) {
			Iterator<String> it = Splitter.on('/').split(fn).iterator();
			return new DefaultTopicDescriptor(it.next(), it.next(), it.next(), it.next(), it.next());
		}

		public String installation() {
			return install;
		}

		public String domainUid() {
			return domainUid;
		}

		public String owner() {
			return owner;
		}

		public String type() {
			return type;
		}

		public String uid() {
			return uid;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(TopicDescriptor.class)//
					.add("in", install)//
					.add("d", domainUid)//
					.add("o", owner)//
					.add("t", type)//
					.add("uid", uid)//
					.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((domainUid == null) ? 0 : domainUid.hashCode());
			result = prime * result + ((uid == null) ? 0 : uid.hashCode());
			result = prime * result + ((install == null) ? 0 : install.hashCode());
			result = prime * result + ((owner == null) ? 0 : owner.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DefaultTopicDescriptor other = (DefaultTopicDescriptor) obj;
			if (domainUid == null) {
				if (other.domainUid != null)
					return false;
			} else if (!domainUid.equals(other.domainUid))
				return false;
			if (uid == null) {
				if (other.uid != null)
					return false;
			} else if (!uid.equals(other.uid))
				return false;
			if (install == null) {
				if (other.install != null)
					return false;
			} else if (!install.equals(other.install))
				return false;
			if (owner == null) {
				if (other.owner != null)
					return false;
			} else if (!owner.equals(other.owner))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}

	}

}
