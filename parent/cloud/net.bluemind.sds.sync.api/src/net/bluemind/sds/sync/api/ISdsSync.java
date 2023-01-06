/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.sds.sync.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;

/**
 * API for managing the SDS stores synchronizations
 * 
 */
@BMApi(version = "3")
@Path("/sdssync")
public interface ISdsSync {

	/**
	 * Returns a stream of SdsSync events from the "fromIndex" to the current end of
	 * the SDS Sync Queue
	 * 
	 * @param fromIndex: ChronicleQueue begining index
	 * @return Stream of SdsSync
	 * @throws ServerFault
	 */
	@GET
	@Path("sync")
	Stream sync(@QueryParam(value = "fromIndex") long fromIndex) throws ServerFault;

	/**
	 * Counts how many items are in the queue between fromIndex and the queue end
	 * 
	 * @param fromIndex: ChronicleQueue begining index
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("count")
	long count(@QueryParam(value = "fromIndex") long fromIndex) throws ServerFault;
}
