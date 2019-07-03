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
package net.bluemind.addressbook.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ImportStats;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3")
@Path("/addressbooks/vcards/{containerUid}")
public interface IVCardService {

	@GET
	@Produces("text/vcard")
	public String exportAll() throws ServerFault;

	@POST
	@Produces("text/vcard")
	public String exportCards(List<String> uids) throws ServerFault;

	/**
	 * @param vcard
	 *            one and only one card
	 * @return vcard uid
	 * @throws ServerFault
	 */
	@PUT
	public TaskRef importCards(String vcard) throws ServerFault;

	public ImportStats directImportCards(String vcard) throws ServerFault;

}
