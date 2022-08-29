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
package net.bluemind.system.api;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.system.api.SubscriptionInformations.Kind;

@BMApi(version = "3")
@Path("/system/installation")
public interface IInstallation extends ICustomTheme {

	@GET
	@Path("subscription")
	public SubscriptionInformations getSubscriptionInformations() throws ServerFault;

	@GET
	@Path("subscriptionKind")
	public Kind getSubscriptionKind() throws ServerFault;

	@GET
	@Path("subscriptionProductiveValid")
	public Boolean isValidProductionSubscription() throws ServerFault;

	@POST
	@Path("subscription")
	public void updateSubscription(String licence) throws ServerFault;

	@POST
	@Path("subscription/_archive")
	@Consumes("application/zip")
	public void updateSubscriptionWithArchive(Stream archive) throws ServerFault;

	/**
	 * Update subscription URL to given version on all servers
	 * 
	 * @param version target version. Special versions:
	 *                <ul>
	 *                <li><i>latest</i> targets the latest published version of
	 *                installed BlueMind major version</li>
	 *                <li><i>current</i> targets the current installed version of
	 *                BlueMind</li>
	 *                </ul>
	 * 
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@POST
	@Path("subscription/_version")
	public void updateSubscriptionVersion(@QueryParam("version") String version);

	@DELETE
	@Path("subscription")
	public void removeSubscription() throws ServerFault;

	@GET
	@Path("state")
	public SystemState getSystemState() throws ServerFault;

	@PUT
	@Path("state/_maintenance")
	public void maintenanceMode() throws ServerFault;

	@DELETE
	@Path("state/_maintenance")
	public void runningMode() throws ServerFault;

	@GET
	@Path("version")
	public InstallationVersion getVersion() throws ServerFault;

	@POST
	@Path("version")
	public void markSchemaAsUpgraded() throws ServerFault;

	@POST
	@Path("_initialize")
	public TaskRef initialize() throws ServerFault;

	@POST
	@Path("_upgrade")
	public TaskRef upgrade() throws ServerFault;

	/**
	 * Run post-installation upgraders
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_postinst")
	public TaskRef postinst() throws ServerFault;

	@GET
	@Path("_upgrade")
	public UpgradeStatus upgradeStatus() throws ServerFault;

	@POST
	@Path("_resetIndexes")
	public void resetIndexes();

	@POST
	@Path("{index}/_resetIndex")
	public void resetIndex(@PathParam("index") String index);

	@GET
	@Path("_infos")
	public PublicInfos getInfos();

	@POST
	@Path("{ip}/ping")
	void ping(@PathParam(value = "ip") String ip) throws ServerFault;

	@GET
	@Path("_subscriptionContacts")
	public List<String> getSubscriptionContacts() throws ServerFault;

	@POST
	@Path("_subscriptionContacts")
	public void setSubscriptionContacts(List<String> emails) throws ServerFault;

	@GET
	@Path("_hostReport")
	public String getHostReport();

	@POST
	@Path("_hostReport")
	public String sendHostReport();

	@POST
	@Path("_clone")
	public TaskRef clone(CloneConfiguration sourceParams);

	/**
	 * The instance will stop handling write requests then will write a BYE message
	 * into kafka to relinquish control to the clone.
	 * 
	 * @throws ServerFault
	 */
	@PUT
	@Path("state/_demote")
	public void demoteLeader() throws ServerFault;

	@PUT
	@Path("state/_promote")
	public void promoteLeader() throws ServerFault;

}
