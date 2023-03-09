/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.api;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICrudSupport;
import net.bluemind.core.container.api.IReadByIdSupport;
import net.bluemind.core.container.api.IRestoreItemCrudSupport;
import net.bluemind.core.container.model.ItemValue;

/**
 * 
 * ISmimeCACerts API - admin can add CA certificates they trust. They'll be
 * trusted for all domain users. Those certificates are fetched by MailApp to
 * verify end-user certificate and check if one can be trusted.
 * 
 */
@BMApi(version = "3", genericType = SmimeCacert.class)
@Path("/smime_cacerts/{containerUid}")
public interface ISmimeCACert extends IChangelogSupport, ICrudSupport<SmimeCacert>, IReadByIdSupport<SmimeCacert>,
		IRestoreItemCrudSupport<SmimeCacert> {

	/**
	 * List all S/MIME certificates of a container
	 * 
	 * @return All {@link SmimeCacert} of the container
	 * @throws ServerFault common error object
	 */
	@GET
	@Path("_all")
	public List<ItemValue<SmimeCacert>> all() throws ServerFault;

	/**
	 * Delete all {@link SmimeCacert}s of this domain
	 * 
	 * @throws ServerFault common error object
	 */
	@POST
	@Path("_reset")
	void reset() throws ServerFault;

}
