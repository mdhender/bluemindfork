/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3", internal = true)
@Path("/cyrus_artifacts/{userId}")
public interface ICyrusReplicationArtifacts {

	@PUT
	@Path("_sieve")
	void storeScript(SieveScript ss);

	@DELETE
	@Path("_sieve")
	void deleteScript(SieveScript ss);

	@GET
	@Path("_sieve")
	List<SieveScript> sieves();

	@PUT
	@Path("_sub")
	void storeSub(MailboxSub ss);

	@DELETE
	@Path("_sub")
	void deleteSub(MailboxSub ss);

	@GET
	@Path("_sub")
	List<MailboxSub> subs();

	@PUT
	@Path("_quota")
	void storeQuota(QuotaRoot ss);

	@DELETE
	@Path("_quota")
	void deleteQuota(QuotaRoot ss);

	@GET
	@Path("_quota")
	List<QuotaRoot> quotas();

	@PUT
	@Path("_seen")
	void storeSeen(SeenOverlay ss);

	@GET
	@Path("_seen")
	List<SeenOverlay> seens();

}
