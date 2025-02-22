/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.system.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3", internal = true)
@Path("/system/cache")
public interface ICacheMgmt {
	/**
	 * Flushes all internal caches
	 */
	@POST
	@Path("_flush")
	public void flushCaches() throws ServerFault;

	/**
	 * Serializes the content of all caches
	 * 
	 * <code>
	 * {
	 *    "cache1": {
	 *        key0: value,
	 *        key1: value,
	 *    },
	 *    "cacheN": {
	 *        keyX: valueY
	 *    }
	 * }
	 * </code>
	 * 
	 * @return
	 */
	@GET
	@Path("_dump")
	@Produces("application/json")
	public Stream dumpContent();

}
