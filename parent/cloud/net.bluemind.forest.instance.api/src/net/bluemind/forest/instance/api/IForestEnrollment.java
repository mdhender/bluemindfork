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
package net.bluemind.forest.instance.api;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import net.bluemind.core.api.BMApi;

/**
 * Endpoint deployed in bm-core when federation plugin is installed.
 *
 */
@BMApi(version = "3")
@Path("/forest/enroll")
public interface IForestEnrollment {

	/**
	 * Called from forest servers to update the stored enpoints in instances
	 */
	@POST
	@Path("_checkpoint")
	void checkpoint(ForestEnpoints endpoints);

}
