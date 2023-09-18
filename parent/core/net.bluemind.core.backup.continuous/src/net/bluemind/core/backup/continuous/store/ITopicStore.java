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
import java.util.Optional;
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

		Optional<String> suffix();

		static String trimInstallationId(String installationId) {
			return installationId;
		}

		default String physicalTopic() {
			StringBuilder b = new StringBuilder();
			b.append(installation().replace("bluemind-", "").replace("-", "")).append("-").append(domainUid());
			suffix().ifPresent(suffix -> b.append("__").append(suffix));
			return b.toString();
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
		private Optional<String> suffix;

		public DefaultTopicDescriptor(String install, String dom, String own, String type, String uid,
				Optional<String> suffix) {
			this.install = install;
			this.domainUid = dom;
			this.owner = own;
			this.type = type;
			this.uid = uid;
			this.suffix = suffix;
		}

		/**
		 * @deprecated
		 * @param fn
		 * @return
		 */
		public static DefaultTopicDescriptor of(String fn) {
			Iterator<String> it = Splitter.on('/').split(fn).iterator();
			return new DefaultTopicDescriptor(it.next(), it.next(), it.next(), it.next(), it.next(), Optional.empty());
		}

		public static TopicDescriptor fromPhysicalTopic(String topicName) {
			int iidx = topicName.indexOf('-');
			int suffixidx = topicName.indexOf("__");
			String installationId = topicName.substring(0, iidx);
			String domainUid = topicName.substring(iidx + 1, suffixidx == -1 ? topicName.length() : suffixidx);
			Optional<String> suffix;
			if (suffixidx != -1) {
				suffix = Optional.of(topicName.substring(suffixidx + 2));
			} else {
				suffix = Optional.empty();
			}
			return new DefaultTopicDescriptor(installationId, domainUid, null, null, null, suffix);
		}

		@Override
		public String installation() {
			return install;
		}

		@Override
		public String domainUid() {
			return domainUid;
		}

		@Override
		public String owner() {
			return owner;
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public String uid() {
			return uid;
		}

		@Override
		public Optional<String> suffix() {
			return suffix;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(TopicDescriptor.class)//
					.add("in", install)//
					.add("d", domainUid)//
					.add("s", suffix)//
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
			result = prime * result + (suffix.isPresent() ? 0 : suffix.hashCode());
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
			if (suffix.isPresent() && other.suffix.isPresent()) {
				if (!suffix.get().equals(other.suffix.get()))
					return false;
			} else if (suffix.isPresent() != other.suffix.isPresent()) {
				return false;
			}
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
