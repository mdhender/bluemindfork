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
package net.bluemind.mailbox.api;

import java.util.Set;

import jakarta.validation.constraints.NotNull;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SimpleShardStats {
	@NotNull
	public String indexName;

	@NotNull
	public Set<String> mailboxes;

	@NotNull
	public Set<String> aliases;

	public long docCount;

	public long deletedCount;

	public long externalRefreshCount;

	public long externalRefreshDuration;

	/**
	 * index size in byte
	 */
	public long size;

}
