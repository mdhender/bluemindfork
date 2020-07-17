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
package net.bluemind.eas.dto;

import java.util.Set;

import net.bluemind.eas.dto.sync.CollectionSyncRequest;

/**
 * The EAS protocol sometimes omit some parameters in its requests and requires
 * to reuse settings from previous requests.
 * 
 * This interface provides access to this data when parsing and incoming
 * request.
 */
public interface IPreviousRequestsKnowledge {

	Set<CollectionSyncRequest> getLastMonitored();

	String getPolicyKey();

	Integer getLastWait();

	Long getHeartbeart();

}
