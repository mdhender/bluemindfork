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

import java.util.Optional;

import net.bluemind.core.backup.continuous.store.ITopicStore.DefaultTopicDescriptor;
import net.bluemind.core.backup.continuous.store.ITopicStore.TopicDescriptor;
import net.bluemind.core.container.model.BaseContainerDescriptor;

public class TopicNames {

	private String iid;

	public TopicNames(String installationId) {
		this.iid = installationId;
	}

	public static String build(String installationId, String domainUid) {
		return installationId + "-" + domainUid;
	}

	public TopicDescriptor forContainer(BaseContainerDescriptor c) {
		return new DefaultTopicDescriptor(iid, Optional.ofNullable(c.domainUid).orElse("__orphans__"), c.owner, c.type,
				c.uid);
	}

}
