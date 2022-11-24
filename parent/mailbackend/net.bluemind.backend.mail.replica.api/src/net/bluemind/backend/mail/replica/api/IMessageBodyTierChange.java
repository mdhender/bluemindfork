/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.backend.mail.replica.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3", internal = true)
@Path("/replicated_tier_change/{serverUid}")
public interface IMessageBodyTierChange {
	public static int TIER_CHANGES_PER_TICK = 1_000;
	public static int TIER_CHANGES_MAX_RETRIES = 7; // 7 days

	/*
	 * Call when a new body is created
	 */
	@PUT
	@Path("_create_body")
	public void createBody(MessageBody body);

	/*
	 * Execute a round of moves between storage tiers (if supported by the message
	 * body object store)
	 */
	@POST
	@Path("_move_tier")
	public Integer moveTier();

	/*
	 * Truncate the whole queue. Used when archiveKind is changed to a storate which
	 * does not support HSM.
	 */
	@POST
	@Path("_truncate")
	public void truncate();

	/*
	 * Requeue all tier moves, after a SystemConfiguration change, or during an
	 * upgrade
	 */
	@POST
	@Path("_requeue_tier_moves")
	public TaskRef requeueAllTierMoves();
}
