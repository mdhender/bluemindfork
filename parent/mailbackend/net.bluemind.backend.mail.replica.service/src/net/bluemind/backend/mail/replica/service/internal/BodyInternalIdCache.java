/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.internal;

import com.google.common.base.MoreObjects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.core.container.model.ItemVersion;

/**
 * When storing a body we might have a header
 * {@link MailApiHeaders#X_BM_INTERNAL_ID} indicating the record id we want
 * 
 * This cache is here to avoid reloading the body to figure it out
 *
 */
public class BodyInternalIdCache {

	private static final Cache<String, ExpectedId> bodyUidToExpectedRecordId = CacheBuilder.newBuilder()
			.maximumSize(512).build();

	private static final Cache<String, VanishedBody> vanish = CacheBuilder.newBuilder().maximumSize(512).build();

	public static class ExpectedId {
		public final long id;
		public final String updateOfBody;
		public final String owner;

		public ExpectedId(long id, String owner, String updateOfBody) {
			this.id = id;
			this.owner = owner;
			this.updateOfBody = updateOfBody;
		}

		public String toString() {
			return MoreObjects.toStringHelper(ExpectedId.class).add("id", id).add("updateOfBody", updateOfBody)
					.add("owner", owner).toString();
		}
	}

	public static class VanishedBody {
		public final long id;
		public ItemVersion version;

		public VanishedBody(long id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "VanishedBody [id=" + id + ", version=" + version + "]";
		}

	}

	public static ExpectedId expectedRecordId(String owner, String bodyUid) {
		ExpectedId ifPresent = bodyUidToExpectedRecordId.getIfPresent(bodyUid);
		if (ifPresent != null && ifPresent.owner.equals(owner)) {
			return ifPresent;
		}
		return null;
	}

	public static VanishedBody vanishedBody(String owner, String bodyUid) {
		return vanish.getIfPresent(owner + "#" + bodyUid);
	}

	public static void storeExpectedRecordId(String bodyUid, ExpectedId id) {
		bodyUidToExpectedRecordId.put(bodyUid, id);
		if (id.updateOfBody != null) {
			vanish.put(id.owner + "#" + id.updateOfBody, new VanishedBody(id.id));
		}
	}

	public static void invalidateBody(String messageBody) {
		bodyUidToExpectedRecordId.invalidate(messageBody);
	}

}
