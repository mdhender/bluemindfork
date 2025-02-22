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
package net.bluemind.eas.api;

import java.util.Map;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/eas")
// FIXME dirty api
public interface IEas {

	// Heartbeat
	@GET
	@Path("_heartbeat")
	public Heartbeat getHeartbeat(@QueryParam("deviceUid") String deviceUid) throws ServerFault;

	@PUT
	@Path("_heartbeat")
	public void setHeartbeat(Heartbeat heartbeat) throws ServerFault;

	// Reset

	@PUT
	@Path("_reset")
	public void insertPendingReset(Account account) throws ServerFault;

	@GET
	@Path("_needReset")
	public Boolean needReset(Account account) throws ServerFault;

	@DELETE
	@Path("_deletePendingReset")
	public void deletePendingReset(Account account) throws ServerFault;

	// CLientID
	@PUT
	@Path("_sendmailId/{clientId}")
	public void insertClientId(@PathParam(value = "clientId") String clientId) throws ServerFault;

	@GET
	@Path("_sendmailId/{clientId}")
	public Boolean isKnownClientId(@PathParam(value = "clientId") String clientId) throws ServerFault;

	// FolderSync
	@PUT
	@Path("_setFolderSync")
	public void setFolderSyncVersions(FolderSyncVersions versions) throws ServerFault;

	@GET
	@Path("_getFolderSync")
	public Map<String, String> getFolderSyncVersions(Account account) throws ServerFault;

	@GET
	@Path("_getConfiguration")
	public Map<String, String> getConfiguration() throws ServerFault;

}
