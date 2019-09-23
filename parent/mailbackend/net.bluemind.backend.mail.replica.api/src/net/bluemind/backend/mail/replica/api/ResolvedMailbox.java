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
package net.bluemind.backend.mail.replica.api;

import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.ItemValue;

@BMApi(version = "3", internal = true)
public class ResolvedMailbox {

	public MailboxReplicaRootDescriptor desc;
	public String partition;
	public ItemValue<MailboxReplica> replica;
	public List<MailboxAnnotation> annotations = Collections.emptyList();

}
