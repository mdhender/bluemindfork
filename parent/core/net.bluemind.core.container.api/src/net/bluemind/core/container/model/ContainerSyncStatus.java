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
package net.bluemind.core.container.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Required;

/** The up-to-date status of a container. */
@BMApi(version = "3")
public class ContainerSyncStatus {

	/** Possible statuses. */
	@BMApi(version = "3")
	public enum Status {
		SUCCESS, ERROR;
	}

	/** Tokens needed for the synchronization. */
	@Required
	public Map<String, String> syncTokens = new HashMap<>();

	/** When the next synchronization should be done in milliseconds. */
	public Long nextSync;

	/** When the last synchronization has been done. */
	public Date lastSync;

	/** The {@link Status} of the synchronization. */
	public Status syncStatus;

	/** Container specific status informations. */
	public String syncStatusInfo;
}
