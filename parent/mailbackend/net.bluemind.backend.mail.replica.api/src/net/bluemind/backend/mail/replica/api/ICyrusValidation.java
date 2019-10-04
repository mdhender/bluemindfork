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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;

/**
 * This internal API is called by sds-proxy.
 *
 */
@BMApi(version = "3", internal = true)
@Path("/cyrus_validation")
public interface ICyrusValidation {

	/**
	 * Check if a new mailbox and its partition are valid as a mailbox for BlueMind.
	 * 
	 * @param mailbox   Cyrus internal name for a mailbox that must be approved by
	 *                  the API (ex: global.virt!user.alice)
	 * @param partition Cyrus internal name for a partition that must be approved by
	 *                  the API (bm-master__global_virt)
	 * @return true/false if the name of the mailbox is valid for a mailbox creation
	 */
	@GET
	@Path("_prevalidate")
	public boolean prevalidate(@QueryParam("mailbox") String mailbox, @QueryParam("partition") String partition);
}
