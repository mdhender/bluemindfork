/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3", internal = true)
@Path("/cyrus_annotations")
public interface ICyrusReplicationAnnotations {

	@PUT
	@Path("_annotation")
	void storeAnnotation(MailboxAnnotation ss);

	@DELETE
	@Path("_annotation")
	void deleteAnnotation(MailboxAnnotation ss);

	@GET
	@Path("{mbox}/_annotation")
	List<MailboxAnnotation> annotations(@PathParam("mbox") String mailbox);
}
